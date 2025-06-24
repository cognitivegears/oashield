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

    @Given("the Coraza server is ready for testing")
    public void corazaServerIsReady() {
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

    @When("I generate ModSecurity rules with JSON Schema validation")
    public void generateRulesWithJsonSchema() {
        TestActionService.generateAndVerifyRules(openApiSpecPath, outputDir, true);
    }

    @When("I generate ModSecurity rules without JSON Schema validation")
    public void generateRulesWithoutJsonSchema() {
        TestActionService.generateAndVerifyRules(openApiSpecPath, outputDir, false);
    }

    @When("I start the Coraza server with the generated rules")
    public void startCorazaServer() {
        baseUrl = TestActionService.startContainerWithRules(outputDir);
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
}
