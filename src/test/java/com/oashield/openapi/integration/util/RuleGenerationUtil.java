package com.oashield.openapi.integration.util;

import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Utility class for generating ModSecurity rules from OpenAPI specifications
 * for integration testing purposes.
 */
public class RuleGenerationUtil {

    /**
     * Generates ModSecurity3 rules from an OpenAPI spec file
     *
     * @param openApiSpecPath The path to the OpenAPI specification file
     * @param outputDir       The directory where the rules should be generated
     * @param useJsonSchema   Whether to generate and use JSON Schema validation
     * @return The path to the generated rules directory
     * @throws RuntimeException if rule generation fails
     */
    public static String generateRules(String openApiSpecPath, String outputDir, boolean useJsonSchema) {
        try {
            // Ensure output directory exists
            Files.createDirectories(Paths.get(outputDir));
            
            // Configure and run the generator
            final CodegenConfigurator configurator = new CodegenConfigurator()
                    .setGeneratorName("modsecurity3")
                    .setInputSpec(openApiSpecPath)
                    .setOutputDir(outputDir);
            
            // Add additional properties if needed
            if (!useJsonSchema) {
                configurator.addAdditionalProperty("skipJsonSchema", "true");
            }
            
            final List<File> files = new DefaultGenerator()
                    .opts(configurator.toClientOptInput())
                    .generate();
            
            if (files.isEmpty()) {
                throw new RuntimeException("No files were generated");
            }
            
            // Create the rules directory structure as expected by the Docker container
            String rulesDir = outputDir + "/rules";
            Files.createDirectories(Paths.get(rulesDir));
            
            // Copy the mainconfig.conf to the rules directory as main.conf
            Path mainConfSrc = Paths.get(outputDir, "mainconfig.conf");
            Path mainConfDest = Paths.get(rulesDir, "main.conf");
            Files.copy(mainConfSrc, mainConfDest, StandardCopyOption.REPLACE_EXISTING);
            
            // Copy all API conf files to the rules directory
            Files.list(Paths.get(outputDir))
                .filter(path -> path.toString().endsWith("Api.conf"))
                .forEach(src -> {
                    try {
                        Path dest = Paths.get(rulesDir, src.getFileName().toString());
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy API conf file: " + src, e);
                    }
                });
            
            // If JSON Schema is enabled, create the schemas directory and copy the schemas
            if (useJsonSchema) {
                String schemasDir = outputDir + "/schemas";
                Files.createDirectories(Paths.get(schemasDir));
                
                // Copy all JSON schema files to the schemas directory
                Files.list(Paths.get(outputDir))
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(src -> {
                            try {
                                Path dest = Paths.get(schemasDir, src.getFileName().toString());
                                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to copy schema file: " + src, e);
                            }
                        });
            }
            
            return outputDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ModSecurity rules", e);
        }
    }
}