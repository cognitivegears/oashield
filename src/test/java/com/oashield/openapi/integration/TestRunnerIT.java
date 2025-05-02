package com.oashield.openapi.integration;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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
@Test
public class TestRunnerIT extends AbstractTestNGCucumberTests {
    
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
    
    @Test
    public void runCucumberTests() {
        // This method will be called by TestNG and will trigger the execution of Cucumber features
    }
}