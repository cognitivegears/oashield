package com.oashield.openapi.integration.data;

/**
 * Thrown when an endpoint format is invalid (should be "specName:/endpoint").
 */
public class InvalidEndpointFormatException extends RuntimeException {

    /**
     * Constructs a new InvalidEndpointFormatException with no detail message.
     */
    public InvalidEndpointFormatException() {
        super();
    }

    /**
     * Constructs a new InvalidEndpointFormatException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidEndpointFormatException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidEndpointFormatException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidEndpointFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new InvalidEndpointFormatException with the specified cause.
     *
     * @param cause the cause
     */
    public InvalidEndpointFormatException(Throwable cause) {
        super(cause);
    }
}
