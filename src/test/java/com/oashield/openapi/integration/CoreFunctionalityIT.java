package com.oashield.openapi.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.oashield.openapi.integration.config.TestConfigurationService;
import com.oashield.openapi.integration.data.TestDataService;
import com.oashield.openapi.integration.util.CorazaContainerManager;
import com.oashield.openapi.integration.util.RuleGenerationUtil;

/**
 * Basic integration test for OAShield functionality.
 * This test checks that the rule generation works correctly and the Coraza container can be started.
 */
public class CoreFunctionalityIT {
    private static String outputDir;
    private static String petStoreSpecPath;

    @BeforeAll
    public static void setup() {
        // Create unique output directory for test output
        outputDir = TestConfigurationService.getInstance().createUniqueOutputDirectory();
        petStoreSpecPath = TestDataService.getInstance().getOpenApiSpecPath("petstore");
    }

    @AfterAll
    public static void cleanup() {
        // Clean up test directory
        try {
            if (Files.exists(Paths.get(outputDir))) {
                Files.walk(Paths.get(outputDir))
                    .sorted((a, b) -> -a.compareTo(b))
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up test files: " + e.getMessage());
        }
    }

    @Test
    public void testRuleGeneration() {
        // Generate rules with JSON Schema validation
        String rulesDir = RuleGenerationUtil.generateRules(petStoreSpecPath, outputDir, true);

        // Verify that main.conf was generated
        assertTrue(Files.exists(Paths.get(outputDir, "rules", "main.conf")),
                "ModSecurity main.conf should be generated");

        // Verify that the JSON Schema was generated
        assertTrue(Files.exists(Paths.get(outputDir, "schemas")),
                "JSON Schema directory should be created");
    }

    @Test
    public void testContainerStartup() {
        if (TestConfigurationService.getInstance().isHttpCallsSkipped()) {
            System.out.println("Skipping container test due to configuration (skip.http.calls)");
            return;
        }

        // Generate rules
        RuleGenerationUtil.generateRules(petStoreSpecPath, outputDir, true);

        // Create container manager
        CorazaContainerManager containerManager = new CorazaContainerManager(outputDir);

        try {
            // Start container
            String baseUrl = containerManager.start();

            // Verify that container started successfully
            assertNotNull(baseUrl, "Container base URL should not be null");
            assertTrue(baseUrl.startsWith("http://"), "Container base URL should start with http://");
        } finally {
            // Stop container
            containerManager.stop();
        }
    }
}
