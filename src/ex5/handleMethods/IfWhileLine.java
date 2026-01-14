package ex5.handleMethods;

import ex5.main.Scope;
import ex5.handleVariables.Variable;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public IfWhileLine(String line) {
        this.line = line;
    }

    public void compileIfWhile(Scope scope, HashMap<String, Variable> globalVars) throws MethodException {
        Matcher m = IF_WHILE_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new MethodException(ERR_INVALID_IF_WHILE_SYNTAX);
        }

        String condition = m.group(1).trim();
        if (condition.isEmpty()) {
            throw new MethodException(ERR_EMPTY_CONDITION);
        }

        validateCondition(condition, scope, globalVars);
    }

    private void validateCondition(String cond, Scope scope, HashMap<String, Variable> globalVars)
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
                Variable v = (scope != null) ? scope.resolve(part) : globalVars.get(part);

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
