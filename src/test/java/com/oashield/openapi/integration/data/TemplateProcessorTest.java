package com.oashield.openapi.integration.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for TemplateProcessor utility.
 * <p>
 * Tests include:
 * - processTemplate with valid and invalid inputs
 * - extractVariables and hasVariables behaviors
 * - validateTemplate success and error scenarios
 * </p>
 */
class TemplateProcessorTest {

    @Test
    void testProcessTemplateValid() {
        Map<String, String> params = new HashMap<>();
        params.put("a", "1");
        params.put("b", "2");
        String result = TemplateProcessor.processTemplate("x${a}y${b}z", params);
        assertEquals("x1y2z", result);
    }

    @Test
    void testProcessTemplateNullTemplate() {
        Map<String, String> params = new HashMap<>();
        params.put("a", "1");
        TemplateProcessingException ex = assertThrows(TemplateProcessingException.class,
                () -> TemplateProcessor.processTemplate(null, params));
        assertEquals("Template cannot be null", ex.getMessage());
    }

    @Test
    void testProcessTemplateNullParameters() {
        TemplateProcessingException ex = assertThrows(TemplateProcessingException.class,
                () -> TemplateProcessor.processTemplate("template", null));
        assertEquals("Parameters map cannot be null", ex.getMessage());
    }

    @Test
    void testExtractVariables() {
        Set<String> vars = TemplateProcessor.extractVariables("${x}${y}${x}");
        assertEquals(Set.of("x", "y"), vars);
    }

    @Test
    void testExtractVariablesNullTemplate() {
        Set<String> vars = TemplateProcessor.extractVariables(null);
        assertTrue(vars.isEmpty());
    }

    @Test
    void testHasVariables() {
        assertTrue(TemplateProcessor.hasVariables("hello ${var}"));
        assertFalse(TemplateProcessor.hasVariables("hello"));
        assertFalse(TemplateProcessor.hasVariables(null));
    }

    @Test
    void testValidateTemplateValid() {
        Map<String, String> params = Map.of("v", "x");
        assertDoesNotThrow(() -> TemplateProcessor.validateTemplate("${v}", params));
    }

    @Test
    void testValidateTemplateMissing() {
        Map<String, String> params = Map.of();
        TemplateProcessingException ex = assertThrows(TemplateProcessingException.class,
                () -> TemplateProcessor.validateTemplate("${v}", params));
        assertTrue(ex.getMessage().contains("Missing template parameters"));
    }

    @Test
    void testValidateTemplateNullTemplate() {
        TemplateProcessingException ex = assertThrows(TemplateProcessingException.class,
                () -> TemplateProcessor.validateTemplate(null, Map.of()));
        assertEquals("Template cannot be null", ex.getMessage());
    }

    @Test
    void testValidateTemplateNullParameters() {
        TemplateProcessingException ex = assertThrows(TemplateProcessingException.class,
                () -> TemplateProcessor.validateTemplate("template", null));
        assertEquals("Parameters map cannot be null", ex.getMessage());
    }
}
