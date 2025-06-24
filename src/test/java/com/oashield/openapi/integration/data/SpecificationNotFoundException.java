package com.oashield.openapi.integration.data;

/**
 * Thrown when a requested specification cannot be found.
 */
public class SpecificationNotFoundException extends RuntimeException {

    /**
     * Constructs a new SpecificationNotFoundException with no detail message.
     */
    public SpecificationNotFoundException() {
        super();
    }

    /**
     * Constructs a new SpecificationNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public SpecificationNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new SpecificationNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public SpecificationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SpecificationNotFoundException with the specified cause.
     *
     * @param cause the cause
     */
    public SpecificationNotFoundException(Throwable cause) {
        super(cause);
    }
}
