package com.oashield.openapi.integration;

import org.testng.annotations.Test;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Integration test runner for OAShield
 * This class is specifically named with the IT suffix to be picked up by the maven-failsafe-plugin
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "com.oashield.openapi.integration.steps",
    plugin = {
        "pretty",
        "html:target/cucumber-reports",
        "json:target/cucumber-reports/cucumber.json",
        "tech.grasshopper.extentreports.cucumber7.adapter.ExtentCucumberAdapter:"
    }
)
public class OAShieldIT extends AbstractTestNGCucumberTests {
    
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
    
    @Test
    public void verifyTestRunnerConfiguration() {
        // This empty test is just to ensure that the class is recognized by TestNG
        // The actual tests are run via the Cucumber runner
    }
}