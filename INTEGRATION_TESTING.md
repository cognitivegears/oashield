# Integration Testing Framework Documentation

## Table of Contents
- [Overview and Purpose](#overview-and-purpose)
- [Prerequisites](#prerequisites)
- [Quick Start Guide](#quick-start-guide)
- [Configuration Options](#configuration-options)
  - [System Properties](#system-properties)
  - [Environment Variables](#environment-variables)
  - [Configuration Files](#configuration-files)
- [Running Tests](#running-tests)
  - [All Tests](#all-tests)
  - [Specific Scenarios](#specific-scenarios)
  - [Environments](#environments)
  - [Skip Modes](#skip-modes)
- [Test Scenarios](#test-scenarios)
- [Adding New Tests](#adding-new-tests)
- [Troubleshooting](#troubleshooting)
- [Architecture](#architecture)
- [Examples](#examples)

## Overview and Purpose

The integration testing framework verifies end-to-end behavior of OAShield by:
- Bootstrapping required services (e.g., Coraza container)
- Executing HTTP requests against sample OpenAPI specifications
- Validating generated ModSecurity rules and API responses

This framework ensures that all components interact correctly under realistic conditions.

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Docker (for containerized tests)
- Git (to clone repository)

Ensure Docker is running and accessible:
```bash
docker info
```

## Quick Start Guide

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/oashield.git
   cd oashield
   ```
2. Build and run all integration tests:
   ```bash
   mvn clean verify
   ```
3. Open the HTML report:
   ```bash
   open target/cucumber-reports/index.html
   ```

## Configuration Options

### System Properties
| Property                   | Default                                                         | Description                           |
|----------------------------|-----------------------------------------------------------------|---------------------------------------|
| `skip.http.calls`          | `false`                                                         | Skip actual HTTP requests             |
| `skip.strict.validation`   | `false`                                                         | Disable strict schema validation      |
| `container.image`          | `ghcr.io/cognitivegears/coraza-validate-server:latest`         | Docker image for Coraza server        |
| `test.timeout`             | `30000` (ms)                                                    | Maximum test execution time           |
| `parallel.execution`       | `true`                                                          | Enable parallel scenario execution    |
| `test.data.directory`      | `${user.dir}/src/test/resources`                                | Path to test data files               |
| `output.directory.base`    | `${java.io.tmpdir}`                                             | Base directory for test outputs       |

### Environment Variables
- `GITHUB_ACTIONS`: Detect CI environment if set to `true`.
- `os.arch`: Used to detect ARM architecture for compatibility.

### Configuration Files
- `src/test/resources/testng.xml`: TestNG suite definition
- `src/test/resources/extent.properties`: Extent report settings
- `src/test/resources/extent-config.xml`: Extent report configuration

## Running Tests

### All Tests
```bash
mvn test
```
or
```bash
mvn test -Dsurefire.suiteXmlFile=src/test/resources/testng.xml
```

### Specific Scenarios
Run a single TestNG class:
```bash
mvn test -Dtest=OAShieldIT
```
Or run a Cucumber feature by tag:
```bash
mvn test -Dcucumber.options="--tags @yourTag"
```

### Environments
- Local (default)
- CI (GitHub Actions)
- ARM Macs (auto-detected via `os.arch`)

### Skip Modes
To skip HTTP calls:
```bash
mvn test -Dskip.http.calls=true
```
To disable strict validation:
```bash
mvn test -Dskip.strict.validation=true
```

## Test Scenarios

Scenarios are defined under:
- Feature files: `src/test/resources/features/*.feature`
- Test data: `src/test/resources/test-data/<scenario>/<…>`

Current scenarios:
- **modsecurity_rule_generation**: Verifies ModSecurity rule templates
- **petstore**: Validates petstore API flows
- **getparam**: Tests URL parameter handling
- **urlintparam**: Tests URL integer parameter parsing

## Adding New Tests

1. Define a new feature:
   - Add a `.feature` file under `src/test/resources/features`.
2. Add test data:
   - Place JSON templates under `src/test/resources/test-data/<yourScenario>`.
3. Implement step definitions (if new actions needed) in:
   `src/test/java/com/oashield/openapi/integration/steps`
4. Verify configuration via `TestConfigurationService` if needed.
5. Run your tests:
   ```bash
   mvn test -Dcucumber.options="--tags @yourScenario"
   ```

## Troubleshooting

- **Docker not found**: Ensure Docker is installed and in PATH.
- **Permission denied writing output**: Check `output.directory.base` and file permissions.
- **Test hangs**: Increase `test.timeout` or disable parallel execution:
  ```bash
  mvn test -Dparallel.execution=false
  ```
- **Feature not recognized**: Verify file paths and TestNG suite includes the Cucumber runner.

## Architecture

The integration framework consists of:
- **Configuration**: `TestConfigurationService` (`integration/config`)
- **Data Loading**: `TestDataService`, `TemplateProcessor` (`integration/data`)
- **Actions**: `HttpRequestAction`, `RuleGenerationAction`, etc. (`integration/actions`)
- **Step Definitions**: Cucumber glue code (`integration/steps`)
- **Runner**: Cucumber–TestNG bridge (`TestRunnerIT`, `CucumberTestNGRunnerIT`)

File structure:
```
.
├─ src/test/resources
│  ├─ features
│  └─ test-data
├─ src/test/java/com/oashield/openapi/integration
│  ├─ config
│  ├─ data
│  ├─ actions
│  ├─ steps
│  └─ TestRunnerIT.java
```

## Examples

**Run only ModSecurity rules generation tests**:
```bash
mvn test -Dcucumber.options="--tags @modsecurity_rule_generation"
```

**Change Coraza container image**:
```bash
mvn test -Dcontainer.image=ghcr.io/myorg/custom-coraza:1.0
```

**Set custom output directory**:
```bash
mvn test -Doutput.directory.base=/tmp/oashield-results
```

--
_Documentation generated on 2025-06-24_
