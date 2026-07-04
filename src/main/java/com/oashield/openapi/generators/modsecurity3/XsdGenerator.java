package com.oashield.openapi.generators.modsecurity3;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.extern.slf4j.Slf4j;

/**
 * Generates an XML Schema (XSD) from OpenAPI models, mirroring
 * {@link JsonSchemaGenerator}'s role for JSON bodies. Scope: elements,
 * attributes, and simple-type facets (pattern, lengths, bounds, enumerations);
 * composed schemas and map types in XML are out of scope.
 *
 * NOTE: current libmodsecurity3 cannot load XSDs at request time (its XXE
 * hardening disables libxml2's entity loader, so @validateSchema fails open to
 * "match everything") and Coraza has no XML support at all — see
 * docs/engine-behavior.md. The generated XSD is therefore only wired into rules
 * behind the validateXmlSchema option (default false); it remains useful for
 * upstream/application-side validation.
 */
@Slf4j
public class XsdGenerator {

    private static final String XS_NS = "http://www.w3.org/2001/XMLSchema";

    /**
     * Generate one XSD containing a named complexType per model plus a top-level
     * element per model, so any model can be an XML document root.
     */
    public String generateXsd(Map<String, ModelsMap> models) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element schema = doc.createElementNS(XS_NS, "xs:schema");
            doc.appendChild(schema);

            for (Map.Entry<String, ModelsMap> entry : models.entrySet()) {
                if (entry.getValue().getModels() == null || entry.getValue().getModels().isEmpty()) {
                    continue;
                }
                ModelMap modelMap = entry.getValue().getModels().get(0);
                CodegenModel model = modelMap.getModel();
                if (model == null) {
                    continue;
                }
                addModel(doc, schema, entry.getKey(), model);
            }

            StringWriter writer = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            javax.xml.transform.Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            log.error("Error generating XSD", e);
            return "";
        }
    }

    private void addModel(Document doc, Element schema, String modelName, CodegenModel model) {
        String rootName = model.getXmlName() != null ? model.getXmlName() : modelName;

        Element complexType = doc.createElementNS(XS_NS, "xs:complexType");
        complexType.setAttribute("name", modelName);
        Element sequence = doc.createElementNS(XS_NS, "xs:sequence");
        complexType.appendChild(sequence);

        if (model.vars != null) {
            for (CodegenProperty var : model.vars) {
                addProperty(doc, sequence, complexType, var);
            }
        }
        schema.appendChild(complexType);

        Element rootElement = doc.createElementNS(XS_NS, "xs:element");
        rootElement.setAttribute("name", rootName);
        rootElement.setAttribute("type", modelName);
        schema.appendChild(rootElement);
    }

    private void addProperty(Document doc, Element sequence, Element complexType, CodegenProperty var) {
        String name = var.getXmlName() != null ? var.getXmlName() : var.baseName;

        if (var.isXmlAttribute) {
            Element attr = doc.createElementNS(XS_NS, "xs:attribute");
            attr.setAttribute("name", name);
            attr.setAttribute("type", xsdType(var));
            if (var.required) {
                attr.setAttribute("use", "required");
            }
            // xs:attribute children must follow the content model inside xs:complexType
            complexType.appendChild(attr);
            return;
        }

        if (var.isArray) {
            CodegenProperty item = var.items;
            String itemName = item != null && item.getXmlName() != null ? item.getXmlName() : name;
            Element repeated = doc.createElementNS(XS_NS, "xs:element");
            repeated.setAttribute("name", itemName);
            applyOccurs(repeated, var);
            typeOrRestriction(doc, repeated, item != null ? item : var);

            if (var.isXmlWrapped) {
                Element wrapper = doc.createElementNS(XS_NS, "xs:element");
                wrapper.setAttribute("name", name);
                wrapper.setAttribute("minOccurs", var.required ? "1" : "0");
                Element wrapperType = doc.createElementNS(XS_NS, "xs:complexType");
                Element wrapperSeq = doc.createElementNS(XS_NS, "xs:sequence");
                wrapperSeq.appendChild(repeated);
                wrapperType.appendChild(wrapperSeq);
                wrapper.appendChild(wrapperType);
                sequence.appendChild(wrapper);
            } else {
                sequence.appendChild(repeated);
            }
            return;
        }

        Element element = doc.createElementNS(XS_NS, "xs:element");
        element.setAttribute("name", name);
        element.setAttribute("minOccurs", var.required ? "1" : "0");
        typeOrRestriction(doc, element, var);
        sequence.appendChild(element);
    }

    private void applyOccurs(Element repeated, CodegenProperty arrayVar) {
        Integer min = arrayVar.getMinItems();
        Integer max = arrayVar.getMaxItems();
        repeated.setAttribute("minOccurs",
                min != null ? min.toString() : (arrayVar.required && !arrayVar.isXmlWrapped ? "1" : "0"));
        repeated.setAttribute("maxOccurs", max != null ? max.toString() : "unbounded");
    }

    /**
     * Set the element's type: a reference for model properties, an inline
     * restriction when facets exist, otherwise a plain built-in type.
     */
    private void typeOrRestriction(Document doc, Element element, CodegenProperty var) {
        if (var.isModel && var.complexType != null) {
            element.setAttribute("type", var.complexType);
            return;
        }

        boolean hasFacets = var.pattern != null || var.getMinLength() != null || var.getMaxLength() != null
                || var.minimum != null || var.maximum != null
                || (var.allowableValues != null && var.allowableValues.get("values") instanceof List);
        if (!hasFacets) {
            element.setAttribute("type", xsdType(var));
            return;
        }

        Element simpleType = doc.createElementNS(XS_NS, "xs:simpleType");
        Element restriction = doc.createElementNS(XS_NS, "xs:restriction");
        restriction.setAttribute("base", xsdType(var));

        String pattern = Modsecurity3Generator.sanitizeSpecPattern(var.pattern);
        if (pattern != null && !pattern.isEmpty()) {
            // XSD patterns are implicitly anchored
            addFacet(doc, restriction, "xs:pattern", "value", Modsecurity3Generator.stripAnchors(pattern));
        }
        if (var.getMinLength() != null) {
            addFacet(doc, restriction, "xs:minLength", "value", var.getMinLength().toString());
        }
        if (var.getMaxLength() != null) {
            addFacet(doc, restriction, "xs:maxLength", "value", var.getMaxLength().toString());
        }
        if (var.minimum != null) {
            addFacet(doc, restriction, var.exclusiveMinimum ? "xs:minExclusive" : "xs:minInclusive",
                    "value", var.minimum);
        }
        if (var.maximum != null) {
            addFacet(doc, restriction, var.exclusiveMaximum ? "xs:maxExclusive" : "xs:maxInclusive",
                    "value", var.maximum);
        }
        if (var.allowableValues != null && var.allowableValues.get("values") instanceof List) {
            for (Object value : (List<?>) var.allowableValues.get("values")) {
                addFacet(doc, restriction, "xs:enumeration", "value", String.valueOf(value));
            }
        }

        simpleType.appendChild(restriction);
        element.appendChild(simpleType);
    }

    private void addFacet(Document doc, Element restriction, String facet, String attr, String value) {
        Element el = doc.createElementNS(XS_NS, facet);
        el.setAttribute(attr, value);
        restriction.appendChild(el);
    }

    private String xsdType(CodegenProperty var) {
        if (var.isInteger) {
            return "xs:int";
        }
        if (var.isLong) {
            return "xs:long";
        }
        if (var.isNumber || var.isFloat || var.isDouble || var.isDecimal) {
            return "xs:decimal";
        }
        if (var.isBoolean) {
            return "xs:boolean";
        }
        if (var.isDate) {
            return "xs:date";
        }
        if (var.isDateTime) {
            return "xs:dateTime";
        }
        return "xs:string";
    }
}
