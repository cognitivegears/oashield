Feature: ModSecurity Rule Generation and Testing
  As a developer using OAShield
  I want to generate ModSecurity rules from OpenAPI specifications
  So that I can protect my API with a WAF that validates requests according to the API spec

  Background:
    Given the Coraza server is ready for testing

  Scenario: Generate rules for API with JSON Schema validation
    Given an OpenAPI specification file at "samples/petstore.yaml"
    When I generate ModSecurity rules with JSON Schema validation
    And I start the Coraza server with the generated rules
    Then a valid GET request to "/pet/1" should return a 200 status code
    And an invalid GET request to "/pet/abc" should be blocked with a 403 status code
    And a POST request to "/pet" with a valid body should return a 200 status code
    And a POST request to "/pet" with an invalid body should be blocked with a 403 status code
    And a request to an undefined path "/undefined/path" should be blocked with a 403 status code
    And a request using an undefined HTTP method "DELETE" to "/pet/1" should be blocked with a 403 status code

  Scenario: Generate rules for API without JSON Schema validation
    Given an OpenAPI specification file at "samples/petstore.yaml"
    When I generate ModSecurity rules without JSON Schema validation
    And I start the Coraza server with the generated rules
    Then a valid GET request to "/pet/1" should return a 200 status code
    And an invalid GET request to "/pet/abc" should be blocked with a 403 status code
    And a POST request to "/pet" with a valid body should return a 200 status code
    And a POST request to "/pet" with an invalid body should return a 200 status code
    And a request to an undefined path "/undefined/path" should be blocked with a 403 status code
    And a request using an undefined HTTP method "DELETE" to "/pet/1" should be blocked with a 403 status code

  Scenario: Generate rules for API with URL integer parameter validation
    Given an OpenAPI specification file at "samples/urlintparam.yaml"
    When I generate ModSecurity rules with JSON Schema validation
    And I start the Coraza server with the generated rules
    Then a valid GET request to "/pet/1" should return a 200 status code
    And an invalid GET request to "/pet/abc" should be blocked with a 403 status code
    And a request to "/pet/1.5" should be blocked with a 403 status code
    And a request to "/pet/-1" should be blocked with a 403 status code

  Scenario: Generate rules for API with GET parameter validation
    Given an OpenAPI specification file at "samples/getparam.yaml"
    When I generate ModSecurity rules with JSON Schema validation
    And I start the Coraza server with the generated rules
    Then a GET request to "/pets?limit=10" should return a 200 status code
    And a GET request to "/pets?limit=abc" should be blocked with a 403 status code
    And a GET request to "/pets?limit=-1" should be blocked with a 403 status code
    And a GET request to "/pets?limit=1000" should be blocked with a 403 status code
    And a GET request to "/pets" without parameters should return a 200 status code