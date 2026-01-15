package ex5.handleVariables;

/**
 * Represents a variable in an S-Java program.
 *
 * <p>The {@code Variable} class stores semantic information required by the
 * verifier to validate variable usage. It does not store runtime values and
 * does not execute code.</p>
 *
 * <p>Each variable is characterized by:
 * <ul>
 *   <li>Its declared type</li>
 *   <li>Whether it has been initialized</li>
 *   <li>Whether it is declared {@code final}</li>
 * </ul>
 *
 * <p>This information is used to enforce S-Java rules such as:
 * <ul>
 *   <li>Variables must be declared before use</li>
 *   <li>Final variables must be initialized exactly once</li>
 *   <li>Only initialized variables may be read</li>
 *   <li>Assignments must respect type compatibility</li>
 * </ul>
 */
public class Variable {
    private final String varType;
    private boolean initialized;
    private final boolean isFinal;

    /**
     * Constructs a new variable descriptor.
     *
     * @param initialized whether the variable is initialized at the time of declaration
     * @param varType the declared type of the variable
     * @param isFinal whether the variable is declared {@code final}
     */
    public Variable(boolean initialized, String varType, boolean isFinal) {
        this.initialized = initialized;
        this.varType = varType;
        this.isFinal = isFinal;
    }

    /**
     * Returns the declared type of the variable.
     *
     * @return the variable type as a string
     */
    public String getVarType() { return varType; }

    /**
     * Indicates whether the variable is declared {@code final}.
     *
     * @return {@code true} if the variable is final, {@code false} otherwise
     */
    public boolean isFinal() { return isFinal; }

    /**
     * Indicates whether the variable has been initialized.
     *
     * @return {@code true} if the variable is initialized, {@code false} otherwise
     */
    public boolean isInitialized() { return initialized; }

    /**
     * Sets the initialization state of the variable.
     *
     * <p>This method is typically called after a successful assignment.</p>
     *
     * @param initialized the new initialization state
     */
    public void setInitialized(boolean initialized) { this.initialized = initialized; }
}
