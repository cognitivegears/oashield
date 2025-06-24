package com.oashield.openapi.integration.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RequestBodyTemplate.
 * <p>
 * Tests include:
 * - Variable substitution in templates
 * - Parameter management and immutability of parameter view
 * - Thread safety of parameter operations
 * - Edge cases with no variables and multiple variables
 * </p>
 */
class RequestBodyTemplateTest {

    private RequestBodyTemplate template;

    @BeforeEach
    void setUp() {
        template = new RequestBodyTemplate("Hello ${name}!");
    }

    @Test
    void testProcessWithVariables() {
        template.setParameter("name", "World");
        String result = template.process();
        assertEquals("Hello World!", result);
    }

    @Test
    void testGetParameterAndGetParameters() {
        template.setParameter("key", "value");
        assertEquals("value", template.getParameter("key"));
        Map<String, String> params = template.getParameters();
        assertEquals(1, params.size());
        assertThrows(UnsupportedOperationException.class, () -> params.put("new", "val"));
    }

    @Test
    void testProcessNoVariables() {
        RequestBodyTemplate t = new RequestBodyTemplate("static content");
        assertFalse(t.hasVariables());
        assertTrue(t.getRequiredVariables().isEmpty());
        assertEquals("static content", t.process());
    }

    @Test
    void testGetRequiredVariablesAndHasVariables() {
        RequestBodyTemplate t = new RequestBodyTemplate("A${x}B${y}C");
        Set<String> vars = t.getRequiredVariables();
        assertEquals(Set.of("x", "y"), vars);
        assertTrue(t.hasVariables());
    }

    @Test
    void testThreadSafetyOfParameters() throws InterruptedException {
        RequestBodyTemplate t = new RequestBodyTemplate("dummy");
        int count = 10;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < count; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                t.setParameter("k" + idx, "v" + idx);
            });
        }
        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        Map<String, String> resultParams = t.getParameters();
        assertEquals(count, resultParams.size());
        for (int i = 0; i < count; i++) {
            assertEquals("v" + i, resultParams.get("k" + i));
        }
    }
}
