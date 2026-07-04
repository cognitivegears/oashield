package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oashield.openapi.generators.modsecurity3.PatternGenerationService;

/**
 * Regression tests for the confirmed generator bugs: mangled spec patterns,
 * blocked root-array bodies, path patterns crossing segment boundaries,
 * allowEmptyValue, and numeric enums.
 */
public class Phase1FixesTest {

    @TempDir
    static Path outputDir;
    private static String conf;
    private static JsonNode schema;

    @BeforeAll
    static void generate() throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("src/test/resources/specs/phase1-features.yaml")
                        .setOutputDir(outputDir.toString())
                        .toClientOptInput())
                .generate();
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
        schema = new ObjectMapper().readTree(outputDir.resolve("schema.json").toFile());
    }

    @Test
    void specPatternsAreEmittedUnmangled() {
        // Spec pattern for the query param appears undelimited in the @rx operand
        assertTrue(conf.contains("ARGS_GET:q \"!@rx ^[a-zA-Z0-9.]{1,20}$\""),
                "spec pattern should be emitted without /.../ delimiters:\n" + conf);
        // No rule anywhere carries the DefaultCodegen-mangled /^...$/ form
        assertFalse(conf.contains("@rx /^"), "mangled delimited pattern leaked into rules:\n" + conf);
    }

    @Test
    void specPatternEmbedsIntoPathRegex() {
        assertTrue(conf.contains("^/users/(?:[a-z]{3,10})$"),
                "path regex should embed the sanitized spec pattern:\n" + conf);
    }

    @Test
    void genericStringPathParamsDoNotCrossSegments() {
        assertTrue(conf.contains("^/files/(?:[^/]+)$"),
                "pattern-less string path params should match a single segment:\n" + conf);
    }

    @Test
    void rootArrayBodyOfModelsIsFlattened() {
        assertTrue(conf.contains("json\\.(?:array_)?\\d{1,9}\\.username"),
                "root-array element fields should be validated and allowlisted:\n" + conf);
        // the array-body operation's ARGS_NAMES allowlist covers the bare json
        // container (Coraza lists it for root arrays), index keys, and fields
        assertTrue(conf.contains(
                "^(?:json|json\\.(?:array_)?\\d{1,9}|json\\.(?:array_)?\\d{1,9}\\.username|json\\.(?:array_)?\\d{1,9}\\.level)$"),
                "root-array body allowlist should cover index keys and element fields:\n" + conf);
    }

    @Test
    void rootArrayBodyOfPrimitivesIsFlattened() {
        assertTrue(conf.contains("/(?i)^json\\.(?:array_)?\\d{1,9}$/"),
                "root-array primitive elements should get an indexed selector:\n" + conf);
    }

    @Test
    void allowEmptyValueQueryParamAcceptsEmpty() {
        assertTrue(conf.contains("ARGS_GET:flag \"!@rx ^(?:[a-z]+)?$\""),
                "allowEmptyValue param should accept an empty value:\n" + conf);
    }

    @Test
    void numericEnumsEmitTypedSchemaValues() {
        JsonNode enumNode = schema.path("definitions").path("BulkUser")
                .path("properties").path("level").path("enum");
        assertTrue(enumNode.isArray() && enumNode.size() == 3,
                "BulkUser.level should have a 3-value enum: " + schema);
        assertTrue(enumNode.get(0).isIntegralNumber(),
                "integer enum values must be emitted as JSON numbers, got: " + enumNode);
    }

    @Test
    void numericEnumPatternGenerationDoesNotCrash() {
        PatternGenerationService service = new PatternGenerationService();
        CodegenParameter param = new CodegenParameter();
        param.baseName = "level";
        param.isEnum = true;
        Map<String, Object> allowable = new HashMap<>();
        allowable.put("values", Arrays.asList(1, 2, 3));
        param.allowableValues = allowable;
        assertEquals("(1|2|3)", service.getAllowedInputPattern(param));
    }
}
