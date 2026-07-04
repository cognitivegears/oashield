package com.oashield.openapi.generators.modsecurity3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for processing models in the Modsecurity3Generator.
 * This class encapsulates model processing logic extracted from Modsecurity3Generator.
 */
@RequiredArgsConstructor
public class ModelProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelProcessor.class);

    // Service components
    private final ConfigurationManager configManager;
    private final JsonSchemaGenerator jsonSchemaGenerator;

    /**
     * Constructor for ModelProcessor.
     * Created by Lombok @RequiredArgsConstructor which creates a constructor
     * with required parameters for final fields.
     */
    public ModelProcessor(ConfigurationManager configManager) {
        this(configManager, new JsonSchemaGenerator());
        LOGGER.debug("Initializing ModelProcessor");
    }

    /**
     * Process models and generate JSON Schema.
     *
     * @param objs The models to process
     * @return The processed models
     */
    public ModelsMap processModels(ModelsMap objs) {
        try {
            LOGGER.info("Generating JSON Schema from models...");
            String jsonSchema = jsonSchemaGenerator.generateJsonSchema(objs);

            // Save the JSON Schema to a file
            String outputFolder = configManager.getOutputFolder();
            String outputPath = outputFolder + File.separator + "schema.json";
            try {
                // Ensure output directory exists before writing
                Files.createDirectories(Paths.get(outputFolder));
                // Validate that outputPath is within outputFolder
                java.nio.file.Path outputDirPath = Paths.get(outputFolder).toAbsolutePath().normalize();
                java.nio.file.Path targetPath = Paths.get(outputPath).toAbsolutePath().normalize();
                if (!targetPath.startsWith(outputDirPath)) {
                    throw new RuntimeException("Target files must be generated within the output directory");
                }
                Files.write(targetPath, jsonSchema.getBytes());
                LOGGER.info("JSON Schema generated successfully: {}", outputPath);
            } catch (IOException e) {
                LOGGER.error("Error writing JSON Schema to file: {}", e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Error generating JSON Schema: {}", e.getMessage());
        }

        return objs;
    }

    /**
     * Process all models and generate JSON Schema.
     *
     * @param objs The map of all models to process
     * @return The processed map of all models
     */
    public Map<String, ModelsMap> processAllModels(Map<String, ModelsMap> objs) {
        // Examine ModelMap structure to understand its content
        if (LOGGER.isDebugEnabled()) {
            for (Map.Entry<String, ModelsMap> entry : objs.entrySet()) {
                String modelName = entry.getKey();
                ModelsMap modelsMap = entry.getValue();
                LOGGER.debug("Model: {}", modelName);

                for (ModelMap modelMap : modelsMap.getModels()) {
                    CodegenModel model = modelMap.getModel();
                    LOGGER.debug("  Model name: {}", model.name);
                    LOGGER.debug("  Model description: {}", model.description);
                    LOGGER.debug("  Model vars count: {}", model.vars.size());

                    // Log specific model properties to understand structure
                    if (model.vars != null && !model.vars.isEmpty()) {
                        LOGGER.debug("  Model has vars");
                        CodegenProperty firstVar = model.vars.get(0);
                        LOGGER.debug("  First var name: {}, dataType: {}", firstVar.name, firstVar.dataType);
                    }
                    if (model.requiredVars != null && !model.requiredVars.isEmpty()) {
                        LOGGER.debug("  Model has requiredVars");
                    }
                }
            }
        }

        // Process models for JSON Schema generation
        if (configManager.isGenerateJsonSchema()) {
            generateJsonSchema(objs);
        }

        return objs;
    }

    /**
     * Generate JSON Schema from models.
     *
     * @param models The models to convert to JSON Schema
     */
    private void generateJsonSchema(Map<String, ModelsMap> models) {
        LOGGER.info("Generating JSON Schema from models...");

        try {
            // Create a combined schema with all models
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootSchema = objectMapper.createObjectNode();
            rootSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
            rootSchema.put("title", "OpenAPI Schema Definitions");
            rootSchema.put("description", "JSON Schema definitions generated from OpenAPI specification");
            rootSchema.put("type", "object");
            ObjectNode definitions = rootSchema.putObject("definitions");

            // Process each model and add to the combined schema
            for (Map.Entry<String, ModelsMap> entry : models.entrySet()) {
                String modelName = entry.getKey();
                ModelsMap modelsMap = entry.getValue();

                // Skip models without any model maps
                if (modelsMap.getModels() == null || modelsMap.getModels().isEmpty()) {
                    continue;
                }

                // Get the first model from the models map
                ModelMap modelMap = modelsMap.getModels().get(0);
                CodegenModel model = modelMap.getModel();

                // Process the model and add it to the definitions
                ObjectNode modelSchema = jsonSchemaGenerator.generateModelSchema(model);
                if (modelSchema != null) {
                    definitions.set(modelName, modelSchema);
                }
            }

            // Convert the schema to a JSON string
            String jsonSchema = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootSchema);

            // Write JSON Schema to file
            String outputFolder = configManager.getOutputFolder();
            File outputDir = new File(outputFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String jsonSchemaOutputFile = configManager.getJsonSchemaOutputFile();
            File schemaFile = new File(outputDir, jsonSchemaOutputFile);
            // Validate that schemaFile is within outputDir
            java.nio.file.Path outputDirPath2 = outputDir.getCanonicalFile().toPath();
            java.nio.file.Path schemaFilePath = schemaFile.getCanonicalFile().toPath();
            if (!schemaFilePath.startsWith(outputDirPath2)) {
                throw new RuntimeException("Target files must be generated within the output directory");
            }
            try (FileWriter writer = new FileWriter(schemaFile)) {
                writer.write(jsonSchema);
            }

            LOGGER.info("JSON Schema generated successfully: {}", schemaFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Error generating JSON Schema", e);
        }
    }

    /**
     * Helper method to flatten a model property into a list of properties.
     *
     * @param currentProperty The property to flatten
     * @param baseNamePrefix The prefix to add to the property name
     * @return A list of flattened properties
     */
    public List<CodegenProperty> flattenModel(CodegenProperty currentProperty, String baseNamePrefix) {
        List<CodegenProperty> properties = new ArrayList<CodegenProperty>();

        // handle array of primitives as single property
        if (currentProperty.isArray && currentProperty.vars != null && !currentProperty.vars.isEmpty() && currentProperty.vars.get(0).isPrimitiveType) {
            currentProperty.baseName = baseNamePrefix + currentProperty.baseName;
            properties.add(currentProperty);
            return properties;
        }
        // 1. The property is a model
        if(currentProperty.isModel) {
            // Recursively flatten the model
            LOGGER.debug("Flattening model property: {}", currentProperty.baseName);
            baseNamePrefix += currentProperty.baseName + ".";
            for(CodegenProperty prop : currentProperty.vars) {
                List<CodegenProperty> flattenedProperties = flattenModel(prop, baseNamePrefix);
                properties.addAll(flattenedProperties);
            }
        }

        // 2. The property is an array of models
        else if(currentProperty.isArray) {
            LOGGER.debug("Flattening array of model property: {}", currentProperty.baseName);
            int i = 0;
            for(CodegenProperty prop : currentProperty.vars) {
                List<CodegenProperty> flattenedProperties = flattenModel(prop, baseNamePrefix + currentProperty.baseName + "." + i + ".");
                i++;
                properties.addAll(flattenedProperties);
            }
        }

        // 3. The property is a primitive type
        else {
            LOGGER.debug("Adding property: {}", currentProperty.baseName);
            // Add the baseNamePrefix to the property
            currentProperty.baseName = baseNamePrefix + currentProperty.baseName;
            properties.add(currentProperty);
        }

        return properties;
    }
}
