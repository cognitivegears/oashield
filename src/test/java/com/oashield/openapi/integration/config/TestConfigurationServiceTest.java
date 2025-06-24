package com.oashield.openapi.integration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TestConfigurationService}.
 */
public class TestConfigurationServiceTest {

    private TestConfigurationService configService;

    @BeforeEach
    void setUp() {
        // Clear properties before each test
        System.clearProperty("skip.http.calls");
        System.clearProperty("skipActualHttpCalls");
        System.clearProperty("skip.strict.validation");
        System.clearProperty("container.image");
        System.clearProperty("test.timeout");
        System.clearProperty("parallel.execution");
        System.clearProperty("test.data.directory");
        System.clearProperty("output.directory.base");

        configService = TestConfigurationService.getInstance();
    }

    @Test
    void testSingleton() {
        TestConfigurationService instance1 = TestConfigurationService.getInstance();
        TestConfigurationService instance2 = TestConfigurationService.getInstance();
        assertSame(instance1, instance2, "getInstance should return the same singleton instance");
    }

    @Test
    void testHttpCallsSkippedDefault() {
        assertFalse(configService.isHttpCallsSkipped(), "HTTP calls should not be skipped by default");
    }

    @Test
    void testHttpCallsSkippedProperty() {
        System.setProperty("skip.http.calls", "true");
        assertTrue(configService.isHttpCallsSkipped(), "HTTP calls should be skipped when skip.http.calls=true");
        System.clearProperty("skip.http.calls");
        System.setProperty("skipActualHttpCalls", "true");
        assertTrue(configService.isHttpCallsSkipped(), "HTTP calls should be skipped when skipActualHttpCalls=true");
    }

    @Test
    void testStrictValidationSkippedDefault() {
        assertFalse(configService.isStrictValidationSkipped(), "Strict validation should not be skipped by default");
    }

    @Test
    void testStrictValidationSkippedProperty() {
        System.setProperty("skip.strict.validation", "true");
        assertTrue(configService.isStrictValidationSkipped(), "Strict validation should be skipped when skip.strict.validation=true");
    }

    @Test
    void testGetContainerImageDefault() {
        String expected = "ghcr.io/cognitivegears/coraza-validate-server:latest";
        assertEquals(expected, configService.getContainerImage(), "Default container image should be used");
    }

    @Test
    void testGetContainerImageOverride() {
        System.setProperty("container.image", "custom/image:tag");
        assertEquals("custom/image:tag", configService.getContainerImage(), "Container image should reflect system property override");
    }

    @Test
    void testGetTestTimeoutDefault() {
        assertEquals(30000L, configService.getTestTimeout(), "Default test timeout should be 30000ms");
    }

    @Test
    void testGetTestTimeoutOverride() {
        System.setProperty("test.timeout", "5000");
        assertEquals(5000L, configService.getTestTimeout(), "Test timeout should reflect system property override");
    }

    @Test
    void testGetTestTimeoutInvalid() {
        System.setProperty("test.timeout", "invalid");
        assertEquals(30000L, configService.getTestTimeout(), "Invalid timeout should fall back to default");
    }

    @Test
    void testParallelExecutionEnabledDefault() {
        assertTrue(configService.isParallelExecutionEnabled(), "Parallel execution should be enabled by default");
    }

    @Test
    void testParallelExecutionOverride() {
        System.setProperty("parallel.execution", "false");
        assertFalse(configService.isParallelExecutionEnabled(), "Parallel execution should reflect system property override");
    }

    @Test
    void testGetTestDataDirectoryDefault() {
        String expected = System.getProperty("user.dir") + "/src/test/resources";
        assertEquals(expected, configService.getTestDataDirectory(), "Default test data directory should be under src/test/resources");
    }

    @Test
    void testGetTestDataDirectoryOverride() {
        System.setProperty("test.data.directory", "/tmp/data");
        assertEquals("/tmp/data", configService.getTestDataDirectory(), "Test data directory should reflect system property override");
    }

    @Test
    void testCreateUniqueOutputDirectory() throws Exception {
        System.clearProperty("output.directory.base");
        String dir1 = configService.createUniqueOutputDirectory();
        String dir2 = configService.createUniqueOutputDirectory();
        Path path1 = Paths.get(dir1);
        Path path2 = Paths.get(dir2);
        assertTrue(Files.exists(path1), "First output directory should exist");
        assertTrue(Files.exists(path2), "Second output directory should exist");
        assertNotEquals(path1.toAbsolutePath(), path2.toAbsolutePath(), "Unique directories should be different");
        // Clean up created directories
        Files.walk(path1).sorted((a, b) -> -a.compareTo(b)).map(Path::toFile).forEach(File::delete);
        Files.walk(path2).sorted((a, b) -> -a.compareTo(b)).map(Path::toFile).forEach(File::delete);
    }
}
