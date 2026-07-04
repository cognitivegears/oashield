package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * schema.json generator coverage (Phase 5): additionalProperties, multipleOf,
 * exclusive bounds, nullable type arrays, readOnly-aware required lists, and
 * object property counts.
 */
public class Phase5SchemaTest {

    @TempDir
    static Path outputDir;
    private static JsonNode thing;

    @BeforeAll
    static void generate() throws IOException {
        new DefaultGenerator()
                .opts(new CodegenConfigurator()
                        .setGeneratorName("modsecurity3")
                        .setInputSpec("src/test/resources/specs/phase3-features.yaml")
                        .setOutputDir(outputDir.toString())
                        .toClientOptInput())
                .generate();
        JsonNode schema = new ObjectMapper().readTree(outputDir.resolve("schema.json").toFile());
        thing = schema.path("definitions").path("Thing");
        assertTrue(thing.isObject(), "Thing definition missing: " + schema);
    }

    @Test
    void mapPropertyEmitsAdditionalPropertiesWithValueSchema() {
        JsonNode attrs = thing.path("properties").path("attrs");
        assertEquals("object", attrs.path("type").asText(), "attrs should be an object: " + attrs);
        assertEquals("^[a-z]{1,20}$", attrs.path("additionalProperties").path("pattern").asText(),
                "map value schema should carry the additionalProperties pattern: " + attrs);
    }

    @Test
    void freeFormObjectEmitsAdditionalPropertiesTrue() {
        JsonNode misc = thing.path("properties").path("misc");
        assertTrue(misc.path("additionalProperties").asBoolean(false),
                "free-form object should emit additionalProperties true: " + misc);
    }

    @Test
    void multipleOfIsEmitted() {
        assertEquals(100, thing.path("properties").path("price").path("multipleOf").asInt(),
                "multipleOf should be emitted: " + thing);
    }

    @Test
    void exclusiveBoundIsEmittedNumerically() {
        JsonNode score = thing.path("properties").path("score");
        assertEquals(0.0, score.path("exclusiveMinimum").asDouble(-1),
                "boolean exclusiveMinimum should become the numeric draft form: " + score);
        assertFalse(score.has("minimum"), "minimum must not also be emitted: " + score);
    }

    @Test
    void nullablePropertyGetsTypeArray() {
        JsonNode type = thing.path("properties").path("nickname").path("type");
        assertTrue(type.isArray(), "nullable property should have a type array: " + thing);
        List<String> types = new ArrayList<>();
        type.forEach(t -> types.add(t.asText()));
        assertTrue(types.contains("string") && types.contains("null"),
                "nullable string should be [string, null]: " + types);
    }

    @Test
    void requiredExcludesReadOnlyProperties() {
        List<String> required = new ArrayList<>();
        thing.path("required").forEach(r -> required.add(r.asText()));
        assertFalse(required.contains("id"), "readOnly required property must be dropped: " + required);
        assertTrue(required.contains("name") && required.contains("nickname"),
                "other required properties must remain: " + required);
    }

    @Test
    void modelLevelMaxPropertiesIsEmitted() {
        assertEquals(10, thing.path("maxProperties").asInt(),
                "model-level maxProperties should be emitted: " + thing);
    }
}
