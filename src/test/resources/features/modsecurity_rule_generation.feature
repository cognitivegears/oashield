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
    Then a valid GET request to "/v2/pet/1" should return a 200 status code
    And an invalid GET request to "/v2/pet/abc" should be blocked with a 403 status code
    And a POST request to "/v2/pet" with a valid body should return a 200 status code
    And a POST request to "/v2/pet" with an invalid body should be blocked with a 403 status code
    And a request to an undefined path "/undefined/path" should be blocked with a 403 status code
    And a request using an undefined HTTP method "PATCH" to "/v2/pet/1" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Generate rules for API without body validation
    Given an OpenAPI specification file at "samples/petstore.yaml"
    When I generate rules without body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a valid GET request to "/v2/pet/1" should return a 200 status code
    And an invalid GET request to "/v2/pet/abc" should be blocked with a 403 status code
    And a POST request to "/v2/pet" with a valid body should return a 200 status code
    And a POST request to "/v2/pet" with an invalid body should return a 200 status code
    And a request to an undefined path "/undefined/path" should be blocked with a 403 status code
    And a request using an undefined HTTP method "PATCH" to "/v2/pet/1" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Generate rules for API with URL integer parameter validation
    Given an OpenAPI specification file at "samples/urlintparam.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a valid GET request to "/v2/pet/1" should return a 200 status code
    And an invalid GET request to "/v2/pet/abc" should be blocked with a 403 status code
    And a request to "/v2/pet/1.5" should be blocked with a 403 status code
    And a request to "/v2/pet/-1" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Generate rules for API with GET parameter validation
    Given an OpenAPI specification file at "samples/getparam.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a GET request to "/v2/pets?limit=10" should return a 200 status code
    And a GET request to "/v2/pets?limit=abc" should be blocked with a 403 status code
    And a GET request to "/v2/pets?limit=-1" should be blocked with a 403 status code
    And a GET request to "/v2/pets?limit=1000" should be blocked with a 403 status code
    And a GET request to "/v2/pets" without parameters should return a 200 status code

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
    Then a POST request to "/v2/pet" with a valid body should return a 200 status code
    And a POST request to "/v2/pet" with an invalid body "missing_required" should be blocked with a 403 status code
    And a POST request to "/v2/pet" with an invalid body "invalid_type" should be blocked with a 403 status code
    And a POST request to "/v2/pet" with an invalid body "extra_property" should be blocked with a 403 status code
    And a POST request to "/v2/user/createWithList" with content type "application/json" and body "[{\"username\":\"alice\"}]" should return a 200 status code
    And a POST request to "/v2/user/createWithList" with content type "application/json" and body "[{\"username\":\"alice\",\"userStatus\":\"notanumber\"}]" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Media type handling for form, multipart, binary, wildcard and optional bodies
    Given an OpenAPI specification file at "samples/multipart.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a POST request to "/profile" with content type "application/x-www-form-urlencoded" and body "displayName=John+Doe&age=30" should return a 200 status code
    And a POST request to "/profile" with content type "application/x-www-form-urlencoded" and body "displayName=bad!name&age=30" should be blocked with a 403 status code
    And a POST request to '/avatar' with content type 'multipart/form-data; boundary=oas' and body '--oas\r\nContent-Disposition: form-data; name="caption"\r\n\r\nhello\r\n--oas--\r\n' should return a 200 status code
    And a POST request to "/blob" with content type "application/octet-stream" and body "0102aabb" should return a 200 status code
    And a POST request to "/anything" with content type "text/plain" and body "whatever" should return a 200 status code
    And a POST request to "/note" with no body should return a 200 status code
    And a POST request to "/note" with content type "application/json" and body "{\"text\":\"hi\"}" should return a 200 status code
    And a POST request to "/note" with content type "application/json" and body "{\"other\":1}" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: Header, cookie, required-parameter and array-count enforcement
    Given an OpenAPI specification file at "samples/paramfeatures.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    When the request has header "X-Request-Id" set to "123e4567-e89b-12d3-a456-426614174000"
    And the request has cookie "session" set to "abcdefghij1234"
    Then a raw GET request to "/widgets?q=test" should return a 200 status code
    When the request has cookie "session" set to "abcdefghij1234"
    Then a raw GET request to "/widgets?q=test" should return a 403 status code
    When the request has header "X-Request-Id" set to "not-a-uuid"
    And the request has cookie "session" set to "abcdefghij1234"
    Then a raw GET request to "/widgets?q=test" should return a 403 status code
    When the request has header "X-Request-Id" set to "123e4567-e89b-12d3-a456-426614174000"
    And the request has header "X-Unknown-Header" set to "anything at all"
    And the request has cookie "session" set to "abcdefghij1234"
    And the request has cookie "tracking_junk" set to "xyz"
    Then a raw GET request to "/widgets?q=test" should return a 200 status code
    When the request has header "X-Request-Id" set to "123e4567-e89b-12d3-a456-426614174000"
    Then a raw GET request to "/widgets?q=test" should return a 403 status code
    When the request has header "X-Request-Id" set to "123e4567-e89b-12d3-a456-426614174000"
    And the request has cookie "session" set to "abcdefghij1234"
    Then a raw GET request to "/widgets" should return a 403 status code
    And a POST request to "/widgets" with content type "application/json" and body "{\"price\":300,\"labels\":[\"a\"]}" should return a 200 status code
    And a POST request to "/widgets" with content type "application/json" and body "{\"price\":123,\"labels\":[\"a\"]}" should be blocked with a 403 status code
    And a POST request to "/widgets" with content type "application/json" and body "{\"price\":300}" should be blocked with a 403 status code
    And a POST request to "/widgets" with content type "application/json" and body "{\"price\":300,\"labels\":[\"a\",\"b\",\"c\",\"d\"]}" should be blocked with a 403 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |

  Scenario Outline: OpenAPI 3.1 const, dependentRequired, patternProperties and nullable
    Given an OpenAPI specification file at "samples/oas31.yaml"
    When I generate rules with body validation for "<engine>"
    And I start the WAF server with the generated rules
    Then a POST request to "/events" with content type "application/json" and body "{\"kind\":\"reminder\",\"start\":\"now\"}" should return a 200 status code
    And a POST request to "/events" with content type "application/json" and body "{\"kind\":\"other\",\"start\":\"now\"}" should be blocked with a 403 status code
    And a POST request to "/events" with content type "application/json" and body "{\"kind\":\"reminder\",\"start\":\"now\",\"end\":\"later\"}" should return a 200 status code
    And a POST request to "/events" with content type "application/json" and body "{\"kind\":\"reminder\",\"end\":\"later\"}" should be blocked with a 403 status code
    And a POST request to "/events" with content type "application/json" and body "{\"kind\":\"reminder\",\"start\":\"now\",\"labels\":{\"x-a\":5}}" should return a 200 status code
    And a POST request to "/events" with content type "application/json" and body "{\"kind\":\"reminder\",\"start\":\"now\",\"labels\":{\"x-a\":\"notanumber\"}}" should be blocked with a 403 status code
    And a POST request to "/events" with content type "application/json" and body "{\"kind\":\"reminder\",\"start\":\"now\",\"labels\":{\"unrelated\":5}}" should be blocked with a 403 status code
    And a POST request to "/events" with content type "application/json" and body "{\"kind\":\"reminder\",\"start\":\"now\",\"note\":null}" should return a 200 status code

    Examples:
      | engine       |
      | coraza       |
      | modsecurity3 |
