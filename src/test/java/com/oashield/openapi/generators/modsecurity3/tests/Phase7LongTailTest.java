package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OpenAPI 3.1 long tail (Phase 7): const, prefixItems, patternProperties,
 * dependentRequired, if/then/else, and content: parameters — per-field rules
 * where tractable, schema.json keywords for Coraza otherwise.
 */
public class Phase7LongTailTest {

    @TempDir
    static Path outputDir;
    private static String conf;
    private static JsonNode event;

    @BeforeAll
    static void generate() throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("samples/oas31.yaml")
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
        JsonNode schema = new ObjectMapper().readTree(outputDir.resolve("schema.json").toFile());
        event = schema.path("definitions").path("Event");
        assertTrue(event.isObject(), "Event definition missing: " + schema);
    }

    @Test
    void constBecomesExactMatchRule() {
        assertTrue(conf.contains("ARGS:json.kind \"!@rx ^reminder$\""),
                "const should produce an exact-match value rule:\n" + conf);
    }

    @Test
    void dependentRequiredBecomesChainedRules() {
        assertTrue(conf.contains("SecRule &ARGS:json.end \"@gt 0\"")
                        && conf.contains("SecRule &ARGS:json.start \"@eq 0\""),
                "dependentRequired should produce chained presence rules:\n" + conf);
    }

    @Test
    void patternPropertiesRestrictAllowlistAndValidateValues() {
        assertTrue(conf.contains("json\\.labels\\.^x-[^.]*".replace("^x-", "x-"))
                        || conf.contains("json\\.labels\\.x-[^.]*"),
                "patternProperties names should be allowlisted by their pattern:\n" + conf);
        assertFalse(conf.contains("json\\.labels\\..{1,256}"),
                "patternProperties must replace the broad free-form wildcard:\n" + conf);
        assertTrue(conf.contains("ARGS:/^json\\.labels\\.x-[^.]*$/ \"!@rx ^-?[0-9]{1,19}$\""),
                "patternProperties values should be validated by type:\n" + conf);
    }

    @Test
    void contentParameterGetsBoundedPatternAndAllowlistEntry() {
        assertTrue(conf.contains("ARGS_GET:meta \"!@rx ^[\\s\\S]{0,"),
                "content: param should get a bounded length cap:\n" + conf);
        assertTrue(conf.matches("(?s).*ARGS_NAMES \"!@rx \\^\\(\\?:[^\"]*meta[^\"]*\\)\\$\".*"),
                "content: param name should be allowlisted:\n" + conf);
    }

    @Test
    void schemaCarriesRawKeywords() {
        assertEquals("reminder", event.path("properties").path("kind").path("const").asText(),
                "const should be copied into schema.json: " + event);
        assertTrue(event.path("properties").path("window").path("prefixItems").isArray(),
                "prefixItems should be copied: " + event);
        assertTrue(event.path("properties").path("labels").path("patternProperties").has("^x-"),
                "patternProperties should be copied: " + event);
        assertTrue(event.path("dependentRequired").has("end"),
                "dependentRequired should be copied: " + event);
        assertTrue(event.has("if") && event.has("then"),
                "if/then should be copied: " + event);
    }
}
