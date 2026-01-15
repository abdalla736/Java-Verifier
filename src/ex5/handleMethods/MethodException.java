package ex5.handleMethods;

/**
 * Exception thrown when a method-related syntax or semantic error is detected
 * during S-Java source code validation.
 *
 * <p>{@code MethodException} represents illegal constructs related to methods
 * and control flow, including but not limited to:</p>
 *
 * <ul>
 *   <li>Invalid method declarations or duplicate method names</li>
 *   <li>Illegal statements inside method bodies</li>
 *   <li>Incorrect or missing {@code return;} statements</li>
 *   <li>Invalid {@code if} / {@code while} condition syntax</li>
 *   <li>Calls to undefined methods</li>
 *   <li>Incorrect number or types of arguments in method calls</li>
 * </ul>
 *
 * <p>This exception is propagated to the main parser and handled by
 * {@code Sjavac}, which reports it as a syntax error.</p>
 */
public class MethodException extends Exception {

    /**
     * Constructs a new {@code MethodException} with a detailed error message.
     *
     * @param message a human-readable description of the method-related error
     */
    public MethodException(String message){
        super(message);
    }
}
