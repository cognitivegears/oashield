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

## License
This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE.md) file for details.
