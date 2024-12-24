# OAShield

![oashield_500](https://github.com/user-attachments/assets/b1051f27-ea0e-44a7-9ff1-d37144c956c3)

## Overview
The OAShield (pronounced like "away shield" or /əˈweɪ ʃild/) project provides a generator for creating Web Application Firewall (WAF) configuration files based on OpenAPI specifications. By leveraging the OpenAPI spec, the generated rules will only allow valid API calls, enhancing security by disallowing any undefined operations.

Currently, OAShield supports generating Modsecurity3 rules, though additional rule generations are planned for the future.

## Description
Traditional WAF rules rely on pattern matching to detect and block suspicious requests. This generator takes a different approach by using the OpenAPI specification to define what constitutes a valid request. For example, if an API specification does not define a POST method for a particular endpoint, the generated rules will disallow any POST requests to that endpoint. These rules can be deployed alongside the API or in a sidecar to provide an additional security layer.

## Usage Instructions
1. **Generate the ModSecurity configuration:**
   - Ensure you have the OpenAPI Generator CLI installed. If not, you can download it from [OpenAPI Generator](https://openapi-generator.tech).
   - Build the generator project:
     ```
     mvn package
     ```
   - This will produce a JAR file in the `target` directory.

2. **Run the generator:**
   - Use the following command to generate the ModSecurity configuration:
     ```
     java -cp /path/to/openapi-generator-cli.jar:target/oashield-0.0.1.jar org.openapitools.codegen.OpenAPIGenerator generate -g modsecurity3 -i /path/to/openapi.yaml -o ./output
     ```
     Replace `/path/to/openapi-generator-cli.jar`, `/path/to/openapi.yaml`, and `./output` with the appropriate paths.

   - For Windows users, use `;` instead of `:` in the classpath:
     ```
     java -cp /path/to/openapi-generator-cli.jar;target/oashield-0.0.1.jar org.openapitools.codegen.OpenAPIGenerator generate -g modsecurity3 -i /path/to/openapi.yaml -o ./output
     ```

3. **Deploy the generated rules:**
   - Copy the generated ModSecurity configuration files from the output path (i.e. `./output`) to your ModSecurity setup.

## License
This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
