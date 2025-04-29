package com.oashield.openapi.generators.modsecurity3;

import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.SupportingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

/**
 * Template manager for Modsecurity3Generator.
 * Handles all template-related functionality and file paths.
 */
public class TemplateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateManager.class);

    // Reference to the parent generator
    private final CodegenConfig generator;

    // Reference to the configuration manager
    private final ConfigurationManager configManager;

    // Template directory
    @Getter private final String templateDir = "modsecurity3";
    
    /**
     * Constructor for TemplateManager
     * 
     * @param configManager The configuration manager
     */
    public TemplateManager(ConfigurationManager configManager) {
        this.configManager = configManager;
        this.generator = configManager.getGenerator();
    }

    /**
     * Constructor for TemplateManager.
     * Created by Lombok @RequiredArgsConstructor which creates a constructor
     * with required parameters for final fields.
     */

    /**
     * Initialize the template configuration.
     */
    public void initialize() {
        // Configure templates
        configureTemplates();

        LOGGER.debug("TemplateManager initialized");
    }

    /**
     * Configure templates for code generation.
     */
    public void configureTemplates() {
        // Configure API template files
        generator.apiTemplateFiles().put(
            "config.mustache", // the template to use
            ".conf"); // the extension for each file to write

        // Note: The template directory is stored in this class
        // The main generator will need to retrieve it and set it directly

        // Configure supporting files
        generator.supportingFiles().add(new SupportingFile(
            "mainconfig.mustache", // the input template or file
            "", // the destination folder, relative `outputFolder`
            "mainconfig.conf") // the output file
        );
    }

    /**
     * Location to write model files. You can use the modelPackage() as defined when
     * the class is instantiated
     */
    public String modelFileFolder() {
        String folder = configManager.getOutputFolder();
        LOGGER.debug("Model file folder: {}", folder);
        return folder;
    }

    /**
     * Location to write api files. You can use the apiPackage() as defined when the
     * class is instantiated
     */
    public String apiFileFolder() {
        String folder = configManager.getOutputFolder();
        LOGGER.debug("API file folder: {}", folder);
        return folder;
    }
}
