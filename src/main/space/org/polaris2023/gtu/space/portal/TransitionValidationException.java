package org.polaris2023.gtu.space.portal;

/**
 * Exception thrown when a dimension transition validation fails.
 * <p>
 * Contains the error code and message from the validation result,
 * providing context for why the transition was rejected.
 * </p>
 *
 * @see ValidationResult
 * @see ITransitionManager
 */
public class TransitionValidationException extends Exception {

    private final String errorCode;

    /**
     * Creates a new transition validation exception.
     *
     * @param errorCode    The machine-readable error code
     * @param errorMessage The human-readable error message
     */
    public TransitionValidationException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    /**
     * Gets the machine-readable error code.
     *
     * @return The error code
     */
    public String errorCode() {
        return errorCode;
    }
}
