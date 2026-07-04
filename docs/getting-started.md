# Getting Started

This guide walks you from zero to a set of deployable WAF rules generated from
your OpenAPI specification.

## Prerequisites

- Java 8 or later (Java 11+ recommended)
- Maven 3.6.0 or later — only needed if you build from source

## 1. Get the CLI

### Option A — Download a release (fastest)

Download the latest `oashield-cli.jar` from the
[Releases](https://github.com/cognitivegears/oashield/releases) page.

### Option B — Build from source

```bash
git clone https://github.com/cognitivegears/oashield.git
cd oashield
mvn package -P build-cli-jar
```

This produces `target/oashield-cli.jar`.

## 2. Generate rules

```bash
java -cp oashield-cli.jar org.openapitools.codegen.OpenAPIGenerator \
  generate -g modsecurity3 \
  -i /path/to/openapi.yaml \
  -o /path/to/output/dir
```

Replace `-i` with the path to your OpenAPI spec and `-o` with the directory
where the generated `.conf` rules should be written.

> **Windows:** the same command works; if you add multiple classpath entries,
> separate them with `;` instead of `:`.

To validate JSON request bodies against the OpenAPI schema (and to target
[Coraza](https://coraza.io)), select the engine flavor:

```bash
java -cp oashield-cli.jar org.openapitools.codegen.OpenAPIGenerator \
  generate -g modsecurity3 \
  -i /path/to/openapi.yaml \
  -o /path/to/output/dir \
  --additional-properties engineFlavor=coraza,schemaRulePath=rules/schema.json
```

See [Configuration](configuration.md) for every option and the differences
between engine flavors.

## 3. Deploy the rules

Copy the generated `.conf` files from your output directory into your
ModSecurity or Coraza setup, alongside your API or in a sidecar. The rules are
standard SecLang, so they load the same way any other rule file does.

For a worked example, see the sample specs in [`samples/`](../samples) and the
pre-generated output in [`samples/output/petstore/`](../samples/output/petstore).

## Running unit tests

```bash
mvn test
```

For integration tests against real WAF engines, see
[Integration Testing](integration-testing.md).
