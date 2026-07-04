package com.oashield.openapi.generators.modsecurity3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oashield.openapi.generators.modsecurity3.types.JsonSchemaTypeMapper;
import org.openapitools.codegen.CodegenComposedSchemas;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Utility class for generating JSON Schema documents from OpenAPI models.
 */
@Slf4j
public class JsonSchemaGenerator {
    private static final String JSON_SCHEMA_DRAFT7 = "http://json-schema.org/draft-07/schema#";

    private final ObjectMapper objectMapper;

    public JsonSchemaGenerator() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate a JSON Schema document from OpenAPI models.
     *
     * @param modelsMap The models to convert to JSON Schema
     * @return A JSON string representing the JSON Schema document
     */
    public String generateJsonSchema(ModelsMap modelsMap) {
        try {
            ObjectNode rootSchema = createRootSchema();
            ObjectNode definitions = rootSchema.putObject("definitions");

            // Process each model and add to definitions
            List<ModelMap> modelMaps = modelsMap.getModels();
            for (ModelMap modelMap : modelMaps) {
                CodegenModel model = modelMap.getModel();
                String modelName = model.name;
                JsonNode schemaNode = extractJsonSchemaFromModelMap(modelName, modelMap);
                if (schemaNode != null) {
                    definitions.set(modelName, schemaNode);
                }
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootSchema);
        } catch (Exception e) {
            log.error("Error generating JSON Schema", e);
            return "{}";
        }
    }

    /**
     * Create the root JSON Schema object.
     *
     * @return The root JSON Schema object
     */
    private ObjectNode createRootSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", JSON_SCHEMA_DRAFT7);
        schema.put("title", "OpenAPI Schema Definitions");
        schema.put("description", "JSON Schema definitions generated from OpenAPI specification");
        // No root "type": request bodies may be objects or arrays; a type:object
        // root would make Coraza's @validateSchema reject root-array bodies.
        return schema;
    }

    /**
     * Generate a JSON Schema for a specific model.
     *
     * @param model The model to convert to JSON Schema
     * @return An ObjectNode representing the JSON Schema for the model
     */
    public ObjectNode generateModelSchema(CodegenModel model) {
        try {
            ObjectNode schemaNode = objectMapper.createObjectNode();

            // Log the model structure to understand what's available
            log.debug("Model name: {}", model.name);

            // Add metadata
            if (model.title != null && !model.title.isEmpty()) {
                schemaNode.put("title", model.title);
            } else {
                schemaNode.put("title", model.name);
            }

            if (model.description != null && !model.description.isEmpty()) {
                schemaNode.put("description", model.description);
            }

            // oneOf/anyOf composition: emit the composition keyword instead of a plain
            // object schema (allOf models already arrive with their vars merged).
            CodegenComposedSchemas composed = model.getComposedSchemas();
            List<CodegenProperty> oneOf = composed != null ? composed.getOneOf() : null;
            List<CodegenProperty> anyOf = composed != null ? composed.getAnyOf() : null;
            if ((oneOf != null && !oneOf.isEmpty()) || (anyOf != null && !anyOf.isEmpty())) {
                boolean isOneOf = oneOf != null && !oneOf.isEmpty();
                ArrayNode members = schemaNode.putArray(isOneOf ? "oneOf" : "anyOf");
                for (CodegenProperty member : (isOneOf ? oneOf : anyOf)) {
                    members.add(composedMemberSchema(member));
                }
                return schemaNode;
            }

            // Set type to object
            schemaNode.put("type", "object");

            // Model-level property counts
            if (model.getMinProperties() != null) {
                schemaNode.put("minProperties", model.getMinProperties());
            }
            if (model.getMaxProperties() != null) {
                schemaNode.put("maxProperties", model.getMaxProperties());
            }

            // Process properties
            if (model.vars != null && !model.vars.isEmpty()) {
                processProperties(model.vars, schemaNode);
            }

            // Process required properties
            if (model.requiredVars != null && !model.requiredVars.isEmpty()) {
                processRequiredProperties(model.requiredVars, schemaNode);
            }

            return schemaNode;
        } catch (Exception e) {
            log.error("Error processing model: {}", model.name, e);
            return null;
        }
    }

    // JSON Schema keywords openapi-generator's Codegen abstractions do not surface;
    // copied verbatim from the raw parsed spec schema. Coraza's validator enforces
    // all of them even under a draft-07 $schema (docs/engine-behavior.md).
    private static final String[] RAW_KEYWORDS = {
            "const", "prefixItems", "patternProperties", "dependentRequired", "dependentSchemas",
            "if", "then", "else", "contains", "minContains", "maxContains", "propertyNames" };

    /**
     * Generate a model schema and enrich it (root and per-property) with the
     * long-tail keywords from the raw spec schema.
     *
     * @param model the codegen model
     * @param rawSchema the raw parsed spec schema for this model, may be null
     * @return the enriched schema node
     */
    public ObjectNode generateModelSchema(CodegenModel model,
            io.swagger.v3.oas.models.media.Schema<?> rawSchema) {
        ObjectNode schemaNode = generateModelSchema(model);
        if (schemaNode != null && rawSchema != null) {
            enrichWithRawKeywords(schemaNode, rawSchema);
        }
        return schemaNode;
    }

    private void enrichWithRawKeywords(ObjectNode schemaNode,
            io.swagger.v3.oas.models.media.Schema<?> rawSchema) {
        try {
            JsonNode raw = io.swagger.v3.core.util.Json31.mapper().valueToTree(rawSchema);
            copyRawKeywords(raw, schemaNode);
            JsonNode rawProps = raw.path("properties");
            JsonNode outProps = schemaNode.path("properties");
            if (rawProps.isObject() && outProps.isObject()) {
                java.util.Iterator<Map.Entry<String, JsonNode>> it = rawProps.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    JsonNode target = outProps.get(entry.getKey());
                    if (target instanceof ObjectNode) {
                        copyRawKeywords(entry.getValue(), (ObjectNode) target);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not enrich schema with raw spec keywords: {}", e.getMessage());
        }
    }

    private void copyRawKeywords(JsonNode raw, ObjectNode target) {
        for (String keyword : RAW_KEYWORDS) {
            JsonNode value = raw.get(keyword);
            if (value != null && !value.isNull()) {
                target.set(keyword, rewriteRefs(value.deepCopy()));
            }
        }
    }

    /**
     * Copied subschemas reference "#/components/schemas/X"; the emitted document
     * keys models under "#/definitions/X".
     */
    private JsonNode rewriteRefs(JsonNode node) {
        if (node instanceof ObjectNode) {
            ObjectNode obj = (ObjectNode) node;
            JsonNode ref = obj.get("$ref");
            if (ref != null && ref.isTextual() && ref.asText().startsWith("#/components/schemas/")) {
                obj.put("$ref", "#/definitions/" + ref.asText().substring("#/components/schemas/".length()));
            }
            obj.forEach(this::rewriteRefs);
        } else if (node.isArray()) {
            node.forEach(this::rewriteRefs);
        }
        return node;
    }

    /**
     * Build the schema for one oneOf/anyOf member: a $ref for model members, an
     * inline primitive schema (type/format/enum/constraints) otherwise.
     *
     * @param member The composed schema member
     * @return An ObjectNode representing the member schema
     */
    private ObjectNode composedMemberSchema(CodegenProperty member) {
        ObjectNode node = objectMapper.createObjectNode();
        if (member.isNull) {
            node.put("type", "null");
            return node;
        }
        if (member.isModel && member.complexType != null && !isPrimitiveType(member.complexType)) {
            node.put("$ref", "#/definitions/" + member.complexType);
            return node;
        }
        // openApiType holds the raw OAS type (integer/number/string/boolean)
        JsonSchemaTypeMapper.applyPrimitiveType(member.openApiType, node);
        if (member.dataFormat != null && !member.dataFormat.isEmpty()) {
            node.put("format", member.dataFormat);
        }
        addEnumValues(member, node);
        processValidationConstraints(member, node);
        return node;
    }

    /**
     * Emit enum values with their JSON types preserved: an integer enum must appear
     * as [1, 2] in the schema, not ["1", "2"], or valid numeric values get rejected.
     */
    private void addEnumValues(CodegenProperty var, ObjectNode node) {
        if (var.allowableValues == null || !(var.allowableValues.get("values") instanceof List)) {
            return;
        }
        List<?> enumValues = (List<?>) var.allowableValues.get("values");
        if (enumValues.isEmpty()) {
            return;
        }
        ArrayNode enumNode = node.putArray("enum");
        for (Object value : enumValues) {
            if (value instanceof Integer) {
                enumNode.add((Integer) value);
            } else if (value instanceof Long) {
                enumNode.add((Long) value);
            } else if (value instanceof Number) {
                enumNode.add(new java.math.BigDecimal(value.toString()));
            } else if (value instanceof Boolean) {
                enumNode.add((Boolean) value);
            } else {
                enumNode.add(String.valueOf(value));
            }
        }
    }

    /**
     * Extract JSON Schema compatible data from a ModelMap.
     *
     * @param modelName The name of the model
     * @param modelMap The model map containing model information
     * @return A JsonNode representing the JSON Schema for the model
     */
    private JsonNode extractJsonSchemaFromModelMap(String modelName, ModelMap modelMap) {
        CodegenModel model = modelMap.getModel();
        return generateModelSchema(model);
    }

    /**
     * Process model properties and add them to the schema.
     *
     * @param vars The list of property variables
     * @param schemaNode The schema node to add properties to
     */
    private void processProperties(List<CodegenProperty> vars, ObjectNode schemaNode) {
        ObjectNode properties = schemaNode.putObject("properties");

        for (CodegenProperty var : vars) {
            // Log the property structure to understand what's available
            if (log.isDebugEnabled()) {
                log.debug("Property name: {}, dataType: {}, isArray: {}, complexType: {}",
                    var.name, var.dataType, var.isArray, var.complexType);
                if (var.items != null) {
                    log.debug("  Items: dataType: {}, isArray: {}, complexType: {}",
                        var.items.dataType, var.items.isArray, var.items.complexType);
                }
            }

            String name = var.name;
            ObjectNode property = properties.putObject(name);

            // Maps and free-form objects: emit additionalProperties instead of a
            // scalar type wrongly derived from the container dataType
            if (var.isMap || var.isFreeFormObject) {
                property.put("type", "object");
                if (var.isMap && var.items != null) {
                    ObjectNode valueSchema = property.putObject("additionalProperties");
                    setItemType(var, valueSchema);
                    processValidationConstraints(var.items, valueSchema);
                    addEnumValues(var.items, valueSchema);
                } else {
                    property.put("additionalProperties", true);
                }
                if (var.description != null && !var.description.isEmpty()) {
                    property.put("description", var.description);
                }
                processValidationConstraints(var, property);
                continue;
            }

            // Set property type
            setPropertyType(var, property);

            // Set property description
            if (var.description != null && !var.description.isEmpty()) {
                property.put("description", var.description);
            }

            // Set property format if available
            if (var.getFormat() != null && !var.getFormat().isEmpty()) {
                property.put("format", var.getFormat());
            }

            // Handle validation constraints
            processValidationConstraints(var, property);

            // Handle references
            if (var.complexType != null && !var.complexType.isEmpty()) {
                String complexType = var.complexType;
                // Check if it's a primitive type
                if (isPrimitiveType(complexType)) {
                    // For an array of primitives complexType holds the ELEMENT type;
                    // setPropertyType already emitted type:array with typed items, so
                    // only scalar properties get the primitive type applied here.
                    if (!var.isArray) {
                        setPrimitiveType(complexType, property);
                    }
                } else if (var.isArray) {
                    // Array of complex type
                    property.put("type", "array");
                    ObjectNode items = property.putObject("items");
                    items.put("$ref", "#/definitions/" + complexType);
                } else {
                    // Direct reference to complex type
                    property.put("$ref", "#/definitions/" + complexType);
                    // Remove type when using $ref as they are mutually exclusive in JSON Schema
                    property.remove("type");
                    property.remove("format");
                }
            }

            // Nullable: JSON null must validate (type arrays are core JSON Schema)
            if (var.isNullable && property.has("type") && property.get("type").isTextual()) {
                String baseType = property.get("type").asText();
                property.putArray("type").add(baseType).add("null");
            }

            // Handle enums
            addEnumValues(var, property);
        }
    }

    /**
     * Process required properties and add them to the schema.
     *
     * @param requiredVars The list of required property variables
     * @param schemaNode The schema node to add required properties to
     */
    private void processRequiredProperties(List<CodegenProperty> requiredVars, ObjectNode schemaNode) {
        ArrayNode required = schemaNode.putArray("required");
        for (CodegenProperty var : requiredVars) {
            // readOnly properties may legally be omitted from requests
            if (var.isReadOnly) {
                continue;
            }
            required.add(var.name);
        }
        if (required.isEmpty()) {
            schemaNode.remove("required");
        }
    }

    /**
     * Set the property type in the JSON Schema.
     *
     * @param var The property variable
     * @param property The property node to set the type on
     */
    private void setPropertyType(CodegenProperty var, ObjectNode property) {
        String dataType = var.dataType;

        // Handle arrays
        if (var.isArray) {
            // Always set type to array for array properties
            property.put("type", "array");
            ObjectNode items = property.putObject("items");
            setItemType(var, items);
            return;
        }

        // Apply type mapping
        JsonSchemaTypeMapper.applyTypeToProperty(dataType, property);
    }

    /**
     * Set the item type for array properties.
     *
     * @param var The property variable
     * @param items The items node to set the type on
     */
    private void setItemType(CodegenProperty var, ObjectNode items) {
        String dataType = var.dataType;

        // Remove "array" from dataType if present
        if (dataType.startsWith("array[") && dataType.endsWith("]")) {
            dataType = dataType.substring(6, dataType.length() - 1);
        }

        // Check if it's a reference to another model first
        if (var.items != null && var.items.complexType != null && !var.items.complexType.isEmpty()) {
            String complexType = var.items.complexType;
            // Check if it's a primitive type
            if (isPrimitiveType(complexType)) {
                // Handle primitive types directly
                JsonSchemaTypeMapper.applyPrimitiveType(complexType, items);
            } else {
                items.put("$ref", "#/definitions/" + complexType);
                // Remove type when using $ref as they are mutually exclusive in JSON Schema
                items.remove("type");
                items.remove("format");
            }
            return;
        }

        // Apply type mapping to items
        JsonSchemaTypeMapper.applyTypeToItems(dataType, items);
    }

    /**
     * Check if a type is a primitive JSON Schema type.
     *
     * @param type The type to check
     * @return true if the type is a primitive JSON Schema type
     */
    private boolean isPrimitiveType(String type) {
        return JsonSchemaTypeMapper.isPrimitiveType(type);
    }

    /**
     * Set the appropriate primitive type in the JSON Schema.
     *
     * @param type The primitive type
     * @param node The node to set the type on
     */
    private void setPrimitiveType(String type, ObjectNode node) {
        JsonSchemaTypeMapper.applyPrimitiveType(type, node);
    }

    /**
     * Process validation constraints and add them to the property.
     *
     * @param var The property variable
     * @param property The property node to add constraints to
     */
    private void processValidationConstraints(CodegenProperty var, ObjectNode property) {
        // Minimum value (numeric exclusiveMinimum form when the bound is exclusive)
        if (var.minimum != null) {
            try {
                property.put(var.exclusiveMinimum ? "exclusiveMinimum" : "minimum",
                        Double.parseDouble(var.minimum));
            } catch (NumberFormatException e) {
                log.warn("Invalid minimum value: {}", var.minimum);
            }
        }

        // Maximum value
        if (var.maximum != null) {
            try {
                property.put(var.exclusiveMaximum ? "exclusiveMaximum" : "maximum",
                        Double.parseDouble(var.maximum));
            } catch (NumberFormatException e) {
                log.warn("Invalid maximum value: {}", var.maximum);
            }
        }

        // multipleOf
        if (var.multipleOf != null) {
            property.put("multipleOf", new java.math.BigDecimal(var.multipleOf.toString()));
        }

        // Object property counts
        if (var.getMinProperties() != null) {
            property.put("minProperties", var.getMinProperties());
        }
        if (var.getMaxProperties() != null) {
            property.put("maxProperties", var.getMaxProperties());
        }

        // Minimum length
        if (var.getMinLength() != null) {
            property.put("minLength", var.getMinLength());
        }

        // Maximum length
        if (var.getMaxLength() != null) {
            property.put("maxLength", var.getMaxLength());
        }

        // Pattern (strip the /.../ delimiters DefaultCodegen wraps spec patterns in;
        // JSON Schema patterns are undelimited)
        if (var.pattern != null) {
            property.put("pattern", Modsecurity3Generator.sanitizeSpecPattern(var.pattern));
        }

        // Minimum items (for arrays)
        if (var.getMinItems() != null) {
            property.put("minItems", var.getMinItems());
        }

        // Maximum items (for arrays)
        if (var.getMaxItems() != null) {
            property.put("maxItems", var.getMaxItems());
        }

        // Unique items (for arrays)
        if (var.getUniqueItems()) {
            property.put("uniqueItems", true);
        }
    }

    /**
     * Exception thrown when there is an error during JSON schema generation.
     */
    public static class SchemaGenerationException extends Exception {

        /**
         * Constructs a new schema generation exception with the specified detail message.
         *
         * @param message the detail message
         */
        public SchemaGenerationException(String message) {
            super(message);
        }

        /**
         * Constructs a new schema generation exception with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause the cause of the exception
         */
        public SchemaGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
