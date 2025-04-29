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
}