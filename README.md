# OpenAPI Generator for ModSecurity3

## Overview
This project provides a generator for creating ModSecurity configuration files based on OpenAPI specifications. By leveraging the OpenAPI spec, the generated ModSecurity rules will only allow valid API calls, enhancing security by disallowing any undefined operations.

## Description
Traditional ModSecurity rules rely on pattern matching to detect and block suspicious requests. This generator takes a different approach by using the OpenAPI specification to define what constitutes a valid request. For example, if an API specification does not define a POST method for a particular endpoint, the generated rules will disallow any POST requests to that endpoint. These rules can be deployed alongside the API or in a sidecar to provide an additional security layer.

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
     java -cp /path/to/openapi-generator-cli.jar:/path/to/your.jar org.openapitools.codegen.OpenAPIGenerator generate -g modsecurity3 -i /path/to/openapi.yaml -o ./output
     ```
     Replace `/path/to/openapi-generator-cli.jar`, `/path/to/your.jar`, and `/path/to/openapi.yaml` with the appropriate paths.

   - For Windows users, use `;` instead of `:` in the classpath:
     ```
     java -cp /path/to/openapi-generator-cli.jar;/path/to/your.jar org.openapitools.codegen.OpenAPIGenerator generate -g modsecurity3 -i /path/to/openapi.yaml -o ./output
     ```

3. **Deploy the generated rules:**
   - Copy the generated ModSecurity configuration files from the `./output` directory to your ModSecurity setup.

## License
This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.