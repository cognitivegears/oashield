package com.oashield.openapi.generators.modsecurity3.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oashield.openapi.generators.modsecurity3.types.JsonSchemaTypeMapper;
import com.oashield.openapi.generators.modsecurity3.types.SchemaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the JsonSchemaTypeMapper class.
 */
public class JsonSchemaTypeMapperTest {

    private ObjectMapper objectMapper;
    private JsonSchemaTypeMapper typeMapper;
    private ObjectNode testNode;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        typeMapper = new JsonSchemaTypeMapper();
        testNode = objectMapper.createObjectNode();
    }

    @Test
    public void testIsPrimitiveTypeWithNullType() {
        assertFalse(typeMapper.isPrimitiveType(null));
    }

    @Test
    public void testApplyTypeToPropertyWithNullType() {
        typeMapper.applyTypeToProperty(null, testNode);

        assertEquals("string", testNode.get("type").asText());
    }

    @Test
    public void testApplyTypeToItemsWithNullType() {
        typeMapper.applyTypeToItems(null, testNode);

        assertEquals("string", testNode.get("type").asText());
    }

    @Test
    public void testApplyPrimitiveTypeWithNullType() {
        typeMapper.applyPrimitiveType(null, testNode);

        assertEquals("string", testNode.get("type").asText());
    }

    @Test
    public void testCaseInsensitiveTypeMapping() {
        // Test with uppercase type name
        typeMapper.applyTypeToProperty("STRING", testNode);
        assertEquals("string", testNode.get("type").asText());

        // Clear the node
        testNode.removeAll();

        // Test with mixed case type name
        typeMapper.applyTypeToProperty("StRiNg", testNode);
        assertEquals("string", testNode.get("type").asText());
    }

    // ===== NEW OpenAPI 3.1 Data Type Mapping Tests =====

    @Test
    public void testTimeFormatMapping() {
        typeMapper.applyTypeToProperty("time", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("time", testNode.get("format").asText());
    }

    @Test
    public void testUriFormatMapping() {
        typeMapper.applyTypeToProperty("uri", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("uri", testNode.get("format").asText());
    }

    @Test
    public void testUriReferenceFormatMapping() {
        typeMapper.applyTypeToProperty("uri-reference", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("uri-reference", testNode.get("format").asText());
    }

    @Test
    public void testUriTemplateFormatMapping() {
        typeMapper.applyTypeToProperty("uri-template", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("uri-template", testNode.get("format").asText());
    }

    @Test
    public void testHostnameFormatMapping() {
        typeMapper.applyTypeToProperty("hostname", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("hostname", testNode.get("format").asText());
    }

    @Test
    public void testIpv4FormatMapping() {
        typeMapper.applyTypeToProperty("ipv4", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("ipv4", testNode.get("format").asText());
    }

    @Test
    public void testIpv6FormatMapping() {
        typeMapper.applyTypeToProperty("ipv6", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("ipv6", testNode.get("format").asText());
    }

    @Test
    public void testJsonPointerFormatMapping() {
        typeMapper.applyTypeToProperty("json-pointer", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("json-pointer", testNode.get("format").asText());
    }

    @Test
    public void testRelativeJsonPointerFormatMapping() {
        typeMapper.applyTypeToProperty("relative-json-pointer", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("relative-json-pointer", testNode.get("format").asText());
    }

    // ===== Test Type Mapping Methods =====

    @Test
    public void testApplyTypeToItemsWithNewFormats() {
        // Test that applyTypeToItems works with new format types
        typeMapper.applyTypeToItems("time", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("time", testNode.get("format").asText());

        testNode.removeAll();
        typeMapper.applyTypeToItems("hostname", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("hostname", testNode.get("format").asText());
    }

    @Test
    public void testApplyPrimitiveTypeWithNewFormats() {
        // Test that applyPrimitiveType works with new format types
        typeMapper.applyPrimitiveType("uri-reference", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("uri-reference", testNode.get("format").asText());

        testNode.removeAll();
        typeMapper.applyPrimitiveType("json-pointer", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("json-pointer", testNode.get("format").asText());
    }

    @Test
    public void testIsPrimitiveTypeWithNewFormats() {
        // Test that all new OpenAPI 3.1 formats are recognized as primitive types
        assertTrue(typeMapper.isPrimitiveType("time"));
        assertTrue(typeMapper.isPrimitiveType("uri"));
        assertTrue(typeMapper.isPrimitiveType("uri-reference"));
        assertTrue(typeMapper.isPrimitiveType("uri-template"));
        assertTrue(typeMapper.isPrimitiveType("hostname"));
        assertTrue(typeMapper.isPrimitiveType("ipv4"));
        assertTrue(typeMapper.isPrimitiveType("ipv6"));
        assertTrue(typeMapper.isPrimitiveType("json-pointer"));
        assertTrue(typeMapper.isPrimitiveType("relative-json-pointer"));
    }

    @Test
    public void testExistingFormatTypesStillWork() {
        // Ensure existing format types still work correctly
        typeMapper.applyTypeToProperty("date", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("date", testNode.get("format").asText());

        testNode.removeAll();
        typeMapper.applyTypeToProperty("email", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("email", testNode.get("format").asText());

        testNode.removeAll();
        typeMapper.applyTypeToProperty("uuid", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertEquals("uuid", testNode.get("format").asText());
    }

    @Test
    public void testUnknownTypeDefaultsToString() {
        // Test that unknown types default to string
        typeMapper.applyTypeToProperty("unknown-format", testNode);
        assertEquals("string", testNode.get("type").asText());
        assertFalse(testNode.has("format"));
    }
}
