package ex5.parser;

import java.util.regex.Pattern;

/**
 * Utility class responsible for classifying raw S-Java source lines.
 *
 * <p>The {@code Line} class performs lexical pattern matching on individual
 * lines of source code and determines their structural category
 * (e.g., method declaration, variable declaration, assignment, control flow,
 * return statement, etc.).</p>
 *
 * <p>This class does not validate semantic correctness; it only identifies
 * the general form of a line. Semantic validation is delegated to other
 * components such as {@code MethodLine}, {@code VariableLine}, and
 * {@code IfWhileLine}.</p>
 */
public class Line {

    // ===== Name rules =====
    private static final String VAR_NAME = "(?!__)(?:[A-Za-z]\\w*|_[A-Za-z0-9]\\w*)";

    // Method name (usually must start with a letter)
    private static final String METHOD_NAME = "([A-Za-z]\\w*)";

    private static final String TYPE = "(int|double|String|boolean|char)";

    // ===== Patterns =====
    private static final Pattern EMPTY_PATTERN =
            Pattern.compile("^\\s*$");

    private static final Pattern COMMENT_PATTERN =
            Pattern.compile("^\\s*//.*$");

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^\\s*void\\s+" + METHOD_NAME + "\\s*\\(.*\\)\\s*\\{\\s*$");

    private static final Pattern RETURN_PATTERN =
            Pattern.compile("^\\s*return\\s*;\\s*$");

    private static final Pattern CLOSING_PATTERN =
            Pattern.compile("^\\s*}\\s*$");

    private static final Pattern IF_PATTERN =
            Pattern.compile("^\\s*if\\s*\\(.*\\)\\s*\\{\\s*$");

    private static final Pattern WHILE_PATTERN =
            Pattern.compile("^\\s*while\\s*\\(.*\\)\\s*\\{\\s*$");

    private static final Pattern VAR_DECL_PATTERN =
            Pattern.compile("^\\s*(?:final\\s+)?" + TYPE + "\\s+.+;\\s*$");

    private static final Pattern ASSIGN_PATTERN =
            Pattern.compile("^\\s*" + VAR_NAME + "\\s*=\\s*.+;\\s*$");

    private static final Pattern METHOD_CALL_PATTERN =
            Pattern.compile("^\\s*" + METHOD_NAME + "\\s*\\(.*\\)\\s*;\\s*$");


    /**
     * Classifies a single raw source line into a {@link TypeLineOptions}.
     *
     * <p>The method attempts to match the line against known legal S-Java
     * constructs in a fixed priority order. The first matching category
     * determines the returned type.</p>
     *
     * <p>If the line does not match any legal form, {@code null} is returned.</p>
     *
     * @param line a raw line of source code
     * @return the corresponding {@link TypeLineOptions}, or {@code null}
     *         if the line does not represent a legal S-Java statement
     */
    public static TypeLineOptions getLineType(String line) {
        if (line == null) return null;

        if (COMMENT_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.commentLine;
        }

        if (EMPTY_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.emptyLine;
        }

        if (METHOD_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.methodLine;
        }

        if (RETURN_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.returnLine;
        }

        if (CLOSING_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.closingBrackets;
        }

        if (IF_PATTERN.matcher(line).matches() || WHILE_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.ifWhileLine;
        }

        if (METHOD_CALL_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.methodCallLine;
        }

        if (ASSIGN_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.assignmentLine;
        }

        if (VAR_DECL_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.variableLine;
        }

        return null;
    }
}
