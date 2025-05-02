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
        // Assuming default max length for decimal is not set, min length becomes 1 for required
        assertEquals("^[0-9]{1,}$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_decimalOptional() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDecimal = true;
        param.required = false;
        assertEquals("^[0-9]*.?[0-9]*$", generator.getParamPattern(param));
    }

    @Test
    public void testGetParamPattern_uuidRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isUuid = true;
        param.required = true;
        assertEquals("^[0-9a-fA-F]+$", generator.getParamPattern(param));
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
}