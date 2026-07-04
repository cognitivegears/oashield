<div align="center">

![OAShield](https://github.com/user-attachments/assets/b1051f27-ea0e-44a7-9ff1-d37144c956c3)

# OAShield

**Turn your OpenAPI spec into a Web Application Firewall that only allows valid API calls.**

[![CI](https://github.com/cognitivegears/oashield/actions/workflows/ci.yml/badge.svg)](https://github.com/cognitivegears/oashield/actions/workflows/ci.yml)
[![Tests](https://github.com/cognitivegears/oashield/actions/workflows/maven-tests.yml/badge.svg)](https://github.com/cognitivegears/oashield/actions/workflows/maven-tests.yml)
[![CLI Build](https://github.com/cognitivegears/oashield/actions/workflows/maven-build-cli.yml/badge.svg)](https://github.com/cognitivegears/oashield/actions/workflows/maven-build-cli.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE.md)
[![Latest Release](https://img.shields.io/github/v/release/cognitivegears/oashield?sort=semver)](https://github.com/cognitivegears/oashield/releases)

[Website](https://oashield.com) · [Getting Started](docs/getting-started.md) · [Configuration](docs/configuration.md) · [Releases](https://github.com/cognitivegears/oashield/releases)

</div>

---

## What is OAShield?

OAShield (pronounced like "away shield", /əˈweɪ ʃild/) generates Web Application
Firewall (WAF) rules directly from your OpenAPI specification. The generated
rules act as a **positive security model**: they permit exactly the requests
your API defines and deny everything else.

It produces standard [SecLang](https://coraza.io/docs/seclang/) rules for both
**[OWASP ModSecurity v3](https://github.com/owasp-modsecurity/ModSecurity)**
(libmodsecurity) and **[Coraza](https://coraza.io)**, selectable with a single
option.

## Why OAShield?

Traditional WAFs rely on pattern matching — a blocklist that tries to recognize
known-bad requests. That leaves a gap: anything the patterns haven't seen gets
through.

OAShield flips the model. Instead of guessing what's malicious, it uses your
OpenAPI spec as the definition of what's **valid**:

- **Deny by default.** If your spec doesn't define a `POST /orders`, the rules
  reject every `POST /orders` request — no signature required.
- **Enforce your contract.** Undeclared endpoints, methods, parameters, and
  request-body fields are blocked. Type and range constraints from the schema
  are checked.
- **Deploy anywhere SecLang runs.** Ship the rules alongside your API or in a
  sidecar for an extra layer that mirrors your API surface exactly.

Your OpenAPI spec is already the source of truth for your API. OAShield makes it
the source of truth for your perimeter too. Curious what the rules look like?
See [How It Works](docs/how-it-works.md).

## Quick Start

```bash
# 1. Get the CLI (or download from Releases)
git clone https://github.com/cognitivegears/oashield.git
cd oashield
mvn package -P build-cli-jar

# 2. Generate rules from your spec
java -cp target/oashield-cli.jar org.openapitools.codegen.OpenAPIGenerator \
  generate -g modsecurity3 \
  -i samples/petstore.yaml \
  -o output/

# 3. Deploy the generated .conf files to your ModSecurity or Coraza setup
```

Full walkthrough: **[Getting Started](docs/getting-started.md)**.

## Documentation

| Guide | What's inside |
|---|---|
| [Getting Started](docs/getting-started.md) | Install, generate rules, and deploy them |
| [How It Works](docs/how-it-works.md) | What the generated rules do, block by block |
| [Configuration](docs/configuration.md) | Options, engine flavors, and validation limitations |
| [Integration Testing](docs/integration-testing.md) | Running the test suite against real WAF engines |

## Examples

Sample OpenAPI specs live in [`samples/`](samples), with pre-generated rules in
[`samples/output/petstore/`](samples/output/petstore) so you can see what
OAShield produces before running it yourself.

## Contributing

Issues and pull requests are welcome. Please run `mvn test` before submitting;
see [Integration Testing](docs/integration-testing.md) for the full suite.

## License

Licensed under the Apache License 2.0. See [LICENSE.md](LICENSE.md).
