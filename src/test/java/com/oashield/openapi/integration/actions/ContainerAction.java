package com.oashield.openapi.integration.actions;

import com.oashield.openapi.integration.config.TestConfigurationService;
import com.oashield.openapi.integration.util.CorazaContainerManager;
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
    private static CorazaContainerManager containerManager;

    /**
     * Start the Coraza container with the specified rules directory.
     *
     * @param rulesDirectory the directory containing ModSecurity rules
     * @return the base URL of the started container
     */
    public static String startContainerWithRules(String rulesDirectory) {
        if (configService.isHttpCallsSkipped()) {
            logger.info("Skipping container startup; skip.http.calls is enabled");
            return "http://localhost:8080";
        }
        String image = configService.getContainerImage();
        containerManager = new CorazaContainerManager(rulesDirectory, image);
        logger.info("Starting Coraza container with rules from: {}", rulesDirectory);
        String baseUrl = containerManager.start();
        verifyContainerHealth(baseUrl);
        return baseUrl;
    }

    /**
     * Verify the Coraza container is healthy by polling the base URL.
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
                if (response.getStatusCode() == 200) {
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
        Assert.assertTrue(serverReady, "Coraza server failed health check at " + baseUrl);
    }

    /**
     * Stop the Coraza container.
     */
    public static void stopContainer() {
        if (containerManager != null) {
            logger.info("Stopping Coraza container");
            containerManager.stop();
        }
    }
}
