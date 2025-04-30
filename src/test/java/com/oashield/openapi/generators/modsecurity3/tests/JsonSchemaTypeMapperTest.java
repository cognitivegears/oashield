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
}