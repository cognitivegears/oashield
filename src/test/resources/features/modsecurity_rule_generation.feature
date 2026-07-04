Feature: ModSecurity Rule Generation and Testing
  As a developer using OAShield
  I want to generate WAF rules from OpenAPI specifications
  So that I can protect my API with a WAF that validates requests according to the API spec

  Background:
    Given the WAF test environment is ready

  Scenario Outline: Generate rules for API with body validation
    Given an OpenAPI specification file at "samples/petstore.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a valid GET request to "/pet/1" should return a 200 status code
    And an invalid GET request to "/pet/abc" should be blocked with a 403 status code
    And a POST request to "/pet" with a valid body should return a 200 status code
    And a POST request to "/pet" with an invalid body should be blocked with a 403 status code
    And a request to an undefined path "/undefined/path" should be blocked with a 403 status code
    And a request using an undefined HTTP method "PATCH" to "/pet/1" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Generate rules for API without body validation
    Given an OpenAPI specification file at "samples/petstore.yaml"
    When I generate rules without body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a valid GET request to "/pet/1" should return a 200 status code
    And an invalid GET request to "/pet/abc" should be blocked with a 403 status code
    And a POST request to "/pet" with a valid body should return a 200 status code
    And a POST request to "/pet" with an invalid body should return a 200 status code
    And a request to an undefined path "/undefined/path" should be blocked with a 403 status code
    And a request using an undefined HTTP method "PATCH" to "/pet/1" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Generate rules for API with URL integer parameter validation
    Given an OpenAPI specification file at "samples/urlintparam.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a valid GET request to "/pet/1" should return a 200 status code
    And an invalid GET request to "/pet/abc" should be blocked with a 403 status code
    And a request to "/pet/1.5" should be blocked with a 403 status code
    And a request to "/pet/-1" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Generate rules for API with GET parameter validation
    Given an OpenAPI specification file at "samples/getparam.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a GET request to "/pets?limit=10" should return a 200 status code
    And a GET request to "/pets?limit=abc" should be blocked with a 403 status code
    And a GET request to "/pets?limit=-1" should be blocked with a 403 status code
    And a GET request to "/pets?limit=1000" should be blocked with a 403 status code
    And a GET request to "/pets" without parameters should return a 200 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Composed schema (anyOf / allOf / oneOf) validation
    Given an OpenAPI specification file at "samples/composed.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a GET request to "/items?code=5" should return a 200 status code
    And a GET request to "/items?code=red" should return a 200 status code
    And a GET request to "/items?code=purple" should be blocked with a 403 status code
    And a POST request to "/contact" with a valid body should return a 200 status code
    And a POST request to "/contact" with an invalid body "bad_id" should be blocked with a 403 status code
    And a POST request to "/contact" with an invalid body "bad_phone" should be blocked with a 403 status code
    And a POST request to "/dog" with a valid body should return a 200 status code
    And a POST request to "/dog" with an invalid body "missing_required" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Request body object validation
    Given an OpenAPI specification file at "samples/petstore.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a POST request to "/pet" with a valid body should return a 200 status code
    And a POST request to "/pet" with an invalid body "missing_required" should be blocked with a 403 status code
    And a POST request to "/pet" with an invalid body "invalid_type" should be blocked with a 403 status code
    And a POST request to "/pet" with an invalid body "extra_property" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |
