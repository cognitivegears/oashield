package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.xml.sax.SAXException;

/**
 * XSD generation (Phase 6): the generated schema.xsd must be a valid XML Schema
 * (parsed with JAXP) that accepts conforming documents and rejects violations,
 * and the @validateSchema XML rule is emitted only when validateXmlSchema=true.
 */
public class XsdGeneratorTest {

    @TempDir
    static Path outputDir;
    private static String xsd;
    private static String conf;

    @BeforeAll
    static void generate() throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("samples/xmlbody.yaml")
                        .setOutputDir(outputDir.toString())
                        .addAdditionalProperty("validateXmlSchema", "true")
                        .toClientOptInput())
                .generate();
        xsd = Files.readString(outputDir.resolve("schema.xsd"));
        try (Stream<Path> files = Files.list(outputDir)) {
            conf = files.filter(p -> p.getFileName().toString().endsWith("Api.conf"))
                    .map(p -> {
                        try {
                            return Files.readString(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }
    }

    private static Validator validator() throws SAXException {
        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(new StreamSource(new StringReader(xsd)));
        return schema.newValidator();
    }

    private static void validate(String xml) throws SAXException, IOException {
        validator().validate(new StreamSource(new StringReader(xml)));
    }

    @Test
    void generatedXsdIsAValidSchema() throws SAXException {
        validator();
    }

    @Test
    void conformingDocumentValidates() throws SAXException, IOException {
        validate("<pet id=\"5\"><name>Rex</name><status>available</status>"
                + "<weight>4.2</weight><tags><tag>small</tag><tag>brown</tag></tags></pet>");
    }

    @Test
    void minimalRequiredDocumentValidates() throws SAXException, IOException {
        validate("<pet><name>Rex</name></pet>");
    }

    @Test
    void missingRequiredElementIsRejected() {
        assertThrows(SAXException.class, () -> validate("<pet><status>available</status></pet>"));
    }

    @Test
    void patternViolationIsRejected() {
        assertThrows(SAXException.class, () -> validate("<pet><name>R3x!</name></pet>"));
    }

    @Test
    void enumViolationIsRejected() {
        assertThrows(SAXException.class,
                () -> validate("<pet><name>Rex</name><status>hibernating</status></pet>"));
    }

    @Test
    void nonIntegerAttributeIsRejected() {
        assertThrows(SAXException.class, () -> validate("<pet id=\"abc\"><name>Rex</name></pet>"));
    }

    @Test
    void validateSchemaRuleEmittedOnlyWhenEnabled(@TempDir Path defaultOut) throws IOException {
        assertTrue(conf.contains("SecRule XML \"@validateSchema schema.xsd\""),
                "opt-in run should emit the XML @validateSchema rule:\n" + conf);

        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("samples/xmlbody.yaml")
                        .setOutputDir(defaultOut.toString())
                        .toClientOptInput())
                .generate();
        String defaultConf;
        try (Stream<Path> files = Files.list(defaultOut)) {
            defaultConf = files.filter(p -> p.getFileName().toString().endsWith("Api.conf"))
                    .map(p -> {
                        try {
                            return Files.readString(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.joining("\n"));
        }
        assertFalse(defaultConf.contains("@validateSchema schema.xsd"),
                "default run must not emit XML @validateSchema (engine cannot load XSDs):\n" + defaultConf);
        assertFalse(Files.exists(defaultOut.resolve("schema.xsd")),
                "default run must not write schema.xsd");
    }
}
