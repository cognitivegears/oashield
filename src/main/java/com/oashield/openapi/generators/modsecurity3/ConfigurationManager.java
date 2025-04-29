package com.oashield.openapi.generators.modsecurity3;

import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration manager for Modsecurity3Generator.
 * Handles all configuration-related functionality.
 */
@RequiredArgsConstructor
@Slf4j
public class ConfigurationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

    // Configuration fields
    @Getter @Setter private String apiVersion = "0.0.2";
    @Getter @Setter private String outputFolder;
    @Getter @Setter private boolean generateJsonSchema = true;
    @Getter @Setter private String jsonSchemaOutputFile = "schema.json";
    @Getter @Setter private boolean validateBodySchema = true;

    // Reference to the parent generator
    @Getter private final CodegenConfig generator;

    /**
     * Constructor for ConfigurationManager.
     * Created by Lombok @RequiredArgsConstructor which creates a constructor
     * with required parameters for final fields.
     */

    /**
     * Initialize the configuration.
     */
    public void initialize() {
        // Configure output folder
        configureOutputFolder();

        // Configure additional properties
        configureAdditionalProperties();

        // Configure CLI options
        configureCliOptions();

        LOGGER.debug("ConfigurationManager initialized with output folder: {}", outputFolder);
    }

    /**
     * Configure the output folder for generated files.
     */
    public void configureOutputFolder() {
        // Only set default if not already set by CLI/config/test
        if (generator.getOutputDir() == null || generator.getOutputDir().isEmpty()) {
            generator.setOutputDir("generated-code/modsecurity3");
        }
        this.outputFolder = generator.getOutputDir();
    }

    /**
     * Configure additional properties for the generator.
     */
    public void configureAdditionalProperties() {
        // Add API version to additional properties
        generator.additionalProperties().put("apiVersion", apiVersion);

        // JSON Schema generation configuration
        generator.additionalProperties().put("generateJsonSchema", generateJsonSchema);
        generator.additionalProperties().put("jsonSchemaOutputFile", jsonSchemaOutputFile);
        generator.additionalProperties().put("validateBodySchema", Boolean.toString(validateBodySchema));
    }

    /**
     * Configure CLI options for the generator.
     */
    public void configureCliOptions() {
        // CLI options for JSON Schema generation
        generator.cliOptions().add(new CliOption("generateJsonSchema", "Generate JSON Schema from models")
            .defaultValue(Boolean.toString(generateJsonSchema)));
        generator.cliOptions().add(new CliOption("jsonSchemaOutputFile", "JSON Schema output file name")
            .defaultValue(jsonSchemaOutputFile));
        generator.cliOptions().add(new CliOption("validateBodySchema", "Generate rules for validating JSON body schema")
            .defaultValue(Boolean.toString(validateBodySchema)));
    }

    /**
     * Process the CLI options passed to the generator.
     */
    public void processOpts() {
        Map<String, Object> additionalProperties = generator.additionalProperties();

        // Synchronize outputFolder with generator's outputDir to respect CLI/test config
        this.outputFolder = generator.getOutputDir();

        // Process JSON Schema generation options
        if (additionalProperties.containsKey("generateJsonSchema")) {
            String generateJsonSchemaStr = additionalProperties.get("generateJsonSchema").toString();
            generateJsonSchema = Boolean.parseBoolean(generateJsonSchemaStr);
            LOGGER.info("generateJsonSchema set to: {}", generateJsonSchema);
        }

        // Process validateBodySchema option
        if (additionalProperties.containsKey("validateBodySchema")) {
            String validateBodySchemaStr = additionalProperties.get("validateBodySchema").toString();
            validateBodySchema = Boolean.parseBoolean(validateBodySchemaStr);
            LOGGER.info("validateBodySchema set to: {}", validateBodySchema);
        }

        if (additionalProperties.containsKey("jsonSchemaOutputFile")) {
            jsonSchemaOutputFile = additionalProperties.get("jsonSchemaOutputFile").toString();
            LOGGER.info("jsonSchemaOutputFile set to: {}", jsonSchemaOutputFile);
        }
    }
}
