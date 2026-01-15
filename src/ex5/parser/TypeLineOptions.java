package ex5.parser;

/**
 * Enumerates the possible classifications of a single S-Java source line.
 *
 * <p>Each value represents a distinct syntactic category identified by
 * {@link Line#getLineType(String)}. These categories are used by the parser
 * to dispatch validation logic and enforce scope rules.</p>
 */
public enum TypeLineOptions {
    /**
     * A comment line starting with {@code //}.
     * Comment lines are ignored by the parser.
     */
    commentLine,

    /**
     * A variable declaration line.
     *
     * <p>Includes declarations with or without {@code final}, and possibly
     * with initialization (e.g., {@code int x;} or {@code final int y = 5;}).</p>
     */
    variableLine,

    /**
     * A method declaration line.
     *
     * <p>Represents the opening line of a {@code void} method, including
     * its parameter list and opening brace.</p>
     */
    methodLine,

    /**
     * An empty or whitespace-only line.
     * Empty lines are ignored by the parser.
     */
    emptyLine,

    /**
     * A {@code return;} statement.
     *
     * <p>Valid only inside method bodies and must appear as the final
     * executable statement of a method.</p>
     */
    returnLine,

    /**
     * An {@code if} or {@code while} statement header.
     *
     * <p>Includes the condition and opening brace.</p>
     */
    ifWhileLine,

    /**
     * A closing brace {@code }} indicating the end of a scope.
     */
    closingBrackets,

    /**
     * A method call statement.
     *
     * <p>Represents a call to a previously declared method.</p>
     */
    methodCallLine,

    /**
     * A variable assignment statement.
     *
     * <p>Represents assignment to an already-declared variable and is
     * illegal in the global scope.</p>
     */
    assignmentLine
}

