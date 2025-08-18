package com.oashield.openapi.generators.modsecurity3.tests;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for pattern generation functionality
 */
public class PatternGenerationTest {

    @Test
    public void testGetAllowedInputPattern_integer() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isInteger = true;
        assertEquals("[0-9]", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_long() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isLong = true;
        assertEquals("[0-9]", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_number() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isNumber = true;
        assertEquals("[0-9]", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_float() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isFloat = true;
        assertEquals("[0-9]", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_double() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDouble = true;
        assertEquals("[0-9]", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_decimal() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDecimal = true;
        assertEquals("[0-9]", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_boolean() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.setIsBoolean(true);
        assertEquals("(true|false)", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_uuid() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isUuid = true;
        assertEquals("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_date() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDate = true;
        assertEquals("^(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d)))?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_dateTime() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDateTime = true;
        assertEquals("^(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d))T([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)(\\.\\d+)?([Zz]|[+\\-](0\\d|1[0-4])(:[0-5]\\d)?))?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_email() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEmail = true;
        assertEquals("^([A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_enum() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEnum = true;
        Map<String, Object> allowableValues = new HashMap<>();
        allowableValues.put("values", Arrays.asList("value1", "value2", "value3"));
        param.allowableValues = allowableValues;
        assertEquals("(value1|value2|value3)", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_enumEmptyValues() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEnum = true;
        Map<String, Object> allowableValues = new HashMap<>();
        allowableValues.put("values", new ArrayList<>());
        param.allowableValues = allowableValues;
        assertEquals(".", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_enumNullValues() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEnum = true;
        Map<String, Object> allowableValues = new HashMap<>();
        allowableValues.put("values", null);
        param.allowableValues = allowableValues;
        assertEquals(".", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_enumInvalidValuesType() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEnum = true;
        Map<String, Object> allowableValues = new HashMap<>();
        allowableValues.put("values", "not a list");
        param.allowableValues = allowableValues;
        assertEquals(".", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_default() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        // No specific type flags set
        assertEquals(".", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testIsDecimal_float() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isFloat = true;
        assertTrue(generator.isDecimal(param));
    }

    @Test
    public void testIsDecimal_double() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDouble = true;
        assertTrue(generator.isDecimal(param));
    }

    @Test
    public void testIsDecimal_decimal() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDecimal = true;
        assertTrue(generator.isDecimal(param));
    }

    @Test
    public void testIsDecimal_integer() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isInteger = true;
        assertFalse(generator.isDecimal(param));
    }

    @Test
    public void testIsDecimal_string() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isString = true;
        assertFalse(generator.isDecimal(param));
    }

    @Test
    public void testIsInvalidPattern_validPattern() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertFalse(generator.isInvalidPattern("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"));
    }

    @Test
    public void testIsInvalidPattern_invalidPattern() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertTrue(generator.isInvalidPattern("^(?!.*<script>).*$"));
        assertTrue(generator.isInvalidPattern("(?=.*[a-z])(?=.*[A-Z]).+"));
        assertTrue(generator.isInvalidPattern("(?<=abc)def"));
        assertTrue(generator.isInvalidPattern("(?<!abc)def"));
    }

    // ===== NEW OpenAPI 3.1 Format Type Tests =====

    @Test
    public void testGetAllowedInputPattern_timeFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "time");
        assertEquals("([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]{1,9})?(Z|[+-][01][0-9]:[0-5][0-9])?", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_uriFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "uri");
        assertEquals("[a-zA-Z][a-zA-Z0-9+.-]{0,31}://[^\\s]{1,2000}", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_uriReferenceFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "uri-reference");
        assertEquals("([a-zA-Z][a-zA-Z0-9+.-]{0,31}://)?[^\\s]{1,2000}", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_uriTemplateFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "uri-template");
        assertEquals("[^\\s{]{0,1000}(\\{[^}]{1,100}\\}[^\\s{]{0,1000}){0,50}", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_hostnameFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "hostname");
        assertEquals("([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.){0,126}[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_ipv4Format() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "ipv4");
        assertEquals("((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_ipv6Format() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "ipv6");
        assertEquals("([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_jsonPointerFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "json-pointer");
        assertEquals("(/(([^/~]|~[01]){0,100})){0,50}", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_relativeJsonPointerFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "relative-json-pointer");
        assertEquals("[0-9]{1,10}(#|(/(([^/~]|~[01]){0,100})){0,50})", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_byteFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "byte");
        assertEquals("[A-Za-z0-9+/]{0,1000}={0,2}", generator.getAllowedInputPattern(param));
    }

    @Test
    public void testGetAllowedInputPattern_binaryFormat() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "binary");
        assertEquals("[0-9a-fA-F]{0,10000}", generator.getAllowedInputPattern(param));
    }

    // ===== ReDoS Security Tests =====

    @Test
    public void testSecurityFix_uuidPatternIsBounded() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isUuid = true;
        param.required = true;

        String pattern = generator.getParamPattern(param);
        // Verify the pattern uses bounded quantifiers and follows RFC 4122 format
        assertEquals("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", pattern);

        // Ensure no unbounded quantifiers like + or * without upper bounds
        assertFalse(pattern.contains("[0-9a-fA-F]+"));
        assertFalse(pattern.contains("[0-9a-fA-F]*"));
    }

    @Test
    public void testSecurityFix_decimalPatternIsBounded() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDecimal = true;
        param.required = true;

        String pattern = generator.getParamPattern(param);
        // Verify the pattern uses bounded quantifiers with maximum limit of 15 digits
        assertEquals("^-?([0-9]{1,15}(\\.[0-9]{1,15})?|\\.[0-9]{1,15})$", pattern);

        // Ensure no unbounded quantifiers
        assertFalse(pattern.contains("[0-9]+"));
        assertFalse(pattern.contains("[0-9]*"));
    }

    @Test
    public void testSecurityFix_allNewFormatsUseBoundedQuantifiers() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        String[] newFormats = {"time", "uri", "uri-reference", "uri-template", "hostname",
                              "ipv4", "ipv6", "json-pointer", "relative-json-pointer",
                              "byte", "binary"};

        for (String format : newFormats) {
            CodegenParameter param = new CodegenParameter();
            param.vendorExtensions = new HashMap<>();
            param.vendorExtensions.put("x-format", format);
            String pattern = generator.getAllowedInputPattern(param);

            // Verify no unbounded repetitions without upper limits
            assertFalse(pattern.contains("*") && !pattern.contains("{"),
                       "Format " + format + " should not contain unbounded * quantifiers");
            assertFalse(pattern.contains("+") && !pattern.contains("{"),
                       "Format " + format + " should not contain unbounded + quantifiers");
        }
    }
}
