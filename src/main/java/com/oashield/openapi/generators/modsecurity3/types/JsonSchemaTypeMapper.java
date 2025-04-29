package com.oashield.openapi.generators.modsecurity3.types;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps OpenAPI types to JSON Schema types.
 */
@Slf4j
public class JsonSchemaTypeMapper {

    private static final Map<String, SchemaType> TYPE_MAPPING = new HashMap<>();
    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>();

    static {
        // Integer types
        TYPE_MAPPING.put("integer", SchemaType.INTEGER);
        TYPE_MAPPING.put("int", SchemaType.INTEGER);
        TYPE_MAPPING.put("long", SchemaType.INTEGER);
        TYPE_MAPPING.put("int32", SchemaType.INTEGER);
        TYPE_MAPPING.put("int64", SchemaType.INTEGER);
        
        // Number types
        TYPE_MAPPING.put("float", SchemaType.NUMBER);
        TYPE_MAPPING.put("double", SchemaType.NUMBER);
        TYPE_MAPPING.put("number", SchemaType.NUMBER);
        TYPE_MAPPING.put("decimal", SchemaType.NUMBER);
        
        // Boolean type
        TYPE_MAPPING.put("boolean", SchemaType.BOOLEAN);
        TYPE_MAPPING.put("bool", SchemaType.BOOLEAN);
        
        // String types
        TYPE_MAPPING.put("string", SchemaType.STRING);
        TYPE_MAPPING.put("date", SchemaType.STRING_DATE);
        TYPE_MAPPING.put("date-time", SchemaType.STRING_DATETIME);
        TYPE_MAPPING.put("datetime", SchemaType.STRING_DATETIME);
        TYPE_MAPPING.put("DateTime", SchemaType.STRING_DATETIME);
        TYPE_MAPPING.put("byte", SchemaType.STRING);
        TYPE_MAPPING.put("binary", SchemaType.STRING);
        TYPE_MAPPING.put("base64", SchemaType.STRING);
        TYPE_MAPPING.put("password", SchemaType.STRING);
        TYPE_MAPPING.put("email", SchemaType.STRING_EMAIL);
        TYPE_MAPPING.put("uuid", SchemaType.STRING_UUID);
        TYPE_MAPPING.put("uri", SchemaType.STRING_URI);
        TYPE_MAPPING.put("url", SchemaType.STRING_URI);
        TYPE_MAPPING.put("hostname", SchemaType.STRING_HOSTNAME);
        TYPE_MAPPING.put("ipv4", SchemaType.STRING_IPV4);
        TYPE_MAPPING.put("ipv6", SchemaType.STRING_IPV6);
        
        // Object type
        TYPE_MAPPING.put("object", SchemaType.OBJECT);
        TYPE_MAPPING.put("map", SchemaType.OBJECT);
        
        // Array type
        TYPE_MAPPING.put("array", SchemaType.ARRAY);
        TYPE_MAPPING.put("list", SchemaType.ARRAY);
        
        // Initialize primitive types set
        PRIMITIVE_TYPES.addAll(TYPE_MAPPING.keySet());
        // Add null type which is a primitive but handled differently
        PRIMITIVE_TYPES.add("null");
    }

    /**
     * Check if a type is a primitive JSON Schema type.
     *
     * @param type The type to check
     * @return true if the type is a primitive JSON Schema type
     */
    public boolean isPrimitiveType(String type) {
        if (type == null) {
            return false;
        }
        return PRIMITIVE_TYPES.contains(type.toLowerCase());
    }

    /**
     * Apply the type mapping to a JSON Schema property node.
     *
     * @param dataType The OpenAPI data type
     * @param property The property node to set the type on
     */
    public void applyTypeToProperty(String dataType, ObjectNode property) {
        if (dataType == null) {
            log.warn("Null data type provided, defaulting to string");
            SchemaType.STRING.applyToNode(property);
            return;
        }
        
        SchemaType schemaType = TYPE_MAPPING.getOrDefault(dataType.toLowerCase(), SchemaType.STRING);
        schemaType.applyToNode(property);
    }

    /**
     * Apply the type mapping to a JSON Schema node for an array item.
     *
     * @param dataType The OpenAPI data type
     * @param items The items node to set the type on
     */
    public void applyTypeToItems(String dataType, ObjectNode items) {
        if (dataType == null) {
            log.warn("Null data type provided for array items, defaulting to string");
            SchemaType.STRING.applyToNode(items);
            return;
        }
        
        SchemaType schemaType = TYPE_MAPPING.getOrDefault(dataType.toLowerCase(), SchemaType.STRING);
        schemaType.applyToNode(items);
    }

    /**
     * Apply the primitive type mapping to a JSON Schema node.
     *
     * @param type The primitive type
     * @param node The node to set the type on
     */
    public void applyPrimitiveType(String type, ObjectNode node) {
        if (type == null) {
            log.warn("Null primitive type provided, defaulting to string");
            SchemaType.STRING.applyToNode(node);
            return;
        }
        
        SchemaType schemaType = TYPE_MAPPING.getOrDefault(type.toLowerCase(), SchemaType.STRING);
        schemaType.applyToNode(node);
    }

    /**
     * Register a custom type mapping. This allows for extension of the default mappings.
     *
     * @param openApiType The OpenAPI type name
     * @param schemaType The corresponding SchemaType
     * @throws IllegalArgumentException if either parameter is null
     */
    public void registerTypeMapping(String openApiType, SchemaType schemaType) {
        if (openApiType == null || schemaType == null) {
            throw new IllegalArgumentException("Type name and schema type cannot be null");
        }
        
        TYPE_MAPPING.put(openApiType.toLowerCase(), schemaType);
        PRIMITIVE_TYPES.add(openApiType.toLowerCase());
        log.debug("Registered custom type mapping: {} -> {}", openApiType, schemaType);
    }
}