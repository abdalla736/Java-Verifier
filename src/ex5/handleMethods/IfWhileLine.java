package ex5.handleMethods;

import ex5.main.Scopes;
import ex5.handleVariables.Variable;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates {@code if} and {@code while} statements in S-Java source code.
 *
 * <p>This class is responsible for syntactic and semantic verification of
 * conditional control-flow statements of the form:
 *
 * <pre>
 * if (condition) {
 * while (condition) {
 * </pre>
 *
 * <p>The class ensures that:
 * <ul>
 *   <li>The statement syntax is correct</li>
 *   <li>The condition is non-empty</li>
 *   <li>Each condition term is valid</li>
 *   <li>All referenced variables exist and are initialized</li>
 *   <li>Only boolean, int, or double expressions are used in conditions</li>
 * </ul>
 *
 * <p>This class does not manage scopes itself; it relies on {@link Scopes}
 * to resolve variable references.</p>
 */
public class IfWhileLine {

    // ===== Regex constants =====
    private static final Pattern IF_WHILE_PATTERN =
            Pattern.compile("^\\s*(?:if|while)\\s*\\((.*)\\)\\s*\\{\\s*$");

    private static final Pattern COND_SPLIT_PATTERN =
            Pattern.compile("\\s*(?:\\|\\||&&)\\s*");

    // Adjust name rule if your spec differs
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^(?!__)(?:[A-Za-z]\\w*|_[A-Za-z0-9]\\w*)$");

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("^[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)$");

    private static final Pattern BOOL_LIT_PATTERN =
            Pattern.compile("^(true|false)$");

    // ===== Error message constants =====
    private static final String ERR_INVALID_IF_WHILE_SYNTAX = "invalid if/while syntax";
    private static final String ERR_EMPTY_CONDITION = "empty condition is illegal";
    private static final String ERR_INVALID_CONDITION_EMPTY_TERM = "invalid condition: empty term";

    private static final String ERR_UNDEFINED_VAR_IN_COND_PREFIX = "undefined variable in condition: ";
    private static final String ERR_UNINITIALIZED_VAR_IN_COND_PREFIX =
            "uninitialized variable in condition: ";
    private static final String ERR_INVALID_TYPE_IN_COND_PREFIX = "invalid type in condition: ";
    private static final String ERR_INVALID_CONDITION_EXPR_PREFIX = "invalid condition expression: ";

    private final String line;

    /**
     * Constructs a new {@code IfWhileLine} for validation.
     *
     * @param line the raw source line containing the {@code if} or {@code while} statement
     */
    public IfWhileLine(String line) {
        this.line = line;
    }

    /**
     * Validates the syntax and semantics of an {@code if} or {@code while} statement.
     *
     * <p>This method verifies:
     * <ul>
     *   <li>Correct statement syntax</li>
     *   <li>Presence of a non-empty condition</li>
     *   <li>Legality of all condition terms</li>
     * </ul>
     *
     * @param scopes the active scope stack used to resolve local variables
     * @param globalVars the map of global variables
     *
     * @throws MethodException if the statement or its condition is illegal
     */
    public void compileIfWhile(Scopes scopes, HashMap<String, Variable> globalVars)
            throws MethodException {
        Matcher m = IF_WHILE_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new MethodException(ERR_INVALID_IF_WHILE_SYNTAX);
        }

        String condition = m.group(1).trim();
        if (condition.isEmpty()) {
            throw new MethodException(ERR_EMPTY_CONDITION);
        }

        validateCondition(condition, scopes, globalVars);
    }

    private void validateCondition(String cond, Scopes scopes, HashMap<String, Variable> globalVars)
            throws MethodException {

        // Split and keep empty tokens to catch "a &&" or "|| b"
        String[] parts = COND_SPLIT_PATTERN.split(cond, -1);

        for (String raw : parts) {
            String part = raw.trim();

            if (part.isEmpty()) {
                throw new MethodException(ERR_INVALID_CONDITION_EMPTY_TERM);
            }

            // boolean literal
            if (BOOL_LIT_PATTERN.matcher(part).matches()) continue;

            // numeric literal allowed in condition (per spec)
            if (NUMBER_PATTERN.matcher(part).matches()) continue;

            // variable reference
            if (NAME_PATTERN.matcher(part).matches()) {
                Variable v = (scopes != null) ? scopes.resolve(part) : globalVars.get(part);

                if (v == null) {
                    throw new MethodException(ERR_UNDEFINED_VAR_IN_COND_PREFIX + part);
                }
                if (!v.isInitialized()) {
                    throw new MethodException(ERR_UNINITIALIZED_VAR_IN_COND_PREFIX + part);
                }

                String type = v.getVarType();
                if (!type.equals("boolean") && !type.equals("int") && !type.equals("double")) {
                    throw new MethodException(ERR_INVALID_TYPE_IN_COND_PREFIX + type);
                }
                continue;
            }

            throw new MethodException(ERR_INVALID_CONDITION_EXPR_PREFIX + part);
        }
    }
}
