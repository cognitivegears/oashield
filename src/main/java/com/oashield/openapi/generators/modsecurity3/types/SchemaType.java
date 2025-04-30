package com.oashield.openapi.generators.modsecurity3.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Enum representing different JSON Schema types with their specific behavior.
 */
public enum SchemaType {
    
    INTEGER {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "integer");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    NUMBER {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "number");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    BOOLEAN {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "boolean");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    STRING {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    STRING_DATE {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "date");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    STRING_DATETIME {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "date-time");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    STRING_EMAIL {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "email");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    STRING_UUID {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "uuid");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },

    STRING_URI {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "uri");
            node.remove("$ref");
        }
    },

    STRING_HOSTNAME {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "hostname");
            node.remove("$ref");
        }
    },

    STRING_IPV4 {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "ipv4");
            node.remove("$ref");
        }
    },

    STRING_IPV6 {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "string");
            node.put("format", "ipv6");
            node.remove("$ref");
        }
    },
    
    OBJECT {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "object");
            // Remove any existing $ref for primitive types
            node.remove("$ref");
        }
    },
    
    ARRAY {
        @Override
        public void applyToNode(ObjectNode node) {
            node.put("type", "array");
            // Note: items must be set separately
        }
    };
    
    /**
     * Apply this schema type to a JSON node.
     *
     * @param node The JSON node to set the type on
     */
    public abstract void applyToNode(ObjectNode node);
}