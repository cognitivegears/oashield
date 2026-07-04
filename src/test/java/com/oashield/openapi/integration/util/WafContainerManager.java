package com.oashield.openapi.integration.util;

/**
 * Common contract for WAF engine containers used in integration tests.
 */
public interface WafContainerManager {
    /**
     * Starts the container.
     *
     * @return The base URL for accessing the container
     */
    String start();

    /**
     * Stops the container.
     */
    void stop();

    /**
     * Gets the base URL for accessing the container.
     *
     * @return The base URL in the format http://host:port
     */
    String getBaseUrl();
}
