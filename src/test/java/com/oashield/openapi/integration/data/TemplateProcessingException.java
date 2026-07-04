package com.oashield.openapi.integration.data;

/**
 * Thrown when template processing fails.
 */
public class TemplateProcessingException extends RuntimeException {

    /**
     * Constructs a new TemplateProcessingException with no detail message.
     */
    public TemplateProcessingException() {
        super();
    }

    /**
     * Constructs a new TemplateProcessingException with the specified detail message.
     *
     * @param message the detail message
     */
    public TemplateProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new TemplateProcessingException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public TemplateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new TemplateProcessingException with the specified cause.
     *
     * @param cause the cause
     */
    public TemplateProcessingException(Throwable cause) {
        super(cause);
    }
}
