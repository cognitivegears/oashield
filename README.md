# OAShield

![oashield_500](https://github.com/user-attachments/assets/b1051f27-ea0e-44a7-9ff1-d37144c956c3)

## Overview
The OAShield (pronounced like "away shield" or /əˈweɪ ʃild/) project provides a generator for creating Web Application Firewall (WAF) configuration files based on OpenAPI specifications. By leveraging the OpenAPI spec, the generated rules will only allow valid API calls, enhancing security by disallowing any undefined operations.

Currently, OAShield supports generating Modsecurity3 rules, though additional rule generations are planned for the future.

## Description
Traditional WAF rules rely on pattern matching to detect and block suspicious requests. This generator takes a different approach by using the OpenAPI specification to define what constitutes a valid request. For example, if an API specification does not define a POST method for a particular endpoint, the generated rules will disallow any POST requests to that endpoint. These rules can be deployed alongside the API or in a sidecar to provide an additional security layer.

## Usage Instructions


### Build the Project:
1. **Prerequisites:**
    - Java 8 or later
    - Maven 3.6.0 or later
2. Clone the repository:
    ```
      git clone https://github.com/cognitivegears/oashield.git
    ```
3. Build the generator project including the CLI jar:
     ```
     cd oashield
     mvn package -P build-cli-jar
     ```
This will produce a JAR file `oashield-cli.jar` in the `target` directory.

### OR Download the CLI Jar:

   - Download the latest release of oashield-cli.jar from the [Releases](https://github.com/cognitivegears/oashield/releases) page.


### Run the generator:
   - Use the following command to generate the ModSecurity configuration:
     ```
     java -cp target/oashield-cli.jar org.openapitools.codegen.OpenAPIGenerator generate -g modsecurity3 -i /path/to/openapi.yaml -o /path/to/output/dir
     ```
     Replace `/path/to/openapi.yaml`, and `/path/to/output/dir` with the appropriate paths.

   - For Windows users, use `;` instead of `:` in the classpath:
     ```
     java -cp target/oashield-cli.jar org.openapitools.codegen.OpenAPIGenerator generate -g modsecurity3 -i /path/to/openapi.yaml -o /path/to/output/dir
     ```

### Deploy the generated rules:

Copy the generated ModSecurity configuration files from the output path (i.e. `/path/to/output/dir`) to your ModSecurity setup.

## Testing

### Unit Tests

Run the unit tests with:

```bash
mvn test
```

### Integration Tests

Integration tests validate the generated rules against a real ModSecurity-compatible WAF (Coraza). These tests require Docker to be installed and running.

Run the integration tests with:

```bash
mvn verify
```

or specifically:

```bash
mvn failsafe:integration-test failsafe:verify
```

The integration tests:
- Generate ModSecurity rules from sample OpenAPI specifications
- Launch a Coraza WAF container with the generated rules
- Send HTTP requests to validate that the rules correctly enforce the API specification
- Test various scenarios including parameter validation, path validation, and method validation

Test reports are generated in the `target/extent-reports` directory.

#### Note for ARM Architecture Users (Mac M1/M2/M3)

Integration tests now support ARM-based systems (like Mac M1/M2/M3) as the Docker container is available for ARM architecture. You can run the integration tests normally on ARM systems:

```bash
mvn verify
```

If you want to skip the HTTP calls for any reason, you can use:

```bash
mvn verify -Dskip.http.calls=true
```

You can also run tests with relaxed validation for status codes (useful when running on systems where some rules may not work correctly yet):

```bash
mvn verify -Dskip.strict.validation=true
```

### Continuous Integration

This project uses GitHub Actions for continuous integration:

- **OAShield Tests**: Runs on all PRs and pushes to the main branch, executing unit and integration tests.
- **OAShield CLI Build**: Builds the CLI JAR and verifies its functionality.
- **OAShield CI**: Comprehensive workflow that builds, tests, and verifies both the library and CLI.

The GitHub Actions workflows automatically skip HTTP calls during testing to ensure compatibility with the CI environment.

## License
This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE.md) file for details.
