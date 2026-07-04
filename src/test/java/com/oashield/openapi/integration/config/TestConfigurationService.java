package com.oashield.openapi.integration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Centralized configuration service for integration tests.
 * Provides thread-safe singleton access to test configuration properties,
 * environment detection, and utility methods for test setup.
 */
public class TestConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(TestConfigurationService.class);
    private static volatile TestConfigurationService instance;

    private TestConfigurationService() {
        logger.info("Initializing TestConfigurationService: GitHubActions={}, ARM={}, DockerAvailable={}",
                isRunningInGithubActions(), isArmArchitecture(), isDockerAvailable());
    }

    /**
     * Returns the singleton instance of TestConfigurationService.
     *
     * @return singleton instance
     */
    public static TestConfigurationService getInstance() {
        if (instance == null) {
            synchronized (TestConfigurationService.class) {
                if (instance == null) {
                    instance = new TestConfigurationService();
                }
            }
        }
        return instance;
    }

    /**
     * Whether actual HTTP calls should be skipped.
     *
     * @return true if skip.http.calls or skipActualHttpCalls is set to true
     */
    public boolean isHttpCallsSkipped() {
        String val = System.getProperty("skip.http.calls",
                System.getProperty("skipActualHttpCalls", "false"));
        boolean skip = Boolean.parseBoolean(val);
        logger.debug("isHttpCallsSkipped: {}", skip);
        return skip;
    }

    /**
     * Whether strict validation should be skipped.
     *
     * @return true if skip.strict.validation is set to true
     */
    public boolean isStrictValidationSkipped() {
        boolean skip = Boolean.parseBoolean(System.getProperty("skip.strict.validation", "false"));
        logger.debug("isStrictValidationSkipped: {}", skip);
        return skip;
    }

    /**
     * Container image to use for Coraza server.
     *
     * @return container image name
     */
    public String getContainerImage() {
        String defaultImage = "ghcr.io/cognitivegears/coraza-validate-server:latest";
        String image = System.getProperty("container.image", defaultImage);
        logger.debug("getContainerImage: {}", image);
        return image;
    }

    /**
     * Test execution timeout in milliseconds.
     *
     * @return timeout in ms
     */
    public long getTestTimeout() {
        String timeoutStr = System.getProperty("test.timeout", "30000");
        long timeout;
        try {
            timeout = Long.parseLong(timeoutStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid test.timeout '{}', using default 30000", timeoutStr);
            timeout = 30000L;
        }
        logger.debug("getTestTimeout: {}", timeout);
        return timeout;
    }

    /**
     * Whether parallel test execution is enabled.
     *
     * @return true if parallel.execution is true
     */
    public boolean isParallelExecutionEnabled() {
        boolean enabled = Boolean.parseBoolean(System.getProperty("parallel.execution", "true"));
        logger.debug("isParallelExecutionEnabled: {}", enabled);
        return enabled;
    }

    /**
     * Directory for test data files.
     *
     * @return test data directory path
     */
    public String getTestDataDirectory() {
        String defaultDir = System.getProperty("user.dir") + "/src/test/resources";
        String dir = System.getProperty("test.data.directory", defaultDir);
        logger.debug("getTestDataDirectory: {}", dir);
        return dir;
    }

    /**
     * Creates a unique output directory for a test run.
     *
     * @return absolute path to the created directory
     */
    public String createUniqueOutputDirectory() {
        String base = System.getProperty("output.directory.base",
                System.getProperty("java.io.tmpdir"));
        String unique = base.endsWith("/") ? base + "oashield-test-" + UUID.randomUUID()
                : base + "/oashield-test-" + UUID.randomUUID();
        Path path = Paths.get(unique);
        try {
            Files.createDirectories(path);
            logger.debug("Created output directory: {}", unique);
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", unique, e);
        }
        return unique;
    }

    /**
     * Detect if running in GitHub Actions environment.
     *
     * @return true if GITHUB_ACTIONS env var is 'true'
     */
    public boolean isRunningInGithubActions() {
        boolean flag = "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"));
        logger.debug("isRunningInGithubActions: {}", flag);
        return flag;
    }

    /**
     * Detect if running on ARM architecture (e.g., M1/M2 Macs).
     *
     * @return true if os.arch contains 'arm' or 'aarch64'
     */
    public boolean isArmArchitecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        boolean isArm = arch.contains("arm") || arch.contains("aarch64");
        logger.debug("isArmArchitecture: {}", isArm);
        return isArm;
    }

    /**
     * Check if Docker is available on the system.
     *
     * @return true if 'docker info' command executes successfully
     */
    public boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            int exit = process.exitValue();
            boolean available = finished && exit == 0;
            logger.debug("isDockerAvailable: {}", available);
            return available;
        } catch (Exception e) {
            logger.debug("isDockerAvailable: false due to exception", e);
            return false;
        }
    }
}
