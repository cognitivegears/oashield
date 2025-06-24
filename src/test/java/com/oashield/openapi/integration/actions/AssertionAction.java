package com.oashield.openapi.integration.actions;

import com.oashield.openapi.integration.config.TestConfigurationService;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Standardized assertion actions for integration tests.
 */
public class AssertionAction {

    private static final Logger logger = LoggerFactory.getLogger(AssertionAction.class);
    private static final TestConfigurationService configService = TestConfigurationService.getInstance();

    /**
     * Assert that the response has the expected status code.
     *
     * @param response       the Response object
     * @param expectedStatus expected HTTP status code
     * @param context        description of the assertion context
     */
    public static void assertStatusCode(Response response, int expectedStatus, String context) {
        Assert.assertNotNull(response, "Response is null for " + context);
        int actual = response.getStatusCode();
        logger.info("Asserting status code for {}: expected={}, actual={}", context, expectedStatus, actual);
        Assert.assertEquals(actual, expectedStatus, "Unexpected status code for " + context);
    }

    /**
     * Assert that the response is valid (HTTP 200).
     *
     * @param response the Response object
     * @param endpoint the endpoint being tested
     */
    public static void assertValidResponse(Response response, String endpoint) {
        assertStatusCode(response, 200, "Valid response for " + endpoint);
    }

    /**
     * Assert that the response is blocked (HTTP 403).
     *
     * @param response the Response object
     * @param endpoint the endpoint being tested
     */
    public static void assertBlockedResponse(Response response, String endpoint) {
        assertStatusCode(response, 403, "Blocked response for " + endpoint);
    }

    /**
     * Assert status code with relaxed validation if strict validation is skipped.
     *
     * @param response       the Response object
     * @param expectedStatus expected HTTP status code
     * @param context        description of the assertion context
     */
    public static void assertStatusCodeWithRelaxedValidation(Response response, int expectedStatus, String context) {
        Assert.assertNotNull(response, "Response is null for " + context);
        if (configService.isStrictValidationSkipped()) {
            int actual = response.getStatusCode();
            logger.warn("Relaxed validation for {}: expected={}, actual={}", context, expectedStatus, actual);
        } else {
            assertStatusCode(response, expectedStatus, context);
        }
    }
}
