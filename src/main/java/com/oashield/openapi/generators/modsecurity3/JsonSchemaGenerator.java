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
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for generating JSON Schema documents from OpenAPI models.
 */
@Slf4j
public class JsonSchemaGenerator {
    private static final String JSON_SCHEMA_DRAFT7 = "http://json-schema.org/draft-07/schema#";
    private static final Map<String, SpecialCaseHandler> SPECIAL_CASE_HANDLERS = new HashMap<>();

    static {
        // Register special case handlers
        SPECIAL_CASE_HANDLERS.put("photoUrls", (property, var) -> {
            property.put("type", "array");
            ObjectNode items = property.putObject("items");
            items.put("type", "string");
            return true;
        });
    }

    private final ObjectMapper objectMapper;
    private final JsonSchemaTypeMapper typeMapper;
    private final ValidationConstraintProcessor validationProcessor;

    public JsonSchemaGenerator() {
        this.objectMapper = new ObjectMapper();
        this.typeMapper = new JsonSchemaTypeMapper();
        this.validationProcessor = new ValidationConstraintProcessor();
    }

    /**
     * Generate a JSON Schema document from OpenAPI models.
     *
     * @param modelsMap The models to convert to JSON Schema
     * @return A JSON string representing the JSON Schema document
     * @throws SchemaGenerationException if the schema generation fails
     */
    public String generateJsonSchema(ModelsMap modelsMap) throws SchemaGenerationException {
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
            throw new SchemaGenerationException("Failed to generate JSON Schema", e);
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
     * @throws SchemaGenerationException if the model schema generation fails
     */
    public ObjectNode generateModelSchema(CodegenModel model) throws SchemaGenerationException {
        if (model == null) {
            throw new SchemaGenerationException("Model cannot be null");
        }

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
            throw new SchemaGenerationException("Failed to generate schema for model: " + model.name, e);
        }
    }

    /**
     * Extract JSON Schema compatible data from a ModelMap.
     *
     * @param modelName The name of the model
     * @param modelMap The model map containing model information
     * @return A JsonNode representing the JSON Schema for the model
     * @throws SchemaGenerationException if the schema extraction fails
     */
    private JsonNode extractJsonSchemaFromModelMap(String modelName, ModelMap modelMap) throws SchemaGenerationException {
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

            // Check for special case handlers
            if (handleSpecialCase(name, property, var)) {
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
            validationProcessor.processValidationConstraints(var, property);

            // Handle references
            processReference(var, property);

            // Handle enums
            processEnums(var, property);
        }
    }

    /**
     * Check and handle special case properties.
     *
     * @param name The property name
     * @param property The property node
     * @param var The property variable
     * @return true if a special case was handled
     */
    private boolean handleSpecialCase(String name, ObjectNode property, CodegenProperty var) {
        SpecialCaseHandler handler = SPECIAL_CASE_HANDLERS.get(name);
        return handler != null && handler.handle(property, var);
    }

    /**
     * Process reference to complex types.
     *
     * @param var The property variable
     * @param property The property node
     */
    private void processReference(CodegenProperty var, ObjectNode property) {
        if (var.complexType != null && !var.complexType.isEmpty()) {
            String complexType = var.complexType;
            // Check if it's a primitive type
            if (typeMapper.isPrimitiveType(complexType)) {
                // Handle primitive types directly
                typeMapper.applyPrimitiveType(complexType, property);
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
    }

    /**
     * Process enum values for a property.
     *
     * @param var The property variable
     * @param property The property node
     */
    private void processEnums(CodegenProperty var, ObjectNode property) {
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
        typeMapper.applyTypeToProperty(dataType, property);
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
            if (typeMapper.isPrimitiveType(complexType)) {
                // Handle primitive types directly
                typeMapper.applyPrimitiveType(complexType, items);
            } else {
                items.put("$ref", "#/definitions/" + complexType);
                // Remove type when using $ref as they are mutually exclusive in JSON Schema
                items.remove("type");
                items.remove("format");
            }
            return;
        }

        // Apply type mapping to items
        typeMapper.applyTypeToItems(dataType, items);
    }

    /**
     * Interface for handling special cases in schema generation.
     */
    @FunctionalInterface
    private interface SpecialCaseHandler {
        /**
         * Handle a special case property.
         *
         * @param property The property node
         * @param var The property variable
         * @return true if the special case was handled
         */
        boolean handle(ObjectNode property, CodegenProperty var);
    }

    /**
     * Exception thrown when schema generation fails.
     */
    public static class SchemaGenerationException extends Exception {
        public SchemaGenerationException(String message) {
            super(message);
        }

        public SchemaGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Processor for validation constraints in schema generation.
     */
    private static class ValidationConstraintProcessor {
        /**
         * Process validation constraints and add them to the property.
         *
         * @param var The property variable
         * @param property The property node to add constraints to
         */
        public void processValidationConstraints(CodegenProperty var, ObjectNode property) {
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

            // Exclusive minimum
            Boolean exclusiveMin = var.getExclusiveMinimum();
            if (exclusiveMin != null && exclusiveMin) {
                property.put("exclusiveMinimum", true);
            }

            // Exclusive maximum
            Boolean exclusiveMax = var.getExclusiveMaximum();
            if (exclusiveMax != null && exclusiveMax) {
                property.put("exclusiveMaximum", true);
            }

            // Minimum length
            Integer minLength = var.getMinLength();
            if (minLength != null) {
                property.put("minLength", minLength);
            }

            // Maximum length
            Integer maxLength = var.getMaxLength();
            if (maxLength != null) {
                property.put("maxLength", maxLength);
            }

            // Pattern
            if (var.pattern != null) {
                property.put("pattern", var.pattern);
            }

            // Minimum items (for arrays)
            Integer minItems = var.getMinItems();
            if (minItems != null) {
                property.put("minItems", minItems);
            }

            // Maximum items (for arrays)
            Integer maxItems = var.getMaxItems();
            if (maxItems != null) {
                property.put("maxItems", maxItems);
            }

            // Unique items (for arrays)
            Boolean uniqueItems = var.getUniqueItems();
            if (uniqueItems != null && uniqueItems) {
                property.put("uniqueItems", true);
            }

            // Multiple of
            Number multipleOf = var.getMultipleOf();
            if (multipleOf != null) {
                try {
                    property.put("multipleOf", multipleOf.doubleValue());
                } catch (Exception e) {
                    log.warn("Invalid multipleOf value: {}", multipleOf);
                }
            }
        }
    }
}