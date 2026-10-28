package com.grow.lyy.ai;

/** Raised when the external AI service cannot be reached or returns no usable response. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
