package ex5.handleMethods;


import ex5.main.Scope;
import ex5.handleVariables.Variable;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IfWhileLine {
    private final String line;

    public IfWhileLine(String line) {
        this.line = line;
    }

    public void compileIfWhile(Scope scope, HashMap<String, Variable> globalVars) throws MethodException {
        Pattern condPattern = Pattern.compile("^\\s*(?:if|while)\\s*\\((.+)\\)\\s*\\{\\s*$");
        Matcher m = condPattern.matcher(line);
        if (!m.matches()) {
            throw new MethodException("invalid if/while syntax");
        }

        String condition = m.group(1).trim();
        validateCondition(condition, scope, globalVars);
    }

    private void validateCondition(String cond, Scope scope, HashMap<String, Variable> globalVars) throws MethodException {
        String[] parts = cond.split("\\s*(?:\\|\\||&&)\\s*");

        for (String part : parts) {
            part = part.trim();
            if (part.equals("true") || part.equals("false")) continue;

            if (Pattern.compile("[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)").matcher(part).matches()) continue;

            if (Pattern.compile("[a-zA-Z]\\w*|_[a-zA-Z0-9]\\w*").matcher(part).matches()) {
                Variable v = scope != null ? scope.resolve(part) : globalVars.get(part);
                if (v == null) {
                    throw new MethodException("undefined variable in condition: " + part);
                }
                if (!v.isInitialized()) {
                    throw new MethodException("uninitialized variable in condition: " + part);
                }
                String type = v.getVarType();
                if (!type.equals("boolean") && !type.equals("int") && !type.equals("double")) {
                    throw new MethodException("invalid type in condition");
                }
            } else {
                throw new MethodException("invalid condition expression");
            }
        }
    }
}
