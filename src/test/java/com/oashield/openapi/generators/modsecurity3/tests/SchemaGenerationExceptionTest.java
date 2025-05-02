package com.oashield.openapi.generators.modsecurity3.tests;

import com.oashield.openapi.generators.modsecurity3.JsonSchemaGenerator.SchemaGenerationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the SchemaGenerationException class.
 */
public class SchemaGenerationExceptionTest {

    @Test
    public void testSchemaGenerationExceptionWithMessage() {
        String message = "Test error message";
        SchemaGenerationException exception = new SchemaGenerationException(message);
        
        assertEquals(message, exception.getMessage());
        assertNotNull(exception);
    }

    @Test
    public void testSchemaGenerationExceptionWithMessageAndCause() {
        String message = "Test error message with cause";
        Throwable cause = new RuntimeException("Original cause");
        SchemaGenerationException exception = new SchemaGenerationException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNotNull(exception);
    }
}