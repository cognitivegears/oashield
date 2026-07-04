package com.oashield.openapi.integration.steps;

import com.oashield.openapi.integration.actions.TestActionService;
import com.oashield.openapi.integration.config.TestConfigurationService;
import com.oashield.openapi.integration.data.TestDataService;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModSecurityStepDefinitions {
    private String openApiSpecPath;
    private String outputDir;
    private String baseUrl;
    private Map<String, Object> testContext = new HashMap<>();

    @Before
    public void setup() {
        outputDir = TestConfigurationService.getInstance().createUniqueOutputDirectory();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @After
    public void cleanup() {
        TestActionService.stopContainer();
        try {
            if (Files.exists(Paths.get(outputDir))) {
                Files.walk(Paths.get(outputDir))
                    .sorted((a, b) -> -a.compareTo(b))
                    .map(path -> path.toFile())
                    .forEach(File::delete);
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up test files: " + e.getMessage());
        }
    }

    @Given("the WAF test environment is ready")
    public void wafTestEnvironmentIsReady() {
        // Placeholder for Docker availability
    }

    @Given("an OpenAPI specification file at {string}")
    public void anOpenApiSpecificationFileAt(String specPath) {
        String filename = Paths.get(specPath).getFileName().toString();
        String specName = filename.contains(".")
            ? filename.substring(0, filename.lastIndexOf('.'))
            : filename;
        openApiSpecPath = TestDataService.getInstance().getOpenApiSpecPath(specName);
        testContext.put("specName", specName);
    }

    @When("I generate rules with body validation for {string}")
    public void generateRulesWithBodyValidation(String engineFlavor) {
        testContext.put("engineFlavor", engineFlavor);
        TestActionService.generateAndVerifyRules(openApiSpecPath, outputDir, true, engineFlavor);
    }

    @When("I generate rules without body validation for {string}")
    public void generateRulesWithoutBodyValidation(String engineFlavor) {
        testContext.put("engineFlavor", engineFlavor);
        TestActionService.generateAndVerifyRules(openApiSpecPath, outputDir, false, engineFlavor);
    }

    @When("I start the WAF server with the generated rules")
    public void startWafServer() {
        String engineFlavor = (String) testContext.getOrDefault("engineFlavor", "coraza");
        baseUrl = TestActionService.startContainerWithRules(outputDir, engineFlavor);
    }

    @Then("a valid GET request to {string} should return a {int} status code")
    public void validGetRequestShouldReturnStatusCode(String path, int expectedStatus) {
        Response response = TestActionService.executeGetRequest(path);
        TestActionService.assertValidResponse(response, path);
    }

    @Then("an invalid GET request to {string} should be blocked with a {int} status code")
    public void invalidGetRequestShouldBeBlocked(String path, int expectedStatus) {
        Response response = TestActionService.executeGetRequest(path);
        TestActionService.assertStatusCodeWithRelaxedValidation(response, expectedStatus, "Invalid GET request to " + path);
    }

    @Then("a POST request to {string} with a valid body should return a {int} status code")
    public void postRequestWithValidBodyShouldReturnStatusCode(String path, int expectedStatus) {
        String specName = (String) testContext.get("specName");
        String validBody = TestDataService.getInstance().getValidRequestBody(specName + ":" + path);
        Response response = TestActionService.executePostRequest(path, validBody);
        TestActionService.assertValidResponse(response, path);
    }

    @Then("a POST request to {string} with an invalid body should be blocked with a {int} status code")
    public void postRequestWithInvalidBodyShouldBeBlocked(String path, int expectedStatus) {
        String specName = (String) testContext.get("specName");
        String invalidBody = TestDataService.getInstance().getInvalidRequestBody(specName + ":" + path, "missing_required");
        Response response = TestActionService.executePostRequest(path, invalidBody);
        TestActionService.assertStatusCodeWithRelaxedValidation(response, expectedStatus, "POST request with invalid body to " + path);
    }

    @Then("a POST request to {string} with an invalid body should return a {int} status code")
    public void postRequestWithInvalidBodyShouldReturnStatusCode(String path, int expectedStatus) {
        String specName = (String) testContext.get("specName");
        String invalidBody = TestDataService.getInstance().getInvalidRequestBody(specName + ":" + path, "missing_required");
        Response response = TestActionService.executePostRequest(path, invalidBody);
        TestActionService.assertStatusCode(response, expectedStatus, "POST request with invalid body to " + path);
    }

    @Then("a request to an undefined path {string} should be blocked with a {int} status code")
    public void requestToUndefinedPathShouldBeBlocked(String path, int expectedStatus) {
        Response response = TestActionService.executeGetRequest(path);
        TestActionService.assertStatusCodeWithRelaxedValidation(response, expectedStatus, "Request to undefined path " + path);
    }

    @Then("a request using an undefined HTTP method {string} to {string} should be blocked with a {int} status code")
    public void requestWithUndefinedMethodShouldBeBlocked(String method, String path, int expectedStatus) {
        Response response = TestActionService.executeMethodRequest(method, path);
        TestActionService.assertStatusCodeWithRelaxedValidation(response, expectedStatus, "Request using undefined method " + method + " to " + path);
    }

    @Then("a request to {string} should be blocked with a {int} status code")
    public void requestShouldBeBlocked(String path, int expectedStatus) {
        Response response = TestActionService.executeGetRequest(path);
        TestActionService.assertStatusCodeWithRelaxedValidation(response, expectedStatus, "Request to " + path);
    }

    @Then("a GET request to {string} should return a {int} status code")
    public void getRequestShouldReturnStatusCode(String path, int expectedStatus) {
        Response response = TestActionService.executeGetRequest(path);
        TestActionService.assertStatusCode(response, expectedStatus, "GET request to " + path);
    }

    @Then("a GET request to {string} should be blocked with a {int} status code")
    public void getRequestShouldBeBlockedWithStatusCode(String path, int expectedStatus) {
        Response response = TestActionService.executeGetRequest(path);
        TestActionService.assertStatusCodeWithRelaxedValidation(response, expectedStatus, "GET request to " + path);
    }

    @Then("a GET request to {string} without parameters should return a {int} status code")
    public void getRequestWithoutParametersShouldReturnStatusCode(String path, int expectedStatus) {
        Response response = TestActionService.executeGetRequest(path);
        TestActionService.assertStatusCode(response, expectedStatus, "GET request without parameters to " + path);
    }

    @Then("a POST request to {string} with an invalid body {string} should be blocked with a {int} status code")
    public void postRequestWithNamedInvalidBodyShouldBeBlocked(String path, String variant, int expectedStatus) {
        String specName = (String) testContext.get("specName");
        String invalidBody = TestDataService.getInstance().getInvalidRequestBody(specName + ":" + path, variant);
        Response response = TestActionService.executePostRequest(path, invalidBody);
        TestActionService.assertStatusCodeWithRelaxedValidation(response, expectedStatus,
            "POST request with invalid body '" + variant + "' to " + path);
    }

    @Then("a POST request to {string} with content type {string} and body {string} should return a {int} status code")
    public void postRequestWithContentTypeShouldReturnStatusCode(String path, String contentType, String body,
            int expectedStatus) {
        Integer status = TestActionService.executeRawPostStatus(path, contentType, unescapeBody(body));
        TestActionService.assertRawStatus(status, expectedStatus, "POST " + contentType + " request to " + path);
    }

    @Then("a POST request to {string} with content type {string} and body {string} should be blocked with a {int} status code")
    public void postRequestWithContentTypeShouldBeBlocked(String path, String contentType, String body,
            int expectedStatus) {
        Integer status = TestActionService.executeRawPostStatus(path, contentType, unescapeBody(body));
        TestActionService.assertRawStatus(status, expectedStatus, "POST " + contentType + " request to " + path);
    }

    @Then("a POST request to {string} with no body should return a {int} status code")
    public void postRequestWithoutBodyShouldReturnStatusCode(String path, int expectedStatus) {
        Integer status = TestActionService.executeRawPostStatus(path, null, null);
        TestActionService.assertRawStatus(status, expectedStatus, "bodiless POST request to " + path);
    }

    /**
     * Gherkin string arguments cannot carry real CRLFs; multipart bodies in the
     * feature file write them as literal \r\n sequences.
     */
    private static String unescapeBody(String body) {
        return body.replace("\\r\\n", "\r\n");
    }
}
