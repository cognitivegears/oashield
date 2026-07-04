package com.oashield.openapi.integration.data;

/**
 * Thrown when a requested template cannot be found.
 */
public class TemplateNotFoundException extends RuntimeException {

    /**
     * Constructs a new TemplateNotFoundException with no detail message.
     */
    public TemplateNotFoundException() {
        super();
    }

    /**
     * Constructs a new TemplateNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public TemplateNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new TemplateNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public TemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new TemplateNotFoundException with the specified cause.
     *
     * @param cause the cause
     */
    public TemplateNotFoundException(Throwable cause) {
        super(cause);
    }
}
