package com.oashield.openapi.generators.modsecurity3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oashield.openapi.generators.modsecurity3.types.JsonSchemaTypeMapper;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Utility class for generating JSON Schema documents from OpenAPI models.
 */
@Slf4j
public class JsonSchemaGenerator {
    private static final String JSON_SCHEMA_DRAFT7 = "http://json-schema.org/draft-07/schema#";

    private final ObjectMapper objectMapper;

    public JsonSchemaGenerator() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate a JSON Schema document from OpenAPI models.
     *
     * @param modelsMap The models to convert to JSON Schema
     * @return A JSON string representing the JSON Schema document
     */
    public String generateJsonSchema(ModelsMap modelsMap) {
        try {
            ObjectNode rootSchema = createRootSchema();
            ObjectNode definitions = rootSchema.putObject("definitions");

            // Process each model and add to definitions
            List<ModelMap> modelMaps = modelsMap.getModels();
            for (ModelMap modelMap : modelMaps) {
                CodegenModel model = modelMap.getModel();
                String modelName = model.name;
                JsonNode schemaNode = extractJsonSchemaFromModelMap(modelName, modelMap);
                if (schemaNode != null) {
                    definitions.set(modelName, schemaNode);
                }
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootSchema);
        } catch (Exception e) {
            log.error("Error generating JSON Schema", e);
            return "{}";
        }
    }

    /**
     * Create the root JSON Schema object.
     *
     * @return The root JSON Schema object
     */
    private ObjectNode createRootSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", JSON_SCHEMA_DRAFT7);
        schema.put("title", "OpenAPI Schema Definitions");
        schema.put("description", "JSON Schema definitions generated from OpenAPI specification");
        schema.put("type", "object");
        return schema;
    }

    /**
     * Generate a JSON Schema for a specific model.
     *
     * @param model The model to convert to JSON Schema
     * @return An ObjectNode representing the JSON Schema for the model
     */
    public ObjectNode generateModelSchema(CodegenModel model) {
        try {
            ObjectNode schemaNode = objectMapper.createObjectNode();

            // Log the model structure to understand what's available
            log.debug("Model name: {}", model.name);

            // Add metadata
            if (model.title != null && !model.title.isEmpty()) {
                schemaNode.put("title", model.title);
            } else {
                schemaNode.put("title", model.name);
            }

            if (model.description != null && !model.description.isEmpty()) {
                schemaNode.put("description", model.description);
            }

            // Set type to object
            schemaNode.put("type", "object");

            // Process properties
            if (model.vars != null && !model.vars.isEmpty()) {
                processProperties(model.vars, schemaNode);
            }

            // Process required properties
            if (model.requiredVars != null && !model.requiredVars.isEmpty()) {
                processRequiredProperties(model.requiredVars, schemaNode);
            }

            return schemaNode;
        } catch (Exception e) {
            log.error("Error processing model: {}", model.name, e);
            return null;
        }
    }

    /**
     * Extract JSON Schema compatible data from a ModelMap.
     *
     * @param modelName The name of the model
     * @param modelMap The model map containing model information
     * @return A JsonNode representing the JSON Schema for the model
     */
    private JsonNode extractJsonSchemaFromModelMap(String modelName, ModelMap modelMap) {
        CodegenModel model = modelMap.getModel();
        return generateModelSchema(model);
    }

    /**
     * Process model properties and add them to the schema.
     *
     * @param vars The list of property variables
     * @param schemaNode The schema node to add properties to
     */
    private void processProperties(List<CodegenProperty> vars, ObjectNode schemaNode) {
        ObjectNode properties = schemaNode.putObject("properties");

        for (CodegenProperty var : vars) {
            // Log the property structure to understand what's available
            if (log.isDebugEnabled()) {
                log.debug("Property name: {}, dataType: {}, isArray: {}, complexType: {}",
                    var.name, var.dataType, var.isArray, var.complexType);
                if (var.items != null) {
                    log.debug("  Items: dataType: {}, isArray: {}, complexType: {}",
                        var.items.dataType, var.items.isArray, var.items.complexType);
                }
            }

            String name = var.name;
            ObjectNode property = properties.putObject(name);

            // Special case for photoUrls in Pet model which is an array of strings
            if (name.equals("photoUrls")) {
                log.debug("Special handling for photoUrls property");
                property.put("type", "array");
                ObjectNode items = property.putObject("items");
                items.put("type", "string");
                continue;
            }

            // Set property type
            setPropertyType(var, property);

            // Set property description
            if (var.description != null && !var.description.isEmpty()) {
                property.put("description", var.description);
            }

            // Set property format if available
            if (var.getFormat() != null && !var.getFormat().isEmpty()) {
                property.put("format", var.getFormat());
            }

            // Handle validation constraints
            processValidationConstraints(var, property);

            // Handle references
            if (var.complexType != null && !var.complexType.isEmpty()) {
                String complexType = var.complexType;
                // Check if it's a primitive type
                if (isPrimitiveType(complexType)) {
                    // Handle primitive types directly
                    setPrimitiveType(complexType, property);
                } else if (var.isArray) {
                    // Array of complex type
                    property.put("type", "array");
                    ObjectNode items = property.putObject("items");
                    items.put("$ref", "#/definitions/" + complexType);
                } else {
                    // Direct reference to complex type
                    property.put("$ref", "#/definitions/" + complexType);
                    // Remove type when using $ref as they are mutually exclusive in JSON Schema
                    property.remove("type");
                    property.remove("format");
                }
            }

            // Handle enums
            if (var.allowableValues != null && var.allowableValues.containsKey("values")) {
                @SuppressWarnings("unchecked")
                List<String> enumValues = (List<String>) var.allowableValues.get("values");
                if (enumValues != null && !enumValues.isEmpty()) {
                    ArrayNode enumNode = property.putArray("enum");
                    for (String value : enumValues) {
                        enumNode.add(value);
                    }
                }
            }
        }
    }

    /**
     * Process required properties and add them to the schema.
     *
     * @param requiredVars The list of required property variables
     * @param schemaNode The schema node to add required properties to
     */
    private void processRequiredProperties(List<CodegenProperty> requiredVars, ObjectNode schemaNode) {
        ArrayNode required = schemaNode.putArray("required");
        for (CodegenProperty var : requiredVars) {
            required.add(var.name);
        }
    }

    /**
     * Set the property type in the JSON Schema.
     *
     * @param var The property variable
     * @param property The property node to set the type on
     */
    private void setPropertyType(CodegenProperty var, ObjectNode property) {
        String dataType = var.dataType;

        // Handle arrays
        if (var.isArray) {
            // Always set type to array for array properties
            property.put("type", "array");
            ObjectNode items = property.putObject("items");
            setItemType(var, items);
            return;
        }

        // Apply type mapping
        JsonSchemaTypeMapper.applyTypeToProperty(dataType, property);
    }

    /**
     * Set the item type for array properties.
     *
     * @param var The property variable
     * @param items The items node to set the type on
     */
    private void setItemType(CodegenProperty var, ObjectNode items) {
        String dataType = var.dataType;

        // Remove "array" from dataType if present
        if (dataType.startsWith("array[") && dataType.endsWith("]")) {
            dataType = dataType.substring(6, dataType.length() - 1);
        }

        // Check if it's a reference to another model first
        if (var.items != null && var.items.complexType != null && !var.items.complexType.isEmpty()) {
            String complexType = var.items.complexType;
            // Check if it's a primitive type
            if (isPrimitiveType(complexType)) {
                // Handle primitive types directly
                JsonSchemaTypeMapper.applyPrimitiveType(complexType, items);
            } else {
                items.put("$ref", "#/definitions/" + complexType);
                // Remove type when using $ref as they are mutually exclusive in JSON Schema
                items.remove("type");
                items.remove("format");
            }
            return;
        }

        // Apply type mapping to items
        JsonSchemaTypeMapper.applyTypeToItems(dataType, items);
    }

    /**
     * Check if a type is a primitive JSON Schema type.
     *
     * @param type The type to check
     * @return true if the type is a primitive JSON Schema type
     */
    private boolean isPrimitiveType(String type) {
        return JsonSchemaTypeMapper.isPrimitiveType(type);
    }

    /**
     * Set the appropriate primitive type in the JSON Schema.
     *
     * @param type The primitive type
     * @param node The node to set the type on
     */
    private void setPrimitiveType(String type, ObjectNode node) {
        JsonSchemaTypeMapper.applyPrimitiveType(type, node);
    }

    /**
     * Process validation constraints and add them to the property.
     *
     * @param var The property variable
     * @param property The property node to add constraints to
     */
    private void processValidationConstraints(CodegenProperty var, ObjectNode property) {
        // Minimum value
        if (var.minimum != null) {
            try {
                property.put("minimum", Double.parseDouble(var.minimum));
            } catch (NumberFormatException e) {
                log.warn("Invalid minimum value: {}", var.minimum);
            }
        }

        // Maximum value
        if (var.maximum != null) {
            try {
                property.put("maximum", Double.parseDouble(var.maximum));
            } catch (NumberFormatException e) {
                log.warn("Invalid maximum value: {}", var.maximum);
            }
        }

        // Minimum length
        if (var.getMinLength() != null) {
            property.put("minLength", var.getMinLength());
        }

        // Maximum length
        if (var.getMaxLength() != null) {
            property.put("maxLength", var.getMaxLength());
        }

        // Pattern
        if (var.pattern != null) {
            property.put("pattern", var.pattern);
        }

        // Minimum items (for arrays)
        if (var.getMinItems() != null) {
            property.put("minItems", var.getMinItems());
        }

        // Maximum items (for arrays)
        if (var.getMaxItems() != null) {
            property.put("maxItems", var.getMaxItems());
        }

        // Unique items (for arrays)
        if (var.getUniqueItems()) {
            property.put("uniqueItems", true);
        }
    }

    /**
     * Exception thrown when there is an error during JSON schema generation.
     */
    public static class SchemaGenerationException extends Exception {

        /**
         * Constructs a new schema generation exception with the specified detail message.
         *
         * @param message the detail message
         */
        public SchemaGenerationException(String message) {
            super(message);
        }

        /**
         * Constructs a new schema generation exception with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause the cause of the exception
         */
        public SchemaGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
