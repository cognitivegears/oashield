package com.oashield.openapi.generators.modsecurity3.tests;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the escapeQuotationMark method in Modsecurity3Generator
 * focusing on ModSecurity rule syntax and potentially problematic escaping cases.
 */
public class EscapeQuotationMarkTest {

    private Modsecurity3Generator generator;

    @BeforeEach
    public void setup() {
        generator = new Modsecurity3Generator();
    }

    @Test
    public void testEscapeQuotationMark_nullInput() {
        // Test null input returns null (no NPE)
        assertNull(generator.escapeQuotationMark(null));
    }

    @Test
    public void testEscapeQuotationMark_emptyString() {
        // Test empty string returns empty string
        assertEquals("", generator.escapeQuotationMark(""));
    }

    @Test
    public void testEscapeQuotationMark_noSpecialChars() {
        // Test string without special characters remains unchanged
        String input = "normalstring123";
        assertEquals(input, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_doubleQuotes() {
        // Test escaping double quotes
        String input = "text with \"quotes\" in it";
        String expected = "text with \\\"quotes\\\" in it";
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_singleQuotes() {
        // Test escaping single quotes
        String input = "text with 'quotes' in it";
        String expected = "text with \\'quotes\\' in it";
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_backslashes() {
        // Test escaping backslashes
        String input = "text with \\ backslash";
        String expected = "text with \\\\ backslash";
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_multipleBackslashes() {
        // Test escaping multiple backslashes
        String input = "text with \\\\ multiple backslashes";
        String expected = "text with \\\\\\\\ multiple backslashes";
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_mixedQuotes() {
        // Test escaping both single and double quotes
        String input = "text with \"double\" and 'single' quotes";
        String expected = "text with \\\"double\\\" and \\'single\\' quotes";
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_regexPattern() {
        // Test escaping a common regex pattern
        String input = "^[a-z0-9]+$";
        String expected = "^[a-z0-9]+$";  // Should remain unchanged
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_modsecurityRuleFragment() {
        // Test escaping within a ModSecurity rule context
        String input = "!@rx \"pattern\"";
        String expected = "!@rx \\\"pattern\\\"";
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_modsecuritySpecialChars() {
        // Test with special characters that have meaning in ModSecurity
        String input = "param=value&x=y|z@rx %{REQUEST_URI}";
        String expected = "param=value&x=y|z@rx %{REQUEST_URI}";  // Should remain unchanged
        assertEquals(expected, generator.escapeQuotationMark(input));
    }

    @Test
    public void testEscapeQuotationMark_pathWithQuotes() {
        // Test escaping an API path containing quotes
        String input = "/api/users/\"admin\"/profile";
        String expected = "/api/users/\\\"admin\\\"/profile";
        assertEquals(expected, generator.escapeQuotationMark(input));
    }
}
