package com.oashield.openapi.generators.modsecurity3.tests;

import com.oashield.openapi.generators.modsecurity3.ConfigurationManager;
import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;
import com.oashield.openapi.generators.modsecurity3.TemplateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.SupportingFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TemplateManager class.
 */
public class TemplateManagerTest {

    private ConfigurationManager mockConfigManager;
    private CodegenConfig mockGenerator;
    private TemplateManager templateManager;
    private final String testOutputFolder = "/tmp/test-output";
    private final Map<String, String> apiTemplateFiles = new HashMap<>();
    private final List<SupportingFile> supportingFiles = new ArrayList<>();

    @BeforeEach
    public void setup() {
        // Create a real generator to use in tests
        mockGenerator = new Modsecurity3Generator();
        
        // Setup the mock configuration manager
        mockConfigManager = new ConfigurationManager(mockGenerator);
        mockConfigManager.setOutputFolder(testOutputFolder);
        
        // Create TemplateManager instance to test
        // Pass both required arguments to the constructor
        templateManager = new TemplateManager(mockGenerator, mockConfigManager);
    }

    @Test
    public void testGetTemplateDir() {
        // Test that the template directory is set correctly
        assertEquals("modsecurity3", templateManager.getTemplateDir());
    }

    @Test
    public void testModelFileFolder() {
        // Test that the model file folder is retrieved correctly
        String folder = templateManager.modelFileFolder();
        assertEquals(testOutputFolder, folder);
    }

    @Test
    public void testApiFileFolder() {
        // Test that the API file folder is retrieved correctly
        String folder = templateManager.apiFileFolder();
        assertEquals(testOutputFolder, folder);
    }

    @Test
    public void testConfigureTemplates() {
        // Clear any existing templates
        mockGenerator.apiTemplateFiles().clear();
        mockGenerator.supportingFiles().clear();
        
        // Configure templates
        templateManager.configureTemplates();
        
        // Verify API template files
        assertTrue(mockGenerator.apiTemplateFiles().containsKey("config.mustache"));
        assertEquals(".conf", mockGenerator.apiTemplateFiles().get("config.mustache"));
        
        // Verify supporting files
        boolean foundMainConfig = false;
        for (SupportingFile file : mockGenerator.supportingFiles()) {
            if (file.getTemplateFile().equals("mainconfig.mustache") && 
                file.getFolder().equals("") && 
                file.getDestinationFilename().equals("mainconfig.conf")) {
                foundMainConfig = true;
                break;
            }
        }
        assertTrue(foundMainConfig, "mainconfig.mustache supporting file should be configured");
    }

    @Test
    public void testInitialize() {
        // Clear any existing templates
        mockGenerator.apiTemplateFiles().clear();
        mockGenerator.supportingFiles().clear();
        
        // Initialize the template manager
        templateManager.initialize();
        
        // Verify templates were configured
        assertFalse(mockGenerator.apiTemplateFiles().isEmpty());
        assertFalse(mockGenerator.supportingFiles().isEmpty());
    }
}