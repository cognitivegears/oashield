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
        
        // Number types
        TYPE_MAPPING.put("float", SchemaType.NUMBER);
        TYPE_MAPPING.put("double", SchemaType.NUMBER);
        TYPE_MAPPING.put("number", SchemaType.NUMBER);
        TYPE_MAPPING.put("decimal", SchemaType.NUMBER);
        
        // Boolean type
        TYPE_MAPPING.put("boolean", SchemaType.BOOLEAN);
        
        // String types
        TYPE_MAPPING.put("string", SchemaType.STRING);
        TYPE_MAPPING.put("date", SchemaType.STRING_DATE);
        TYPE_MAPPING.put("date-time", SchemaType.STRING_DATETIME);
        TYPE_MAPPING.put("DateTime", SchemaType.STRING_DATETIME);
        TYPE_MAPPING.put("byte", SchemaType.STRING);
        TYPE_MAPPING.put("binary", SchemaType.STRING);
        TYPE_MAPPING.put("password", SchemaType.STRING);
        TYPE_MAPPING.put("email", SchemaType.STRING_EMAIL);
        TYPE_MAPPING.put("uuid", SchemaType.STRING_UUID);
        
        // Object type
        TYPE_MAPPING.put("object", SchemaType.OBJECT);
        
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
    public static boolean isPrimitiveType(String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    /**
     * Apply the type mapping to a JSON Schema property node.
     *
     * @param dataType The OpenAPI data type
     * @param property The property node to set the type on
     */
    public static void applyTypeToProperty(String dataType, ObjectNode property) {
        SchemaType schemaType = TYPE_MAPPING.getOrDefault(dataType, SchemaType.STRING);
        schemaType.applyToNode(property);
    }

    /**
     * Apply the type mapping to a JSON Schema node for an array item.
     *
     * @param dataType The OpenAPI data type
     * @param items The items node to set the type on
     */
    public static void applyTypeToItems(String dataType, ObjectNode items) {
        SchemaType schemaType = TYPE_MAPPING.getOrDefault(dataType, SchemaType.STRING);
        schemaType.applyToNode(items);
    }

    /**
     * Apply the primitive type mapping to a JSON Schema node.
     *
     * @param type The primitive type
     * @param node The node to set the type on
     */
    public static void applyPrimitiveType(String type, ObjectNode node) {
        SchemaType schemaType = TYPE_MAPPING.getOrDefault(type, SchemaType.STRING);
        schemaType.applyToNode(node);
    }
}