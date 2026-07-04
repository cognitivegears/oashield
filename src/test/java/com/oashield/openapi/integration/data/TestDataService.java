package com.oashield.openapi.integration.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.oashield.openapi.integration.config.TestConfigurationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main singleton service for test data management.
 * Provides thread-safe access to test data templates and OpenAPI specs.
 */
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);

    private static volatile TestDataService instance;

    private final Map<String, RequestBodyTemplate> templateCache;

    private final TestConfigurationService configService;

    private TestDataService() {
        this.templateCache = new ConcurrentHashMap<>();
        this.configService = TestConfigurationService.getInstance();
        logger.info("Initializing TestDataService");
    }

    /**
     * Returns the singleton instance of TestDataService.
     *
     * @return singleton instance
     */
    public static TestDataService getInstance() {
        if (instance == null) {
            synchronized (TestDataService.class) {
                if (instance == null) {
                    instance = new TestDataService();
                }
            }
        }
        return instance;
    }

    /**
     * Parses the specification name from the endpoint identifier.
     *
     * @param endpoint endpoint in format "specName:/endpointPath"
     * @return the specName
     * @throws InvalidEndpointFormatException if format is invalid
     */
    private String parseSpecName(String endpoint) {
        if (endpoint == null) {
            throw new InvalidEndpointFormatException("Endpoint cannot be null");
        }
        int idx = endpoint.indexOf(':');
        if (idx <= 0) {
            throw new InvalidEndpointFormatException("Invalid endpoint format: " + endpoint);
        }
        String specName = endpoint.substring(0, idx).trim();
        if (specName.isEmpty()) {
            throw new InvalidEndpointFormatException("Specification name is empty in endpoint: " + endpoint);
        }
        return specName;
    }

    /**
     * Parses the endpoint path from the endpoint identifier.
     *
     * @param endpoint endpoint in format "specName:/endpointPath"
     * @return endpointPath without leading slash
     * @throws InvalidEndpointFormatException if format is invalid
     */
    private String parseEndpointPath(String endpoint) {
        if (endpoint == null) {
            throw new InvalidEndpointFormatException("Endpoint cannot be null");
        }
        int idx = endpoint.indexOf(':');
        if (idx < 0 || idx == endpoint.length() - 1) {
            throw new InvalidEndpointFormatException("Invalid endpoint format: " + endpoint);
        }
        String path = endpoint.substring(idx + 1);
        if (!path.startsWith("/")) {
            throw new InvalidEndpointFormatException("Endpoint path must start with '/': " + path);
        }
        String trimmed = path.substring(1);
        if (trimmed.isEmpty()) {
            throw new InvalidEndpointFormatException("Endpoint path is empty in endpoint: " + endpoint);
        }
        return trimmed;
    }

    /**
     * Returns a processed valid request body JSON for the given endpoint.
     *
     * @param endpoint endpoint identifier "specName:/endpointPath"
     * @return processed JSON string
     * @throws TemplateNotFoundException         if template file not found
     * @throws TemplateProcessingException       if processing fails
     * @throws InvalidEndpointFormatException    if endpoint format invalid
     */
    public String getValidRequestBody(String endpoint) {
        String key = "valid|" + endpoint;
        RequestBodyTemplate template = templateCache.computeIfAbsent(key, k -> {
            String specName = parseSpecName(endpoint);
            String endpointPath = parseEndpointPath(endpoint);
            Path filePath = Paths.get(configService.getTestDataDirectory(),
                    "test-data", specName, endpointPath, "valid.json");
            if (!Files.exists(filePath)) {
                throw new TemplateNotFoundException("Valid template not found: " + filePath);
            }
            try {
                String content = new String(Files.readAllBytes(filePath));
                return new RequestBodyTemplate(content);
            } catch (IOException e) {
                throw new TemplateNotFoundException("Failed to read template: " + filePath, e);
            }
        });
        logger.debug("Processing valid template for endpoint: {}", endpoint);
        return template.process();
    }

    /**
     * Returns a processed invalid request body JSON for the given endpoint and scenario.
     *
     * @param endpoint endpoint identifier "specName:/endpointPath"
     * @param scenario invalid scenario name without extension
     * @return processed JSON string
     * @throws TemplateNotFoundException         if template file not found
     * @throws TemplateProcessingException       if processing fails
     * @throws InvalidEndpointFormatException    if endpoint format invalid
     */
    public String getInvalidRequestBody(String endpoint, String scenario) {
        String key = "invalid|" + endpoint + "|" + scenario;
        RequestBodyTemplate template = templateCache.computeIfAbsent(key, k -> {
            String specName = parseSpecName(endpoint);
            String endpointPath = parseEndpointPath(endpoint);
            Path filePath = Paths.get(configService.getTestDataDirectory(),
                    "test-data", specName, endpointPath, "invalid", scenario + ".json");
            if (!Files.exists(filePath)) {
                throw new TemplateNotFoundException("Invalid template not found: " + filePath);
            }
            try {
                String content = new String(Files.readAllBytes(filePath));
                return new RequestBodyTemplate(content);
            } catch (IOException e) {
                throw new TemplateNotFoundException("Failed to read template: " + filePath, e);
            }
        });
        logger.debug("Processing invalid template for endpoint: {}, scenario: {}", endpoint, scenario);
        return template.process();
    }

    /**
     * Returns the path to the OpenAPI specification file for the given specName.
     *
     * @param specName specification name
     * @return absolute path to spec file
     * @throws SpecificationNotFoundException if spec file not found
     */
    public String getOpenApiSpecPath(String specName) {
        if (specName == null || specName.trim().isEmpty()) {
            throw new SpecificationNotFoundException("Specification name must not be null or empty");
        }
        Path specPath = Paths.get("samples", specName + ".yaml").toAbsolutePath();
        if (!Files.exists(specPath)) {
            throw new SpecificationNotFoundException("Specification file not found: " + specPath);
        }
        logger.debug("OpenAPI spec path: {}", specPath);
        return specPath.toString();
    }

    /**
     * Returns a list of available test scenarios for the given specification.
     *
     * @param specName specification name
     * @return list of TestScenario objects; empty if none found
     */
    public List<TestScenario> getTestScenarios(String specName) {
        List<TestScenario> scenarios = new ArrayList<>();
        Path baseDir = Paths.get(configService.getTestDataDirectory(), "test-data", specName);
        if (!Files.isDirectory(baseDir)) {
            return scenarios;
        }
        try {
            Files.list(baseDir).filter(Files::isDirectory).forEach(epDir -> {
                String endpointPath = epDir.getFileName().toString();
                // valid scenario
                Path validFile = epDir.resolve("valid.json");
                if (Files.exists(validFile)) {
                    scenarios.add(new TestScenario(endpointPath,
                            TestScenario.ScenarioType.VALID,
                            "Valid request body for endpoint " + endpointPath));
                }
                // invalid scenarios
                Path invalidDir = epDir.resolve("invalid");
                if (Files.isDirectory(invalidDir)) {
                    try {
                        Files.list(invalidDir)
                             .filter(p -> p.toString().endsWith(".json"))
                             .forEach(p -> {
                                 String name = p.getFileName().toString();
                                 String scen = name.substring(0, name.length() - 5);
                                 scenarios.add(new TestScenario(endpointPath + ":" + scen,
                                         TestScenario.ScenarioType.INVALID,
                                         "Invalid scenario " + scen + " for endpoint " + endpointPath));
                             });
                    } catch (IOException e) {
                        logger.warn("Failed to list invalid scenarios in {}", invalidDir, e);
                    }
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to list test scenarios in {}", baseDir, e);
        }
        return scenarios;
    }

    /**
     * Returns query parameters for the given endpoint.
     *
     * @param endpoint endpoint identifier
     * @param valid whether to return valid or invalid parameters
     * @return map of query parameters (currently empty)
     */
    public Map<String, String> getQueryParameters(String endpoint, boolean valid) {
        // TODO: implement query parameter extraction from test data
        return Collections.emptyMap();
    }
}
