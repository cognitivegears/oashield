package com.oashield.openapi.generators.modsecurity3.tests;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenParameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parameter pattern functionality
 */
public class ParamPatternTest {

    @Test
    public void testGetParamPattern_integerRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isInteger = true;
        param.required = true;
        assertEquals("^[0-9]{1,19}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_integerOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isInteger = true;
        param.required = false;
        assertEquals("^([0-9]{1,19})?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_stringWithMinAndMaxLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isString = true;
        param.required = true;
        param.setMinLength(2);
        param.setMaxLength(5);
        assertEquals("^.{2,5}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_stringWithMinLengthOnly() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isString = true;
        param.required = true;
        param.setMinLength(2);
        assertEquals("^.{2,}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_stringWithMaxLengthOnly() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isString = true;
        param.required = true;
        param.setMaxLength(5);
        assertEquals("^.{1,5}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_booleanRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.setIsBoolean(true);
        param.required = true;
        assertEquals("^(true|false)$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_booleanOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.setIsBoolean(true);
        param.required = false;
        assertEquals("^(true|false)?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_enumRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEnum = true;
        param.required = true;
        Map<String, Object> allowableValues = new HashMap<>();
        allowableValues.put("values", Arrays.asList("valueA", "valueB"));
        param.allowableValues = allowableValues;
        assertEquals("^(valueA|valueB)$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_enumOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEnum = true;
        param.required = false;
        Map<String, Object> allowableValues = new HashMap<>();
        allowableValues.put("values", Arrays.asList("valueA", "valueB"));
        param.allowableValues = allowableValues;
        assertEquals("^(valueA|valueB)?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_decimalRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDecimal = true;
        param.required = true;
        // SECURITY FIX: Now uses bounded quantifiers to prevent ReDoS
        assertEquals("^-?([0-9]{1,15}(\\.[0-9]{1,15})?|\\.[0-9]{1,15})$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_decimalOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDecimal = true;
        param.required = false;
        // SECURITY FIX: Now uses bounded quantifiers to prevent ReDoS
        assertEquals("^-?([0-9]{0,15}(\\.[0-9]{1,15})?|\\.[0-9]{1,15})?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_uuidRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isUuid = true;
        param.required = true;
        assertEquals("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_uuidOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isUuid = true;
        param.required = false;
        assertEquals("^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_dateRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDate = true;
        param.required = true;
        assertEquals("^(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d)))$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_dateTimeRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDateTime = true;
        param.required = true;
        assertEquals("^(\\d{4}-((01|03|05|07|08|10|12)-(0[1-9]|1\\d|2\\d|3[0-1])|(04|06|09|11)-(0[1-9]|1\\d|2\\d|30)|02-(0[1-9]|1\\d|2\\d))T([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)(\\.\\d+)?([Zz]|[+\\-](0\\d|1[0-4])(:[0-5]\\d)?))$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_emailRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEmail = true;
        param.required = true;
        assertEquals("^([A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_defaultTypeRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.required = true;
        assertEquals("^.+$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_defaultTypeOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.required = false;
        assertEquals("^.*$", generator.getParamPattern(param));
    }

    // ===== NEW OpenAPI 3.1 Format Type Tests =====

    @Test
    public void testGetParamPattern_timeFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        // Use vendorExtensions to store format since setFormat() doesn't work
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "time");
        param.required = true;
        assertEquals("^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]{1,9})?(Z|[+-][01][0-9]:[0-5][0-9])?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_timeFormatOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "time");
        param.required = false;
        assertEquals("^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]{1,9})?(Z|[+-][01][0-9]:[0-5][0-9])?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_uriFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "uri");
        param.required = true;
        assertEquals("^[a-zA-Z][a-zA-Z0-9+.-]{0,31}://[^\\s]{1,2000}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_uriReferenceFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "uri-reference");
        param.required = true;
        assertEquals("^([a-zA-Z][a-zA-Z0-9+.-]{0,31}://)?[^\\s]{1,2000}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_uriTemplateFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "uri-template");
        param.required = true;
        assertEquals("^[^\\s{]{0,1000}(\\{[^}]{1,100}\\}[^\\s{]{0,1000}){0,50}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_hostnameFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "hostname");
        param.required = true;
        assertEquals("^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.){0,126}[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_ipv4FormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "ipv4");
        param.required = true;
        assertEquals("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_ipv6FormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "ipv6");
        param.required = true;
        assertEquals("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_jsonPointerFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "json-pointer");
        param.required = true;
        assertEquals("^(/(([^/~]|~[01]){0,100})){0,50}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_relativeJsonPointerFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "relative-json-pointer");
        param.required = true;
        assertEquals("^[0-9]{1,10}(#|(/(([^/~]|~[01]){0,100})){0,50})$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_byteFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "byte");
        param.required = true;
        assertEquals("^[A-Za-z0-9+/]{0,1000}={0,2}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_binaryFormatRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.vendorExtensions = new HashMap<>();
        param.vendorExtensions.put("x-format", "binary");
        param.required = true;
        assertEquals("^[0-9a-fA-F]{0,10000}$", generator.getParamPattern(param));
    }
}
