package com.oashield.openapi.generators.modsecurity3.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end generator tests for anyOf / allOf / oneOf support (issue #13):
 * generated per-field rules and schema.json must both reflect the composition.
 */
public class ComposedSchemaTest {

    private static final String OUTPUT_DIR = "target/test-composed";
    private static String rules;
    private static JsonNode schema;

    @BeforeAll
    public static void generate() throws Exception {
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3")
                .setInputSpec("samples/composed.yaml")
                .setOutputDir(OUTPUT_DIR);
        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        new DefaultGenerator().opts(clientOptInput).generate();

        rules = new String(Files.readAllBytes(new File(OUTPUT_DIR, "DefaultApi.conf").toPath()));
        schema = new ObjectMapper().readTree(new File(OUTPUT_DIR, "schema.json"));
    }

    // --- Rule generation (used by both modsecurity3 and coraza flavors) ---

    @Test
    public void anyOfPrimitivePropertyGetsUnionPatternAndIsAllowlisted() {
        // Contact.id is anyOf [integer, string(uuid)]
        assertTrue(rules.contains("SecRule ARGS:json.id \"!@rx ^(?:[0-9]{1,19}|"),
                "json.id should be validated against an alternation of the anyOf member patterns");
        assertTrue(rules.contains("|json\\.id|"),
                "json.id must appear in the ARGS_NAMES allowlist");
    }

    @Test
    public void oneOfModelPropertyFlattensAllBranchesWithoutRequiringThem() {
        // contactMethod is oneOf [EmailContact, PhoneContact]: both branches validated...
        assertTrue(rules.contains("SecRule ARGS:json.contactMethod.email"),
                "email branch should have a value rule");
        assertTrue(rules.contains("SecRule ARGS:json.contactMethod.phone \"!@rx ^[0-9]{10}$\""),
                "phone branch should keep its spec pattern");
        // ...but neither branch's required properties may be enforced (only one branch is present)
        assertFalse(rules.contains("Missing required property json.contactMethod.email"),
                "required inside a oneOf branch must not produce a presence rule");
        assertFalse(rules.contains("Missing required property json.contactMethod.phone"),
                "required inside a oneOf branch must not produce a presence rule");
        // required outside the composition still applies
        assertTrue(rules.contains("Missing required property json.name"));
    }

    @Test
    public void allOfBodyModelIsFlattenedWithMergedRequired() {
        // Dog = allOf [Animal, {breed}]; previously produced no rules and an empty allowlist
        assertTrue(rules.contains("Missing required property json.species"),
                "required property from the allOf parent must be enforced");
        assertTrue(rules.contains("SecRule ARGS:json.breed"),
                "property from the inline allOf member must be validated");
        assertFalse(rules.contains("ARGS_NAMES \"!@rx ^(?:)$\""),
                "allOf body must not produce an empty ARGS_NAMES allowlist");
        assertTrue(rules.contains("json\\.species") && rules.contains("json\\.breed"),
                "allOf properties must be allowlisted");
    }

    @Test
    public void oneOfQueryParameterGetsUnionPattern() {
        // code is oneOf [integer, string enum]; optional, so the alternation is optional too
        assertTrue(rules.contains("SecRule ARGS_GET:code \"!@rx ^(?:[0-9]{1,19}|(red|green|blue))?$\""),
                "oneOf query parameter should be validated against the member pattern alternation");
    }

    // --- schema.json (used by the coraza @validateSchema rule) ---

    @Test
    public void schemaEmitsOneOfRefsForModelComposition() {
        JsonNode def = schema.get("definitions").get("Contact_contactMethod");
        assertNotNull(def);
        assertTrue(def.has("oneOf"), "composed model should use the oneOf keyword");
        assertFalse(def.has("required"), "union of branch requireds must not be emitted");
        assertEquals("#/definitions/EmailContact", def.get("oneOf").get(0).get("$ref").asText());
        assertEquals("#/definitions/PhoneContact", def.get("oneOf").get(1).get("$ref").asText());
    }

    @Test
    public void schemaEmitsAnyOfPrimitiveMembers() {
        JsonNode def = schema.get("definitions").get("Contact_id");
        assertNotNull(def);
        assertTrue(def.has("anyOf"), "composed model should use the anyOf keyword");
        assertFalse(def.has("type"), "anyOf schema must not also claim type object");
        assertEquals("integer", def.get("anyOf").get(0).get("type").asText());
        assertEquals("string", def.get("anyOf").get(1).get("type").asText());
        assertEquals("uuid", def.get("anyOf").get(1).get("format").asText());
    }

    @Test
    public void schemaMergesAllOfIntoObject() {
        JsonNode dog = schema.get("definitions").get("Dog");
        assertNotNull(dog);
        assertEquals("object", dog.get("type").asText());
        assertTrue(dog.get("properties").has("species"));
        assertTrue(dog.get("properties").has("breed"));
        assertEquals("species", dog.get("required").get(0).asText());
    }

    @Test
    public void schemaPatternsAreUndelimited() {
        JsonNode phone = schema.get("definitions").get("PhoneContact").get("properties").get("phone");
        assertEquals("^[0-9]{10}$", phone.get("pattern").asText(),
                "spec patterns must not keep DefaultCodegen's /.../ delimiters");
    }
}
