package com.oashield.openapi.generators.modsecurity3.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oashield.openapi.generators.modsecurity3.types.SchemaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the SchemaType enum.
 */
public class SchemaTypeTest {

    private ObjectMapper objectMapper;
    private ObjectNode testNode;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        testNode = objectMapper.createObjectNode();
    }

    @Test
    public void testStringUriType() {
        SchemaType.STRING_URI.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("uri", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringHostnameType() {
        SchemaType.STRING_HOSTNAME.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("hostname", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringIpv4Type() {
        SchemaType.STRING_IPV4.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("ipv4", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringIpv6Type() {
        SchemaType.STRING_IPV6.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("ipv6", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testArrayType() {
        SchemaType.ARRAY.applyToNode(testNode);

        assertEquals("array", testNode.get("type").asText());
        // Array type doesn't set items, it's set separately
        assertTrue(!testNode.has("items"), "Should not have items property yet");
    }

    // ===== Basic Type Tests =====

    @Test
    public void testIntegerType() {
        SchemaType.INTEGER.applyToNode(testNode);

        assertEquals("integer", testNode.get("type").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testNumberType() {
        SchemaType.NUMBER.applyToNode(testNode);

        assertEquals("number", testNode.get("type").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testBooleanType() {
        SchemaType.BOOLEAN.applyToNode(testNode);

        assertEquals("boolean", testNode.get("type").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringType() {
        SchemaType.STRING.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testObjectType() {
        SchemaType.OBJECT.applyToNode(testNode);

        assertEquals("object", testNode.get("type").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    // ===== String Format Type Tests =====

    @Test
    public void testStringDateType() {
        SchemaType.STRING_DATE.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("date", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringDateTimeType() {
        SchemaType.STRING_DATETIME.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("date-time", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringEmailType() {
        SchemaType.STRING_EMAIL.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("email", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringUuidType() {
        SchemaType.STRING_UUID.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("uuid", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    // ===== NEW OpenAPI 3.1 Format Type Tests =====

    @Test
    public void testStringTimeType() {
        SchemaType.STRING_TIME.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("time", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringUriReferenceType() {
        SchemaType.STRING_URI_REFERENCE.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("uri-reference", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringUriTemplateType() {
        SchemaType.STRING_URI_TEMPLATE.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("uri-template", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringJsonPointerType() {
        SchemaType.STRING_JSON_POINTER.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("json-pointer", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    @Test
    public void testStringRelativeJsonPointerType() {
        SchemaType.STRING_RELATIVE_JSON_POINTER.applyToNode(testNode);

        assertEquals("string", testNode.get("type").asText());
        assertEquals("relative-json-pointer", testNode.get("format").asText());
        assertTrue(!testNode.has("$ref"), "Should not have $ref property");
    }

    // ===== Comprehensive Coverage Test =====

    @Test
    public void testAllSchemaTypesRemoveRefProperty() {
        // Test that all schema types properly remove $ref when applied
        SchemaType[] allTypes = SchemaType.values();

        for (SchemaType type : allTypes) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("$ref", "#/components/schemas/SomeModel");

            type.applyToNode(node);

            // All types except ARRAY should remove $ref
            if (type != SchemaType.ARRAY) {
                assertTrue(!node.has("$ref"),
                    "SchemaType " + type.name() + " should remove $ref property");
            }
            assertTrue(node.has("type"),
                "SchemaType " + type.name() + " should set type property");
        }
    }
}
