package org.polaris2023.gtu.space.portal;

/**
 * Represents the result of validating a dimension transition.
 * <p>
 * Contains a validity flag and, if invalid, an error code and message
 * describing why the validation failed.
 * </p>
 *
 * @param valid        Whether the validation passed
 * @param errorCode    A machine-readable error code, or null if valid
 * @param errorMessage A human-readable error message, or null if valid
 *
 * @see ITransitionManager#validateTransition
 */
public record ValidationResult(
        boolean valid,
        String errorCode,
        String errorMessage
) {

    /**
     * Creates a successful validation result.
     *
     * @return A ValidationResult indicating success
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null, null);
    }

    /**
     * Creates a failed validation result with the specified error.
     *
     * @param errorCode    A machine-readable error code (e.g., "DESTINATION_UNAVAILABLE")
     * @param errorMessage A human-readable error message
     * @return A ValidationResult indicating failure with the given error
     */
    public static ValidationResult failure(String errorCode, String errorMessage) {
        return new ValidationResult(false, errorCode, errorMessage);
    }

    /**
     * Creates a failed validation result with an error code only.
     *
     * @param errorCode A machine-readable error code
     * @return A ValidationResult indicating failure with the error code as the message
     */
    public static ValidationResult failure(String errorCode) {
        return new ValidationResult(false, errorCode, errorCode);
    }

    /**
     * Checks if the validation failed.
     *
     * @return true if the validation failed
     */
    public boolean isFailure() {
        return !valid;
    }

    /**
     * Throws an exception if this result represents a failure.
     *
     * @throws TransitionValidationException if the validation failed
     */
    public void throwIfFailure() throws TransitionValidationException {
        if (!valid) {
            throw new TransitionValidationException(errorCode, errorMessage);
        }
    }
}
