package com.oashield.openapi.generators.modsecurity3;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/**
 * Main test class that originally contained all tests,
 * now it just serves as a convenience entry point for debugging the generator.
 * All specific test cases have been moved to specialized test classes in the tests package.
 */
public class Modsecurity3GeneratorTest {

    /**
     * Use this test to launch the code generator in the debugger.
     * This allows you to easily set break points in Modsecurity3Generator.
     */
    @Test
    public void launchCodeGenerator() {
        // to understand how the 'openapi-generator-cli' module is using 'CodegenConfigurator', have a look at the 'Generate' class:
        // https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-cli/src/main/java/org/openapitools/codegen/cmd/Generate.java
        final CodegenConfigurator configurator = new CodegenConfigurator()
                .setGeneratorName("modsecurity3") // use this codegen library
                //.setInputSpec("../../../../../../samples/petstore.yaml") // sample OpenAPI file
                //.setInputSpec("https://raw.githubusercontent.com/openapitools/openapi-generator/master/modules/openapi-generator/src/test/resources/2_0/petstore.yaml") // or from the server
                .setInputSpec("samples/petstore.yaml")
                .setOutputDir("output/modsecurity3"); // output directory

        final ClientOptInput clientOptInput = configurator.toClientOptInput();
        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(clientOptInput).generate();
    }
}