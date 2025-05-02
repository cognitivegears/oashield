package com.oashield.openapi.generators.modsecurity3.tests;

import com.oashield.openapi.generators.modsecurity3.Modsecurity3Generator;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenParameter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for pattern formatting functionality
 */
public class PatternFormattingTest {

    @Test
    public void testAllowMultiple_boolean() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.setIsBoolean(true);
        assertFalse(generator.allowMultiple(param));
    }

    @Test
    public void testAllowMultiple_decimal() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDecimal = true;
        assertFalse(generator.allowMultiple(param));
    }

    @Test
    public void testAllowMultiple_date() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDate = true;
        assertFalse(generator.allowMultiple(param));
    }

    @Test
    public void testAllowMultiple_dateTime() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isDateTime = true;
        assertFalse(generator.allowMultiple(param));
    }

    @Test
    public void testAllowMultiple_enum() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isEnum = true;
        assertFalse(generator.allowMultiple(param));
    }

    @Test
    public void testAllowMultiple_string() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isString = true;
        assertTrue(generator.allowMultiple(param));
    }

    @Test
    public void testGetMinLengthPatternString_requiredWithMinLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.required = true;
        param.setMinLength(5);
        assertEquals("5", generator.getMinLengthPatternString(param, true));
    }

    @Test
    public void testGetMinLengthPatternString_requiredWithoutMinLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.required = true;
        assertEquals("1", generator.getMinLengthPatternString(param, true));
    }

    @Test
    public void testGetMinLengthPatternString_notRequiredWithMinLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.required = false;
        param.setMinLength(5);
        assertEquals("5", generator.getMinLengthPatternString(param, false));
    }

    @Test
    public void testGetMinLengthPatternString_notRequiredWithoutMinLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.required = false;
        assertEquals("", generator.getMinLengthPatternString(param, false));
    }

    @Test
    public void testGetMaxLengthPatternString_withMaxLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.setMaxLength(10);
        assertEquals("10", generator.getMaxLengthPatternString(param, false));
    }

    @Test
    public void testGetMaxLengthPatternString_withoutMaxLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        assertEquals("", generator.getMaxLengthPatternString(param, false));
    }

    @Test
    public void testGetMaxLengthPatternString_integerType() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isInteger = true;
        assertEquals("10", generator.getMaxLengthPatternString(param, false));
    }

    @Test
    public void testGetMaxLengthPatternString_longType() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isLong = true;
        assertEquals("19", generator.getMaxLengthPatternString(param, false));
    }

    @Test
    public void testGetMaxLengthPatternString_integerTypeWithLargerMaxLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isInteger = true;
        param.setMaxLength(15);
        assertEquals("10", generator.getMaxLengthPatternString(param, false));
    }

    @Test
    public void testGetMaxLengthPatternString_longTypeWithLargerMaxLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        CodegenParameter param = new CodegenParameter();
        param.isLong = true;
        param.setMaxLength(25);
        assertEquals("19", generator.getMaxLengthPatternString(param, false));
    }

    @Test
    public void testGetDecimalPattern_required() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals("[0-9]{1,10}", generator.getDecimalPattern("[0-9]", "1", "10", true));
    }

    @Test
    public void testGetDecimalPattern_notRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals("[0-9]*.?[0-9]*", generator.getDecimalPattern("[0-9]", "", "", false));
    }

    @Test
    public void testGetNonDecimalPattern_allowMultiple_noMinMax() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals(".+", generator.getNonDecimalPattern(".", "", "", true, true));
        assertEquals(".*", generator.getNonDecimalPattern(".", "", "", true, false));
    }

    @Test
    public void testGetNonDecimalPattern_allowMultiple_minLength1() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals(".+", generator.getNonDecimalPattern(".", "1", "", true, true));
        assertEquals(".+", generator.getNonDecimalPattern(".", "1", "", true, false));
    }

    @Test
    public void testGetNonDecimalPattern_allowMultiple_minAndMaxLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals(".{2,5}", generator.getNonDecimalPattern(".", "2", "5", true, true));
        assertEquals(".{2,5}", generator.getNonDecimalPattern(".", "2", "5", true, false));
    }

    @Test
    public void testGetNonDecimalPattern_allowMultiple_minLengthEqualsMaxLength() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals(".{5}", generator.getNonDecimalPattern(".", "5", "5", true, true));
        assertEquals(".{5}", generator.getNonDecimalPattern(".", "5", "5", true, false));
    }

    @Test
    public void testGetNonDecimalPattern_notAllowMultiple_required() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals("(true|false)", generator.getNonDecimalPattern("(true|false)", "", "", false, true));
    }

    @Test
    public void testGetNonDecimalPattern_notAllowMultiple_notRequired() {
        Modsecurity3Generator generator = new Modsecurity3Generator();
        assertEquals("(true|false)?", generator.getNonDecimalPattern("(true|false)", "", "", false, false));
    }
}