package com.cloudsimulation.models;

/**
 * Exception thrown when configuration validation fails.
 * Provides detailed error information including line number, field name, invalid value, and reason.
 */
public class ConfigValidationException extends RuntimeException {
    private final Integer lineNumber;
    private final String fieldName;
    private final String invalidValue;
    private final String reason;

    /**
     * Creates a new ConfigValidationException with detailed error information.
     *
     * @param lineNumber   YAML line number where error occurred (0 if not applicable)
     * @param fieldName    Configuration field that failed validation
     * @param invalidValue The invalid value that was provided
     * @param reason       Human-readable explanation of why validation failed
     */
    public ConfigValidationException(Integer lineNumber, String fieldName, String invalidValue, String reason) {
        super(formatMessage(lineNumber, fieldName, invalidValue, reason));
        this.lineNumber = lineNumber;
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.reason = reason;
    }

    private static String formatMessage(Integer lineNumber, String fieldName, String invalidValue, String reason) {
        if (lineNumber != null && lineNumber > 0) {
            return String.format("Configuration error at line %d: Invalid value '%s' for field '%s' - %s",
                    lineNumber, invalidValue, fieldName, reason);
        } else {
            return String.format("Configuration error: Invalid value '%s' for field '%s' - %s",
                    invalidValue, fieldName, reason);
        }
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }

    public String getReason() {
        return reason;
    }
}
