package com.oashield.openapi.integration.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Manager for the Coraza Docker container used in integration tests.
 * This class handles starting and stopping the container, and provides
 * methods for accessing the container's host and port.
 */
public class CorazaContainerManager implements WafContainerManager {
    private static final String CORAZA_IMAGE = "ghcr.io/cognitivegears/coraza-validate-server:latest";
    private static final int CORAZA_PORT = 8080;

    private final GenericContainer<?> container;

    /**
     * Constructor for the Coraza container manager using default image.
     *
     * @param rulesDirectory The absolute path to the directory containing the rules
     */
    public CorazaContainerManager(String rulesDirectory) {
        this(rulesDirectory, CORAZA_IMAGE);
    }

    /**
     * Constructor for the Coraza container manager with custom image.
     *
     * @param rulesDirectory The absolute path to the directory containing the rules (main.conf + *Api.conf)
     * @param containerImage Docker image name for the container
     */
    public CorazaContainerManager(String rulesDirectory, String containerImage) {
        // The server reads /app/rules/main.conf; @validateSchema paths like
        // "rules/schema.json" resolve relative to its /app working directory.
        this.container = new GenericContainer<>(DockerImageName.parse(containerImage))
                .withExposedPorts(CORAZA_PORT)
                .withFileSystemBind(rulesDirectory, "/app/rules")
                // A WAF that answers 403 at / (undefined endpoint, default deny) is healthy.
                .waitingFor(Wait.forHttp("/").forStatusCode(200).forStatusCode(403));
    }

    /**
     * Starts the Coraza container.
     *
     * @return The base URL for accessing the container
     */
    @Override
    public String start() {
        container.start();
        return getBaseUrl();
    }

    /**
     * Stops the Coraza container.
     */
    @Override
    public void stop() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    /**
     * Gets the base URL for accessing the container.
     *
     * @return The base URL in the format http://host:port
     */
    @Override
    public String getBaseUrl() {
        return String.format("http://%s:%d", container.getHost(), container.getMappedPort(CORAZA_PORT));
    }

    /**
     * Gets the mapped port for the Coraza container.
     *
     * @return The mapped port
     */
    public int getMappedPort() {
        return container.getMappedPort(CORAZA_PORT);
    }

    /**
     * Gets the host for the Coraza container.
     *
     * @return The host name or IP address
     */
    public String getHost() {
        return container.getHost();
    }
}
