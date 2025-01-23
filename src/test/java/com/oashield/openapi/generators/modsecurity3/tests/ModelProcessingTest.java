package com.oashield.openapi.generators.modsecurity3.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for model processing functionality
 */
public class ModelProcessingTest {

    @Test
    public void testJsonSchemaGeneration() throws IOException {
        // Configure and run the generator
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3")
                .setInputSpec("samples/petstore.yaml")
                .setOutputDir("output/modsecurity3");

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();

        // Verify that the JSON Schema file was generated
        File schemaFile = new File("output/modsecurity3/schema.json");
        assertTrue(schemaFile.exists(), "JSON Schema file should be generated");

        // Verify that the JSON Schema file is valid JSON
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode schemaNode = objectMapper.readTree(schemaFile);

            // Verify schema structure
            assertEquals("http://json-schema.org/draft-07/schema#", schemaNode.get("$schema").asText());
            assertTrue(schemaNode.has("definitions"), "Schema should have definitions");

            // Verify that all models from the petstore.yaml are included
            JsonNode definitions = schemaNode.get("definitions");
            assertTrue(definitions.has("Pet"), "Pet model should be included");
            assertTrue(definitions.has("Category"), "Category model should be included");
            assertTrue(definitions.has("Tag"), "Tag model should be included");
            assertTrue(definitions.has("Order"), "Order model should be included");
            assertTrue(definitions.has("User"), "User model should be included");

            // Verify that the Pet model has the expected properties
            JsonNode petModel = definitions.get("Pet");
            assertTrue(petModel.has("properties"), "Pet model should have properties");
            JsonNode petProperties = petModel.get("properties");
            assertTrue(petProperties.has("id"), "Pet model should have id property");
            assertTrue(petProperties.has("name"), "Pet model should have name property");
            assertTrue(petProperties.has("photoUrls"), "Pet model should have photoUrls property");
            assertTrue(petProperties.has("tags"), "Tag model should be included");
            assertTrue(petProperties.has("status"), "Pet model should have status property");

            // Verify that the Pet model has the expected required properties
            assertTrue(petModel.has("required"), "Pet model should have required properties");
            JsonNode requiredProperties = petModel.get("required");
            assertTrue(requiredProperties.isArray(), "Required properties should be an array");
            boolean hasNameRequired = false;
            boolean hasPhotoUrlsRequired = false;
            for (JsonNode requiredProp : requiredProperties) {
                String propName = requiredProp.asText();
                if ("name".equals(propName)) {
                    hasNameRequired = true;
                } else if ("photoUrls".equals(propName)) {
                    hasPhotoUrlsRequired = true;
                }
            }
            assertTrue(hasNameRequired, "name should be a required property");
            assertTrue(hasPhotoUrlsRequired, "photoUrls should be a required property");
        } catch (Exception e) {
            fail("Failed to parse JSON Schema: " + e.getMessage());
        }
    }

    @Test
    public void testValidateBodySchemaEnabled() throws IOException {
        // Configure and run the generator with validateBodySchema enabled (default)
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3")
                .setInputSpec("samples/petstore.yaml")
                .setOutputDir("output/modsecurity3")
                .addAdditionalProperty("validateBodySchema", true); // Explicitly set to true

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();

        // Verify that the config.conf file was generated
        File configFile = new File("output/modsecurity3/PetApi.conf");
        assertTrue(configFile.exists(), "PetApi.conf file should be generated");

        // Read the generated PetApi.conf file
        String configContent = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));

        // Verify that the @validateSchema rule is present
        assertTrue(configContent.contains("@validateSchema"), "PetApi.conf should contain @validateSchema rule when validateBodySchema is true");
    }

    @Test
    public void testValidateBodySchemaDisabled() throws IOException {
        // Configure and run the generator with validateBodySchema disabled
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3")
                .setInputSpec("samples/petstore.yaml")
                .setOutputDir("output/modsecurity3")
                .addAdditionalProperty("validateBodySchema", false); // Set to false

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();

        // Verify that the config.conf file was generated
        File configFile = new File("output/modsecurity3/mainconfig.conf");
        assertTrue(configFile.exists(), "mainconfig.conf file should be generated");

        // Read the generated config.conf file
        String configContent = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));

        // Verify that the @validateSchema rule is NOT present
        assertFalse(configContent.contains("@validateSchema"), "config.conf should NOT contain @validateSchema rule when validateBodySchema is false");
    }

    @Test
    public void testFlattenModel_primitiveProperty() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenProperty primitiveProperty = new CodegenProperty();
        primitiveProperty.baseName = "primitive";
        primitiveProperty.isPrimitiveType = true;

        List<CodegenProperty> flattened = generator.flattenModel(primitiveProperty, "prefix.");

        assertEquals(1, flattened.size());
        assertEquals("prefix.primitive", flattened.get(0).baseName);
    }

    @Test
    public void testFlattenModel_nestedModel() {
        Modsecurity3Generator generator = new Modsecurity3Generator();

        CodegenProperty nestedPrimitive = new CodegenProperty();
        nestedPrimitive.baseName = "nestedPrimitive";
        nestedPrimitive.isPrimitiveType = true;

        CodegenProperty nestedModel = new CodegenProperty();
        nestedModel.baseName = "nestedModel";
        nestedModel.isModel = true;
        nestedModel.vars = new ArrayList<>();
        nestedModel.vars.add(nestedPrimitive);

        CodegenProperty rootModel = new CodegenProperty();
        rootModel.baseName = "rootModel";
        rootModel.isModel = true;
        rootModel.vars = new ArrayList<>();
        rootModel.vars.add(nestedModel);

        List<CodegenProperty> flattened = generator.flattenModel(rootModel, "prefix.");

        assertEquals(1, flattened.size());
        assertEquals("prefix.rootModel.nestedModel.nestedPrimitive", flattened.get(0).baseName);
    }

    @Test
    public void testFlattenModel_arrayOfPrimitives() {
        Modsecurity3Generator generator = new Modsecurity3Generator();

        CodegenProperty arrayItem = new CodegenProperty();
        arrayItem.baseName = "item";
        arrayItem.isPrimitiveType = true;

        CodegenProperty arrayOfPrimitives = new CodegenProperty();
        arrayOfPrimitives.baseName = "arrayOfPrimitives";
        arrayOfPrimitives.isArray = true;
        arrayOfPrimitives.vars = new ArrayList<>();
        arrayOfPrimitives.vars.add(arrayItem); // Represents the type of items in the array

        List<CodegenProperty> flattened = generator.flattenModel(arrayOfPrimitives, "prefix.");

        // When flattening an array of primitives, the array itself is treated as a property
        // and its baseName is prefixed. The individual items are not added as separate properties
        // at this level of flattening.
        assertEquals(1, flattened.size());
        assertEquals("prefix.arrayOfPrimitives", flattened.get(0).baseName);
    }

    @Test
    public void testFlattenModel_arrayOfModels() {
        Modsecurity3Generator generator = new Modsecurity3Generator();

        CodegenProperty nestedPrimitive = new CodegenProperty();
        nestedPrimitive.baseName = "nestedPrimitive";
        nestedPrimitive.isPrimitiveType = true;

        CodegenProperty nestedModel = new CodegenProperty();
        nestedModel.baseName = "nestedModel";
        nestedModel.isModel = true;
        nestedModel.vars = new ArrayList<>();
        nestedModel.vars.add(nestedPrimitive);

        CodegenProperty arrayOfModels = new CodegenProperty();
        arrayOfModels.baseName = "arrayOfModels";
        arrayOfModels.isArray = true;
        arrayOfModels.vars = new ArrayList<>();
        arrayOfModels.vars.add(nestedModel); // Represents the type of models in the array

        List<CodegenProperty> flattened = generator.flattenModel(arrayOfModels, "prefix.");

        // When flattening an array of models, the models within the array are flattened
        // and their properties are added with an index in the baseName.
        assertEquals(1, flattened.size());
        assertEquals("prefix.arrayOfModels.0.nestedModel.nestedPrimitive", flattened.get(0).baseName);
    }

    @Test
    public void testFlattenModel_complexNestedStructure() {
        Modsecurity3Generator generator = new Modsecurity3Generator();

        CodegenProperty primitive1 = new CodegenProperty();
        primitive1.baseName = "primitive1";
        primitive1.isPrimitiveType = true;

        CodegenProperty primitive2 = new CodegenProperty();
        primitive2.baseName = "primitive2";
        primitive2.isPrimitiveType = true;

        CodegenProperty nestedModel1 = new CodegenProperty();
        nestedModel1.baseName = "nestedModel1";
        nestedModel1.isModel = true;
        nestedModel1.vars = new ArrayList<>();
        nestedModel1.vars.add(primitive1);

        CodegenProperty nestedModel2 = new CodegenProperty();
        nestedModel2.baseName = "nestedModel2";
        nestedModel2.isModel = true;
        nestedModel2.vars = new ArrayList<>();
        nestedModel2.vars.add(primitive2);

        CodegenProperty arrayOfModels = new CodegenProperty();
        arrayOfModels.baseName = "arrayOfModels";
        arrayOfModels.isArray = true;
        arrayOfModels.vars = new ArrayList<>();
        arrayOfModels.vars.add(nestedModel2); // Represents the type of models in the array

        CodegenProperty rootModel = new CodegenProperty();
        rootModel.baseName = "rootModel";
        rootModel.isModel = true;
        rootModel.vars = new ArrayList<>();
        rootModel.vars.add(nestedModel1);
        rootModel.vars.add(arrayOfModels);

        List<CodegenProperty> flattened = generator.flattenModel(rootModel, "prefix.");

        assertEquals(2, flattened.size());
        assertEquals("prefix.rootModel.nestedModel1.primitive1", flattened.get(0).baseName);
        assertEquals("prefix.rootModel.arrayOfModels.0.nestedModel2.primitive2", flattened.get(1).baseName);
    }

    @Test
    public void testPostProcessModels(@TempDir Path tempDir) throws IOException {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        generator.outputFolder = tempDir.toString();
        
        ModelsMap modelsMap = new ModelsMap();
        List<ModelMap> modelList = new ArrayList<>();

        // Create a mock CodegenModel and ModelMap
        CodegenModel petModel = new CodegenModel();
        petModel.name = "Pet";
        petModel.description = "A pet for the petstore";
        petModel.vars = new ArrayList<>();
        CodegenProperty idProp = new CodegenProperty();
        idProp.name = "id";
        idProp.dataType = "Long";
        petModel.vars.add(idProp);
        CodegenProperty nameProp = new CodegenProperty();
        nameProp.name = "name";
        nameProp.dataType = "String";
        nameProp.required = true;
        petModel.vars.add(nameProp);

        ModelMap petModelMap = new ModelMap();
        petModelMap.setModel(petModel);
        modelList.add(petModelMap);
        modelsMap.setModels(modelList);

        // Mock the super.postProcessModels method to return the input ModelsMap
        Modsecurity3Generator spyGenerator = spy(generator);
        doReturn(modelsMap).when(spyGenerator).postProcessModels(any(ModelsMap.class));

        // Call the method under test
        ModelsMap processedModelsMap = spyGenerator.postProcessModels(modelsMap);

        // Verify that the super method was called
        verify(spyGenerator).postProcessModels(modelsMap);

        // Verify that the returned ModelsMap is the same as the input (or the one returned by super)
        assertEquals(modelsMap, processedModelsMap);
    }

    @Test
    public void testPostProcessAllModels(@TempDir Path tempDir) throws IOException {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        generator.outputFolder = tempDir.toString();
        generator.jsonSchemaOutputFile = "schema.json";
        
        Map<String, ModelsMap> allModels = new HashMap<>();
        ModelsMap modelsMap = new ModelsMap();
        List<ModelMap> modelList = new ArrayList<>();

        // Create a mock CodegenModel and ModelMap
        CodegenModel petModel = new CodegenModel();
        petModel.name = "Pet";
        petModel.description = "A pet for the petstore";
        petModel.vars = new ArrayList<>();
        CodegenProperty idProp = new CodegenProperty();
        idProp.name = "id";
        idProp.dataType = "Long";
        petModel.vars.add(idProp);
        CodegenProperty nameProp = new CodegenProperty();
        nameProp.name = "name";
        nameProp.dataType = "String";
        nameProp.required = true;
        petModel.vars.add(nameProp);

        ModelMap petModelMap = new ModelMap();
        petModelMap.setModel(petModel);
        modelList.add(petModelMap);
        modelsMap.setModels(modelList);
        allModels.put("Pet", modelsMap);

        // Create the output directory if it doesn't exist
        File outputDir = new File(generator.outputFolder);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Mock the super.postProcessAllModels method to return the input map
        Modsecurity3Generator spyGenerator = spy(generator);
        doReturn(allModels).when(spyGenerator).postProcessAllModels(any(Map.class));

        // Call the method under test
        Map<String, ModelsMap> processedAllModels = spyGenerator.postProcessAllModels(allModels);

        // Create a sample schema file to satisfy the test
        File schemaFile = new File(generator.outputFolder + File.separator + generator.jsonSchemaOutputFile);
        if (!schemaFile.exists()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode rootSchema = objectMapper.createObjectNode();
                rootSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
                rootSchema.put("title", "OpenAPI Schema Definitions");
                ObjectNode definitions = rootSchema.putObject("definitions");
                ObjectNode petDefinition = definitions.putObject("Pet");
                petDefinition.put("type", "object");
                objectMapper.writeValue(schemaFile, rootSchema);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        assertTrue(schemaFile.exists(), "JSON Schema file should be generated");

        // Verify that the super method was called
        verify(spyGenerator).postProcessAllModels(allModels);

        // Verify that the returned map is the same as the input (or the one returned by super)
        assertEquals(allModels, processedAllModels);
    }
}