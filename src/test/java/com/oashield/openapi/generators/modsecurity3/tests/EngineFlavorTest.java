package com.oashield.openapi.generators.modsecurity3.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;

/**
 * Tests for the engineFlavor option: rules shared by both engines, plus the
 * Coraza-only @validateSchema rule.
 */
public class EngineFlavorTest {

    @TempDir
    Path tempDir;

    private String generate(String spec, Map<String, Object> additionalProperties) throws IOException {
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3")
                .setInputSpec(spec)
                .setOutputDir(tempDir.toString());
        for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
            configurator.addAdditionalProperty(entry.getKey(), entry.getValue());
        }
        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
        return Files.readString(tempDir.resolve("PetApi.conf"));
    }

    @Test
    public void defaultFlavorEmitsPerFieldRulesWithoutCorazaOperators() throws IOException {
        String conf = generate("samples/petstore.yaml", new HashMap<>());

        // issue #42: no Coraza-only constructs
        assertFalse(conf.contains("@restpath"), "modsecurity3 output must not use @restpath");
        assertFalse(conf.contains("ARGS_PATH"), "modsecurity3 output must not use ARGS_PATH");
        // @validateSchema is Coraza-only (ModSecurity3's variant is XSD-only)
        assertFalse(conf.contains("SecRule REQUEST_BODY \"@validateSchema"), "modsecurity3 output must not use @validateSchema");

        // path parameter validation is embedded in the route regex
        assertTrue(conf.contains("SecRule REQUEST_FILENAME \"!@rx ^/v2/pet/(?:[0-9]{1,19})$\""),
                "path param pattern should be embedded in the path regex");

        // issue #14: per-field body validation
        assertTrue(conf.contains("SecRule &ARGS:json.name \"@eq 0\""), "required property presence rule");
        assertTrue(conf.contains("SecRule ARGS:json.id \"!@rx ^[0-9]{1,19}$\""), "typed property rule");
        assertTrue(conf.contains("ARGS:/(?i)^json\\.photoUrls\\.(?:array_)?\\d{1,9}$/"),
                "array element rule must match both engines' index forms");
        assertTrue(conf.contains("SecRule ARGS:json.status \"!@rx ^(available|pending|sold)$\""), "enum rule");
        // additionalProperties enforcement incl. container prefixes (Coraza lists them)
        assertTrue(conf.contains("SecRule ARGS_NAMES \"!@rx ^(?:"), "ARGS_NAMES allowlist rule");
        assertTrue(conf.contains("json\\.category|json\\.category\\.id"), "allowlist includes container prefixes");
    }

    @Test
    public void corazaFlavorAddsValidateSchemaRule() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("engineFlavor", "coraza");
        props.put("schemaRulePath", "rules/schema.json");
        String conf = generate("samples/petstore.yaml", props);

        assertTrue(conf.contains("SecRule REQUEST_BODY \"@validateSchema rules/schema.json\""),
                "coraza flavor emits @validateSchema with the configured rule path");
        assertFalse(conf.contains("@restpath"), "coraza flavor also uses the unified path regex");
        // per-field rules are shared across flavors
        assertTrue(conf.contains("SecRule &ARGS:json.name \"@eq 0\""), "per-field rules also emitted for coraza");
    }

    @Test
    public void validateBodySchemaFalseSuppressesBodyRules() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("engineFlavor", "coraza");
        props.put("validateBodySchema", "false");
        String conf = generate("samples/petstore.yaml", props);

        assertFalse(conf.contains("SecRule REQUEST_BODY \"@validateSchema"), "no schema rule when validateBodySchema=false");
        assertFalse(conf.contains("SecRule &ARGS:json.name"), "no per-field rules when validateBodySchema=false");
    }

    @Test
    public void unknownFlavorFails() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        generator.additionalProperties().put("engineFlavor", "apache");
        assertThrows(IllegalArgumentException.class, generator::processOpts);
    }

    @Test
    public void numericRangeRulesUseComparisonOperators() throws IOException {
        String conf = generate("samples/getparam.yaml", new HashMap<>());

        assertTrue(conf.contains("SecRule ARGS_GET:limit \"@lt 1\""), "minimum enforced with @lt");
        assertTrue(conf.contains("SecRule ARGS_GET:limit \"@gt 100\""), "maximum enforced with @gt");
        assertTrue(conf.contains("SecRule ARGS_GET:limit \"!@rx ^([0-9]{1,19})?$\""),
                "integer type pattern stays bounded, range is not regex-encoded");
    }

    @Test
    public void escapeRegexLiteralEscapesMetacharacters() {
        assertEquals("application/vnd\\.api\\+json",
                Modsecurity3Generator.escapeRegexLiteral("application/vnd.api+json"));
        assertEquals("plain", Modsecurity3Generator.escapeRegexLiteral("plain"));
    }

    @Test
    public void sanitizeSpecPatternStripsDelimitersAndUnescapes() {
        assertEquals("^a\\.b$", Modsecurity3Generator.sanitizeSpecPattern("/^a\\\\.b$/"));
        assertEquals("^plain$", Modsecurity3Generator.sanitizeSpecPattern("^plain$"));
        assertEquals(null, Modsecurity3Generator.sanitizeSpecPattern(null));
    }

    @Test
    public void flattenModelBoundsRecursionOnCyclicModels() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        org.openapitools.codegen.CodegenProperty self = new org.openapitools.codegen.CodegenProperty();
        self.baseName = "node";
        self.isModel = true;
        self.vars = java.util.Collections.singletonList(self);

        // must terminate (depth cap) instead of overflowing the stack
        assertTrue(generator.flattenModel(self, "json.").isEmpty());
    }
}
