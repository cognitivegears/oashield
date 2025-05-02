package com.oashield.openapi.integration;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * TestNG Cucumber runner for integration tests.
 * This class must be named to end with "IT" to be picked up by the failsafe plugin.
 */
@Test
@CucumberOptions(
    features = {"src/test/resources/features/modsecurity_rule_generation.feature"},
    glue = {"com.oashield.openapi.integration.steps"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/cucumber-report.html",
        "json:target/cucumber-reports/cucumber-report.json",
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
    }
)
public class CucumberTestNGRunnerIT extends AbstractTestNGCucumberTests {
    
    @BeforeClass
    public void setup() {
        System.out.println("============================================");
        System.out.println("Starting Cucumber tests");
        System.out.println("============================================");
        
        // Log if we're skipping HTTP calls
        String skipHttpCalls = System.getProperty("skip.http.calls", "false");
        System.out.println("skip.http.calls property: " + skipHttpCalls);
    }
    
    @Override
    @DataProvider(parallel = false)  // set to false to avoid parallel execution issues
    public Object[][] scenarios() {
        return super.scenarios();
    }
}