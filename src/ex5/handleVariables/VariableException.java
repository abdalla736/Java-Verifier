package ex5.handleVariables;

/**
 * Exception thrown when a variable-related syntax or semantic error is detected
 * during S-Java source code validation.
 *
 * <p>{@code VariableException} is used to signal illegal variable declarations
 * or assignments, including but not limited to:</p>
 *
 * <ul>
 *   <li>Invalid declaration or assignment syntax</li>
 *   <li>Duplicate variable declarations (global or local)</li>
 *   <li>Assignment to undeclared variables</li>
 *   <li>Reassignment of {@code final} variables</li>
 *   <li>Use of uninitialized variables</li>
 *   <li>Type incompatibility in assignments</li>
 * </ul>
 *
 * <p>This exception is propagated up to the main parser and eventually handled
 * by {@code Sjavac}, which converts it into a syntax error output code.</p>
 */
public class VariableException extends Exception {

    /**
     * Constructs a new {@code VariableException} with a detailed error message.
     *
     * @param message a human-readable description of the variable-related error
     */
    public VariableException(String message) {
        super(message);
    }
}
