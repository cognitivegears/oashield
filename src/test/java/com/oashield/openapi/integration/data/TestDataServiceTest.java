/**
 * Unit tests for TestDataService.
 * <p>
 * Tests include:
 * - Singleton pattern thread safety
 * - Endpoint parsing via private methods
 * - Valid and invalid request body template loading
 * - OpenAPI spec path resolution
 * - Error handling for invalid inputs
 * - Caching behavior verification
 * - Integration with TestConfigurationService
 * </p>
 */
package com.oashield.openapi.integration.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.oashield.openapi.integration.config.TestConfigurationService;

class TestDataServiceTest {

    private TestDataService service;

    @BeforeEach
    void setUp() {
        // Point TestConfigurationService to test resources directory
        System.setProperty("test.data.directory",
                Paths.get("src", "test", "resources").toAbsolutePath().toString());
        service = TestDataService.getInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Reset TestDataService singleton for isolation
        Field instField = TestDataService.class.getDeclaredField("instance");
        instField.setAccessible(true);
        instField.set(null, null);
        // Reset TestConfigurationService singleton
        Field configInst = TestConfigurationService.class.getDeclaredField("instance");
        configInst.setAccessible(true);
        configInst.set(null, null);
    }

    @Test
    void testSingletonThreadSafety() throws InterruptedException {
        int threadCount = 10;
        List<TestDataService> instances = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    latch.await();
                    instances.add(TestDataService.getInstance());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        latch.countDown();
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(threadCount, instances.size());
        TestDataService first = instances.get(0);
        for (TestDataService inst : instances) {
            assertSame(first, inst);
        }
    }

    @Test
    void testParseSpecNameValid() throws Exception {
        Method method = TestDataService.class.getDeclaredMethod("parseSpecName", String.class);
        method.setAccessible(true);
        assertEquals("petstore", method.invoke(service, "petstore:/pet"));
        assertEquals("getparam", method.invoke(service, "getparam:/pet/findByStatus"));
    }

    @Test
    void testParseSpecNameInvalidFormats() throws Exception {
        Method method = TestDataService.class.getDeclaredMethod("parseSpecName", String.class);
        method.setAccessible(true);
        String[] invalids = {null, "invalidformat", ":/path", " :/path"};
        for (String input : invalids) {
            InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                    () -> method.invoke(service, input));
            assertTrue(ex.getCause() instanceof InvalidEndpointFormatException);
        }
    }

    @Test
    void testParseEndpointPathValid() throws Exception {
        Method method = TestDataService.class.getDeclaredMethod("parseEndpointPath", String.class);
        method.setAccessible(true);
        assertEquals("pet", method.invoke(service, "petstore:/pet"));
        assertEquals("pet/findByStatus",
                method.invoke(service, "getparam:/pet/findByStatus"));
    }

    @Test
    void testParseEndpointPathInvalidFormats() throws Exception {
        Method method = TestDataService.class.getDeclaredMethod("parseEndpointPath", String.class);
        method.setAccessible(true);
        String[] invalids = {null, "invalidformat", "petstore:path", "petstore:/", " :/"};
        for (String input : invalids) {
            InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                    () -> method.invoke(service, input));
            assertTrue(ex.getCause() instanceof InvalidEndpointFormatException);
        }
    }

    @Test
    void testGetValidRequestBody() throws Exception {
        String result = service.getValidRequestBody("petstore:/pet");
        String testDataDir = TestConfigurationService.getInstance().getTestDataDirectory();
        Path path = Paths.get(testDataDir, "test-data", "petstore", "pet", "valid.json");
        String expected = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    @Test
    void testGetInvalidRequestBody() {
        String result = service.getInvalidRequestBody("petstore:/pet", "missing_required");
        String testDataDir = TestConfigurationService.getInstance().getTestDataDirectory();
        Path path = Paths.get(testDataDir, "test-data", "petstore", "pet", "invalid",
                "missing_required.json");
        try {
            String expected = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            assertEquals(expected, result);
        } catch (Exception e) {
            fail("Failed to read expected invalid template: " + e.getMessage());
        }
    }

    @Test
    void testTemplateNotFound() {
        assertThrows(TemplateNotFoundException.class,
                () -> service.getValidRequestBody("petstore:/nonexistent"));
        assertThrows(TemplateNotFoundException.class,
                () -> service.getInvalidRequestBody("petstore:/pet", "nonexistent"));
    }

    @Test
    void testGetOpenApiSpecPath() {
        String petPath = service.getOpenApiSpecPath("petstore");
        assertEquals(Paths.get("samples", "petstore.yaml").toAbsolutePath().toString(), petPath);
        String urlPath = service.getOpenApiSpecPath("urlintparam");
        assertEquals(Paths.get("samples", "urlintparam.yaml").toAbsolutePath().toString(), urlPath);
        String getparamPath = service.getOpenApiSpecPath("getparam");
        assertEquals(Paths.get("samples", "getparam.yaml").toAbsolutePath().toString(), getparamPath);
    }

    @Test
    void testGetOpenApiSpecPathErrors() {
        assertThrows(SpecificationNotFoundException.class, () -> service.getOpenApiSpecPath(null));
        assertThrows(SpecificationNotFoundException.class, () -> service.getOpenApiSpecPath(""));
        assertThrows(SpecificationNotFoundException.class,
                () -> service.getOpenApiSpecPath("nonexistent"));
    }

    @Test
    void testCachingBehavior() throws Exception {
        Field cacheField = TestDataService.class.getDeclaredField("templateCache");
        cacheField.setAccessible(true);
        Map<?, ?> cache = (Map<?, ?>) cacheField.get(service);
        cache.clear();
        assertTrue(cache.isEmpty());
        service.getValidRequestBody("petstore:/pet");
        assertEquals(1, cache.size());
        service.getValidRequestBody("petstore:/pet");
        assertEquals(1, cache.size());
    }

    @Test
    void testIntegrationWithConfigService() throws Exception {
        Field field = TestDataService.class.getDeclaredField("configService");
        field.setAccessible(true);
        Object configInService = field.get(service);
        assertSame(TestConfigurationService.getInstance(), configInService);
    }
}
