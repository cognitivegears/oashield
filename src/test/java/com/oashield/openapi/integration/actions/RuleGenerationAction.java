package com.oashield.openapi.integration.actions;

import com.oashield.openapi.integration.util.RuleGenerationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Rule generation actions for integration tests.
 */
public class RuleGenerationAction {
    private static final Logger logger = LoggerFactory.getLogger(RuleGenerationAction.class);

    /**
     * Generate rules from the OpenAPI spec and verify the generated files.
     *
     * @param specPath      path to the OpenAPI specification
     * @param outputDir     directory where rules are generated
     * @param useJsonSchema whether JSON Schema validation is enabled
     * @return the output directory path
     */
    public static String generateAndVerifyRules(String specPath, String outputDir, boolean useJsonSchema) {
        return generateAndVerifyRules(specPath, outputDir, useJsonSchema, "coraza");
    }

    /**
     * Generate rules for the given engine flavor and verify the generated files.
     *
     * @param specPath      path to the OpenAPI specification
     * @param outputDir     directory where rules are generated
     * @param useJsonSchema whether body validation is enabled
     * @param engineFlavor  "coraza" or "modsecurity3"
     * @return the output directory path
     */
    public static String generateAndVerifyRules(String specPath, String outputDir, boolean useJsonSchema, String engineFlavor) {
        logger.info("Generating rules for {} with JSON Schema={}", engineFlavor, useJsonSchema);
        String result = RuleGenerationUtil.generateRules(specPath, outputDir, useJsonSchema, engineFlavor);
        verifyRuleFiles(outputDir, useJsonSchema);
        return result;
    }

    /**
     * Verify that the expected rule files exist after generation.
     *
     * @param outputDir        directory where rules are generated
     * @param expectJsonSchema whether JSON Schema files are expected
     */
    public static void verifyRuleFiles(String outputDir, boolean expectJsonSchema) {
        Path rulesDir = Paths.get(outputDir, "rules");
        Assert.assertTrue(Files.exists(rulesDir), "Rules directory does not exist: " + rulesDir);

        Path mainConf = rulesDir.resolve("main.conf");
        Assert.assertTrue(Files.exists(mainConf), "main.conf not found in rules directory");

        File[] confFiles = rulesDir.toFile().listFiles((dir, name) -> name.endsWith("Api.conf"));
        Assert.assertNotNull(confFiles, "No API conf files found");
        Assert.assertTrue(confFiles.length > 0, "No API conf files present in rules directory");

        if (expectJsonSchema) {
            Path schemasDir = Paths.get(outputDir, "schemas");
            Assert.assertTrue(Files.exists(schemasDir), "Schemas directory not found: " + schemasDir);

            File[] jsonFiles = schemasDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            Assert.assertNotNull(jsonFiles, "No JSON schema files found");
            Assert.assertTrue(jsonFiles.length > 0, "No JSON schema files present in schemas directory");
        }
    }
}
