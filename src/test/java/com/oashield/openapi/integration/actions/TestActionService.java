package com.oashield.openapi.integration.actions;

import com.oashield.openapi.integration.config.TestConfigurationService;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Centralized test action service facade for integration tests.
 */
public final class TestActionService {

    private static final Logger logger = LoggerFactory.getLogger(TestActionService.class);
    private static final TestConfigurationService configService = TestConfigurationService.getInstance();
    private static String baseUrl;

    private TestActionService() {
        // Prevent instantiation
    }

    /**
     * Start the Coraza container with given rules directory.
     *
     * @param rulesDirectory directory containing ModSecurity rules
     * @return the base URL of the started container
     */
    public static String startContainerWithRules(String rulesDirectory) {
        baseUrl = ContainerAction.startContainerWithRules(rulesDirectory);
        return baseUrl;
    }

    /**
     * Verify container health.
     *
     * @param url base URL to verify
     */
    public static void verifyContainerHealth(String url) {
        ContainerAction.verifyContainerHealth(url);
    }

    /**
     * Stop the Coraza container.
     */
    public static void stopContainer() {
        ContainerAction.stopContainer();
    }

    /**
     * Execute GET request to the given path.
     *
     * @param path endpoint path or full URL
     * @return Response object or null if skipped
     */
    public static Response executeGetRequest(String path) {
        if (configService.isHttpCallsSkipped()) {
            logger.info("Skipping GET request to {} due to skip.http.calls", path);
            return null;
        }
        return HttpRequestAction.executeGetRequest(buildUrl(path));
    }

    /**
     * Execute POST request with JSON body to given path.
     *
     * @param path        endpoint path or full URL
     * @param requestBody JSON body as String
     * @return Response object or null if skipped
     */
    public static Response executePostRequest(String path, String requestBody) {
        if (configService.isHttpCallsSkipped()) {
            logger.info("Skipping POST request to {} due to skip.http.calls", path);
            return null;
        }
        return HttpRequestAction.executePostRequest(buildUrl(path), requestBody);
    }

    /**
     * Execute HTTP request with specified method to given path.
     *
     * @param method HTTP method name
     * @param path   endpoint path or full URL
     * @return Response object or null if skipped
     */
    public static Response executeMethodRequest(String method, String path) {
        if (configService.isHttpCallsSkipped()) {
            logger.info("Skipping {} request to {} due to skip.http.calls", method, path);
            return null;
        }
        return HttpRequestAction.executeMethodRequest(method, buildUrl(path));
    }

    /**
     * Execute GET request with query parameters to given path.
     *
     * @param path   endpoint path or full URL
     * @param params map of query parameters
     * @return Response object or null if skipped
     */
    public static Response executeGetRequestWithParams(String path, Map<String, String> params) {
        if (configService.isHttpCallsSkipped()) {
            logger.info("Skipping GET request with params to {} due to skip.http.calls", path);
            return null;
        }
        return HttpRequestAction.executeGetRequestWithParams(buildUrl(path), params);
    }

    /**
     * Assert that the response has expected status code.
     *
     * @param response       Response object
     * @param expectedStatus expected HTTP status code
     * @param context        context description
     */
    public static void assertStatusCode(Response response, int expectedStatus, String context) {
        if (configService.isHttpCallsSkipped() && response == null) {
            System.out.println("Skipping assertion for " + context + " due to skip.http.calls");
            return;
        }
        AssertionAction.assertStatusCode(response, expectedStatus, context);
    }

    /**
     * Assert valid (200) response for endpoint.
     *
     * @param response Response object
     * @param endpoint endpoint path or description
     */
    public static void assertValidResponse(Response response, String endpoint) {
        if (configService.isHttpCallsSkipped() && response == null) {
            System.out.println("Skipping assertion for " + endpoint + " due to skip.http.calls");
            return;
        }
        AssertionAction.assertValidResponse(response, endpoint);
    }

    /**
     * Assert blocked (403) response for endpoint.
     *
     * @param response Response object
     * @param endpoint endpoint path or description
     */
    public static void assertBlockedResponse(Response response, String endpoint) {
        if (configService.isHttpCallsSkipped() && response == null) {
            System.out.println("Skipping assertion for " + endpoint + " due to skip.http.calls");
            return;
        }
        AssertionAction.assertBlockedResponse(response, endpoint);
    }

    /**
     * Assert status code with relaxed validation if strict validation is skipped.
     *
     * @param response       Response object
     * @param expectedStatus expected HTTP status code
     * @param context        context description
     */
    public static void assertStatusCodeWithRelaxedValidation(Response response, int expectedStatus, String context) {
        if (configService.isHttpCallsSkipped() && response == null) {
            System.out.println("Skipping assertion for " + context + " due to skip.http.calls");
            return;
        }
        AssertionAction.assertStatusCodeWithRelaxedValidation(response, expectedStatus, context);
    }

    /**
     * Generate rules and verify generated files.
     *
     * @param specPath      path to OpenAPI spec
     * @param outputDir     directory to generate rules
     * @param useJsonSchema whether JSON Schema validation is enabled
     * @return output directory path
     */
    public static String generateAndVerifyRules(String specPath, String outputDir, boolean useJsonSchema) {
        return RuleGenerationAction.generateAndVerifyRules(specPath, outputDir, useJsonSchema);
    }

    /**
     * Verify generated rule files exist.
     *
     * @param outputDir        directory containing generated files
     * @param expectJsonSchema whether JSON Schema files are expected
     */
    public static void verifyRuleFiles(String outputDir, boolean expectJsonSchema) {
        RuleGenerationAction.verifyRuleFiles(outputDir, expectJsonSchema);
    }

    private static String buildUrl(String path) {
        if (path.startsWith("http")) {
            return path;
        }
        return baseUrl + path;
    }
}
