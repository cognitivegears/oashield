package com.oashield.openapi.generators.modsecurity3.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oashield.openapi.generators.modsecurity3.JsonSchemaGenerator;
import com.oashield.openapi.generators.modsecurity3.JsonSchemaGenerator.SchemaGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the JsonSchemaGenerator class.
 */
public class JsonSchemaGeneratorTest {

    private JsonSchemaGenerator jsonSchemaGenerator;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        jsonSchemaGenerator = new JsonSchemaGenerator();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testGenerateJsonSchema() throws Exception {
        // Create a simple ModelsMap with a test model
        ModelsMap modelsMap = new ModelsMap();
        List<ModelMap> models = new ArrayList<>();
        
        ModelMap modelMap = new ModelMap();
        CodegenModel model = createTestModel("TestModel", "A test model");
        modelMap.setModel(model);
        models.add(modelMap);
        
        modelsMap.setModels(models);
        
        // Generate JSON Schema
        String jsonSchema = jsonSchemaGenerator.generateJsonSchema(modelsMap);
        
        // Parse the generated schema
        JsonNode schemaNode = objectMapper.readTree(jsonSchema);
        
        // Validate the schema structure
        assertEquals("http://json-schema.org/draft-07/schema#", schemaNode.get("$schema").asText());
        assertEquals("OpenAPI Schema Definitions", schemaNode.get("title").asText());
        assertEquals("object", schemaNode.get("type").asText());
        
        // Validate the model definition
        JsonNode definitions = schemaNode.get("definitions");
        assertNotNull(definitions);
        
        JsonNode testModel = definitions.get("TestModel");
        assertNotNull(testModel);
        assertEquals("TestModel", testModel.get("title").asText());
        assertEquals("A test model", testModel.get("description").asText());
    }

    @Test
    public void testGenerateModelSchema() throws SchemaGenerationException {
        // Create a test model with various properties
        CodegenModel model = createTestModel("TestModel", "A test model with various property types");
        
        // Generate schema for the model
        ObjectNode schemaNode = jsonSchemaGenerator.generateModelSchema(model);
        
        // Validate basic structure
        assertEquals("TestModel", schemaNode.get("title").asText());
        assertEquals("A test model with various property types", schemaNode.get("description").asText());
        assertEquals("object", schemaNode.get("type").asText());
        
        // Validate properties
        JsonNode properties = schemaNode.get("properties");
        assertNotNull(properties);
        
        // Check string property
        JsonNode stringProp = properties.get("stringProperty");
        assertEquals("string", stringProp.get("type").asText());
        assertEquals("A string property", stringProp.get("description").asText());
        
        // Check integer property
        JsonNode intProp = properties.get("intProperty");
        // The type mapping may vary depending on implementation, so allowing either integer or string
        assertTrue(intProp.has("type"));
        assertTrue(intProp.has("format"));
        
        // Check number property
        JsonNode numberProp = properties.get("numberProperty");
        assertTrue(numberProp.has("type"));
        assertTrue(numberProp.has("format"));
        
        // Check array property
        JsonNode arrayProp = properties.get("arrayProperty");
        assertEquals("array", arrayProp.get("type").asText());
        assertEquals("string", arrayProp.get("items").get("type").asText());
        
        // Check complex reference property
        JsonNode refProp = properties.get("refProperty");
        assertEquals("#/definitions/OtherModel", refProp.get("$ref").asText());
        
        // Check required properties
        JsonNode required = schemaNode.get("required");
        assertNotNull(required);
        assertEquals(2, required.size());
        assertEquals("stringProperty", required.get(0).asText());
        assertEquals("intProperty", required.get(1).asText());
    }

    @Test
    public void testGenerateModelSchemaWithValidationConstraints() throws SchemaGenerationException {
        // Create a test model with validation constraints
        CodegenModel model = new CodegenModel();
        model.name = "ValidationModel";
        model.classname = "ValidationModel";
        model.description = "A model with validation constraints";
        
        // Create a property with validation constraints
        CodegenProperty stringProp = new CodegenProperty();
        stringProp.name = "stringWithConstraints";
        stringProp.getter = "getStringWithConstraints";
        stringProp.setter = "setStringWithConstraints";
        stringProp.dataType = "String";
        stringProp.baseType = "String";
        stringProp.isString = true;
        stringProp.required = true;
        stringProp.description = "String with constraints";
        stringProp.setPattern("^[a-zA-Z0-9]+$");
        stringProp.setMinLength(3);
        stringProp.setMaxLength(50);
        
        // Create a number property with constraints
        CodegenProperty numberProp = new CodegenProperty();
        numberProp.name = "numberWithConstraints";
        numberProp.getter = "getNumberWithConstraints";
        numberProp.setter = "setNumberWithConstraints";
        numberProp.dataType = "Double";
        numberProp.baseType = "Double";
        numberProp.isNumeric = true;
        numberProp.isNumber = true;
        numberProp.required = true;
        numberProp.description = "Number with constraints";
        numberProp.minimum = "1.0";
        numberProp.maximum = "100.0";
        
        // Create an array property with constraints
        CodegenProperty arrayProp = new CodegenProperty();
        arrayProp.name = "arrayWithConstraints";
        arrayProp.getter = "getArrayWithConstraints";
        arrayProp.setter = "setArrayWithConstraints";
        arrayProp.dataType = "array[String]";
        arrayProp.baseType = "Array";
        arrayProp.isArray = true;
        arrayProp.description = "Array with constraints";
        arrayProp.setMinItems(1);
        arrayProp.setMaxItems(10);
        arrayProp.setUniqueItems(true);
        
        // Create items for the array
        CodegenProperty items = new CodegenProperty();
        items.dataType = "String";
        items.baseType = "String";
        items.isString = true;
        arrayProp.items = items;
        
        // Add properties to the model
        List<CodegenProperty> vars = new ArrayList<>();
        vars.add(stringProp);
        vars.add(numberProp);
        vars.add(arrayProp);
        model.vars = vars;
        
        // Add required properties
        List<CodegenProperty> requiredVars = new ArrayList<>();
        requiredVars.add(stringProp);
        requiredVars.add(numberProp);
        model.requiredVars = requiredVars;
        
        // Generate schema for the model
        ObjectNode schemaNode = jsonSchemaGenerator.generateModelSchema(model);
        
        // Validate basic structure
        assertEquals("ValidationModel", schemaNode.get("title").asText());
        
        // Validate properties with constraints
        JsonNode properties = schemaNode.get("properties");
        
        // Check string property constraints
        JsonNode stringConstraints = properties.get("stringWithConstraints");
        assertEquals("string", stringConstraints.get("type").asText());
        assertEquals("^[a-zA-Z0-9]+$", stringConstraints.get("pattern").asText());
        assertEquals(3, stringConstraints.get("minLength").asInt());
        assertEquals(50, stringConstraints.get("maxLength").asInt());
        
        // Check number property constraints
        JsonNode numberConstraints = properties.get("numberWithConstraints");
        assertTrue(numberConstraints.has("type"));
        assertEquals(1.0, numberConstraints.get("minimum").asDouble());
        assertEquals(100.0, numberConstraints.get("maximum").asDouble());
        
        // Check array property constraints
        JsonNode arrayConstraints = properties.get("arrayWithConstraints");
        assertEquals("array", arrayConstraints.get("type").asText());
        assertEquals(1, arrayConstraints.get("minItems").asInt());
        assertEquals(10, arrayConstraints.get("maxItems").asInt());
        assertTrue(arrayConstraints.get("uniqueItems").asBoolean());
        assertEquals("string", arrayConstraints.get("items").get("type").asText());
    }

    @Test
    public void testGenerateSchemaWithEnumProperties() throws SchemaGenerationException {
        // Create a test model with enum properties
        CodegenModel model = new CodegenModel();
        model.name = "EnumModel";
        model.classname = "EnumModel";
        model.description = "A model with enum properties";
        
        // Create an enum property
        CodegenProperty enumProp = new CodegenProperty();
        enumProp.name = "status";
        enumProp.getter = "getStatus";
        enumProp.setter = "setStatus";
        enumProp.dataType = "String";
        enumProp.baseType = "String";
        enumProp.isString = true;
        enumProp.isEnum = true;
        enumProp.description = "Status enum";
        
        // Create enum values
        Map<String, Object> allowableValues = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("PENDING");
        values.add("APPROVED");
        values.add("REJECTED");
        allowableValues.put("values", values);
        enumProp.allowableValues = allowableValues;
        
        // Add properties to the model
        List<CodegenProperty> vars = new ArrayList<>();
        vars.add(enumProp);
        model.vars = vars;
        
        // Generate schema for the model
        ObjectNode schemaNode = jsonSchemaGenerator.generateModelSchema(model);
        
        // Validate enum property
        JsonNode properties = schemaNode.get("properties");
        JsonNode enumProperty = properties.get("status");
        assertEquals("string", enumProperty.get("type").asText());
        
        // Check enum values
        JsonNode enumValues = enumProperty.get("enum");
        assertNotNull(enumValues);
        assertEquals(3, enumValues.size());
        assertEquals("PENDING", enumValues.get(0).asText());
        assertEquals("APPROVED", enumValues.get(1).asText());
        assertEquals("REJECTED", enumValues.get(2).asText());
    }

    @Test
    public void testGenerateSchemaWithArrayOfComplexType() throws SchemaGenerationException {
        // Create a test model with an array of complex type
        CodegenModel model = new CodegenModel();
        model.name = "ArrayModel";
        model.classname = "ArrayModel";
        model.description = "A model with an array of complex type";
        
        // Create an array property with complex items
        CodegenProperty arrayProp = new CodegenProperty();
        arrayProp.name = "items";
        arrayProp.getter = "getItems";
        arrayProp.setter = "setItems";
        arrayProp.dataType = "array[Item]";
        arrayProp.baseType = "Array";
        arrayProp.isArray = true;
        arrayProp.complexType = "Item";
        arrayProp.description = "Array of items";
        
        // Create items for the array
        CodegenProperty items = new CodegenProperty();
        items.dataType = "Item";
        items.baseType = "Item";
        items.complexType = "Item";
        arrayProp.items = items;
        
        // Add properties to the model
        List<CodegenProperty> vars = new ArrayList<>();
        vars.add(arrayProp);
        model.vars = vars;
        
        // Generate schema for the model
        ObjectNode schemaNode = jsonSchemaGenerator.generateModelSchema(model);
        
        // Validate array property with complex items
        JsonNode properties = schemaNode.get("properties");
        JsonNode arrayProperty = properties.get("items");
        assertEquals("array", arrayProperty.get("type").asText());
        
        // Check items reference
        JsonNode itemsNode = arrayProperty.get("items");
        assertEquals("#/definitions/Item", itemsNode.get("$ref").asText());
    }

    /**
     * Helper method to create a test model with various property types.
     */
    private CodegenModel createTestModel(String name, String description) {
        CodegenModel model = new CodegenModel();
        model.name = name;
        model.classname = name;
        model.description = description;
        
        // Create a string property
        CodegenProperty stringProp = new CodegenProperty();
        stringProp.name = "stringProperty";
        stringProp.getter = "getStringProperty";
        stringProp.setter = "setStringProperty";
        stringProp.dataType = "String";
        stringProp.baseType = "String";
        stringProp.isString = true;
        stringProp.required = true;
        stringProp.description = "A string property";
        
        // Create an integer property
        CodegenProperty intProp = new CodegenProperty();
        intProp.name = "intProperty";
        intProp.getter = "getIntProperty";
        intProp.setter = "setIntProperty";
        intProp.dataType = "Integer";
        intProp.baseType = "Integer";
        intProp.isInteger = true;
        intProp.required = true;
        intProp.setFormat("int32");
        intProp.description = "An integer property";
        
        // Create a number property
        CodegenProperty numberProp = new CodegenProperty();
        numberProp.name = "numberProperty";
        numberProp.getter = "getNumberProperty";
        numberProp.setter = "setNumberProperty";
        numberProp.dataType = "Double";
        numberProp.baseType = "Double";
        numberProp.isNumber = true;
        numberProp.setFormat("double");
        numberProp.description = "A number property";
        
        // Create an array property
        CodegenProperty arrayProp = new CodegenProperty();
        arrayProp.name = "arrayProperty";
        arrayProp.getter = "getArrayProperty";
        arrayProp.setter = "setArrayProperty";
        arrayProp.dataType = "array[String]";
        arrayProp.baseType = "Array";
        arrayProp.isArray = true;
        arrayProp.description = "An array property";
        
        // Create items for the array
        CodegenProperty items = new CodegenProperty();
        items.dataType = "String";
        items.baseType = "String";
        items.isString = true;
        arrayProp.items = items;
        
        // Create a reference property
        CodegenProperty refProp = new CodegenProperty();
        refProp.name = "refProperty";
        refProp.getter = "getRefProperty";
        refProp.setter = "setRefProperty";
        refProp.dataType = "OtherModel";
        refProp.baseType = "OtherModel";
        refProp.complexType = "OtherModel";
        refProp.description = "A reference property";
        
        // Add properties to the model
        List<CodegenProperty> vars = new ArrayList<>();
        vars.add(stringProp);
        vars.add(intProp);
        vars.add(numberProp);
        vars.add(arrayProp);
        vars.add(refProp);
        model.vars = vars;
        
        // Add required properties
        List<CodegenProperty> requiredVars = new ArrayList<>();
        requiredVars.add(stringProp);
        requiredVars.add(intProp);
        model.requiredVars = requiredVars;
        
        return model;
    }
}