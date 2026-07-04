package com.oashield.openapi.integration.actions;

import com.oashield.openapi.integration.config.TestConfigurationService;
import com.oashield.openapi.integration.util.CorazaContainerManager;
import com.oashield.openapi.integration.util.ModSecurityContainerManager;
import com.oashield.openapi.integration.util.WafContainerManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Container management actions for integration tests.
 */
public class ContainerAction {

    private static final Logger logger = LoggerFactory.getLogger(ContainerAction.class);
    private static final TestConfigurationService configService = TestConfigurationService.getInstance();
    private static WafContainerManager containerManager;

    /**
     * Start the Coraza container with the specified rules directory.
     *
     * @param rulesDirectory the directory containing ModSecurity rules
     * @return the base URL of the started container
     */
    public static String startContainerWithRules(String rulesDirectory) {
        return startContainerWithRules(rulesDirectory, "coraza");
    }

    /**
     * Start the WAF container for the given engine flavor with the specified rules directory.
     *
     * @param rulesDirectory the directory containing the generated rules (main.conf + *Api.conf)
     * @param engineFlavor   "coraza" or "modsecurity3"
     * @return the base URL of the started container
     */
    public static String startContainerWithRules(String rulesDirectory, String engineFlavor) {
        if (configService.isHttpCallsSkipped()) {
            logger.info("Skipping container startup; skip.http.calls is enabled");
            return "http://localhost:8080";
        }
        if ("modsecurity3".equals(engineFlavor)) {
            containerManager = new ModSecurityContainerManager(rulesDirectory);
        } else {
            containerManager = new CorazaContainerManager(rulesDirectory, configService.getContainerImage());
        }
        logger.info("Starting {} container with rules from: {}", engineFlavor, rulesDirectory);
        String baseUrl = containerManager.start();
        verifyContainerHealth(baseUrl);
        return baseUrl;
    }

    /**
     * Verify the WAF container is healthy by polling the base URL. With rules loaded,
     * / is an undefined endpoint, so a 403 (default deny) also proves the WAF is up.
     *
     * @param baseUrl the base URL of the container
     */
    public static void verifyContainerHealth(String baseUrl) {
        int maxRetries = 5;
        boolean serverReady = false;
        for (int i = 0; i < maxRetries; i++) {
            try {
                logger.info("Health check attempt {} for {}", i + 1, baseUrl);
                Response response = RestAssured.get(baseUrl);
                if (response.getStatusCode() == 200 || response.getStatusCode() == 403) {
                    serverReady = true;
                    break;
                }
            } catch (Exception e) {
                logger.warn("Health check failed on attempt {}: {}", i + 1, e.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertTrue(serverReady, "WAF server failed health check at " + baseUrl);
    }

    /**
     * Stop the WAF container.
     */
    public static void stopContainer() {
        if (containerManager != null) {
            logger.info("Stopping WAF container");
            containerManager.stop();
        }
    }
}
