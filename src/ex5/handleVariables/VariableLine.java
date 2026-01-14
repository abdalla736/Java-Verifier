package ex5.handleVariables;


import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ex5.main.Scope;

public class VariableLine {
    private static final String NAME = "(?:[A-Za-z]\\w*|_[A-Za-z0-9]\\w*)";
    private static final String TYPE = "(?:int|double|boolean|char|String)";
    private static final String INT_LIT = "[+-]?\\d+";
    private static final String DOUBLE_LIT = "[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)";
    private static final String BOOL_LIT = "(?:true|false)";
    private static final String CHAR_LIT = "'[^']'";
    private static final String STR_LIT = "\"[^\"]*\"";
    private static final String VALUE = "(?:" + STR_LIT + "|" + CHAR_LIT + "|" + BOOL_LIT
            + "|" + DOUBLE_LIT + "|" + INT_LIT + "|" + NAME + ")";

    private static final Pattern DECL_HEAD = Pattern.compile("^\\s*(final\\s+)?(" + TYPE + ")\\s+(.+?)\\s*$");
    private static final Pattern DECL_ITEM = Pattern.compile("^(" + NAME + ")(?:\\s*=\\s*(" + VALUE + "))?$");
    private static final Pattern ASSIGN_ITEM = Pattern.compile("^(" + NAME + ")\\s*=\\s*(" + VALUE + ")$");

    private final String varLine;

    public VariableLine(String varLine) {
        this.varLine = varLine;
    }

    public void compileVariableLine(HashMap<String, Variable> variables, Scope scope, boolean isGlobal)
            throws VariableException {
        String line = varLine == null ? "" : varLine.trim();

        if (!line.endsWith(";")) {
            throw new VariableException("missing ';'.");
        }

        line = line.substring(0, line.length() - 1).trim();
        if (line.isEmpty()) {
            throw new VariableException("empty line");
        }

        Matcher head = DECL_HEAD.matcher(line);

        if (head.matches()) {
            handleDeclaration(head, variables, scope, isGlobal);
        } else {
            handleAssignment(line, variables, scope, isGlobal);
        }
    }

    private void handleDeclaration(Matcher head, HashMap<String, Variable> variables,
                                   Scope scope, boolean isGlobal) throws VariableException {
        boolean isFinal = head.group(1) != null;
        String type = head.group(2);
        String rest = head.group(3).trim();

        String[] parts = rest.split("\\s*,\\s*");
        for (String part : parts) {
            Matcher item = DECL_ITEM.matcher(part);
            if (!item.matches()) {
                throw new VariableException("invalid declaration");
            }

            String name = item.group(1);
            String itemValue = item.group(2);

            if (isGlobal) {
                if (variables.containsKey(name)) {
                    throw new VariableException("duplicate global variable: " + name);
                }
            } else {
                if (scope != null && scope.isDeclaredInCurrentScope(name)) {
                    throw new VariableException("duplicate variable in scope: " + name);
                }
            }

            if (isFinal && itemValue == null) {
                throw new VariableException("final must be initialized");
            }

            boolean init = itemValue != null;
            if (init) {
                assignmentValidity(type, itemValue, variables, scope, isGlobal);
            }

            Variable var = new Variable(init, type, isFinal);
            if (isGlobal) {
                variables.put(name, var);
            } else if (scope != null) {
                scope.declareLocal(name, var);
            }
        }
    }

    private void handleAssignment(String line, HashMap<String, Variable> variables,
                                  Scope scope, boolean isGlobal) throws VariableException {
        String[] parts = line.split("\\s*,\\s*");
        for (String part : parts) {
            Matcher item = ASSIGN_ITEM.matcher(part);
            if (!item.matches()) {
                throw new VariableException("bad assignment syntax");
            }

            String name = item.group(1);
            String itemValue = item.group(2);

            Variable variable = isGlobal ? variables.get(name) :
                    (scope != null ? scope.resolve(name) : null);
            if (variable == null) {
                throw new VariableException("assign to undeclared variable: " + name);
            }
            if (variable.isFinal()) {
                throw new VariableException("cannot reassign final variable: " + name);
            }

            assignmentValidity(variable.getVarType(), itemValue, variables, scope, isGlobal);
            variable.setInitialized(true);
        }
    }

    private void assignmentValidity(String type, String itemValue,
                                    HashMap<String, Variable> variables,
                                    Scope scope, boolean isGlobal)
            throws VariableException {

        if (itemValue == null) return;

        if (Pattern.compile(NAME).matcher(itemValue).matches()) {
            Variable refVar = isGlobal ? variables.get(itemValue) :
                    (scope != null ? scope.resolve(itemValue) : variables.get(itemValue));
            if (refVar == null) {
                throw new VariableException("reference to undeclared variable: " + itemValue);
            }
            if (!refVar.isInitialized()) {
                throw new VariableException("reference to uninitialized variable: " + itemValue);
            }
            if (!isTypeCompatible(type, refVar.getVarType())) {
                throw new VariableException("type mismatch in assignment");
            }
            return;
        }

        switch (type) {
            case "int":
                if (!Pattern.compile(INT_LIT).matcher(itemValue).matches()) {
                    throw new VariableException("incorrect int assignment.");
                }
                break;
            case "double":
                if (!Pattern.compile(DOUBLE_LIT + "|" + INT_LIT).matcher(itemValue).matches()) {
                    throw new VariableException("incorrect double assignment.");
                }
                break;
            case "boolean":
                if (!Pattern.compile(BOOL_LIT + "|" + DOUBLE_LIT + "|" + INT_LIT).matcher(itemValue).matches()) {
                    throw new VariableException("incorrect boolean assignment.");
                }
                break;
            case "char":
                if (!Pattern.compile(CHAR_LIT).matcher(itemValue).matches()) {
                    throw new VariableException("incorrect char assignment.");
                }
                break;
            case "String":
                if (!Pattern.compile(STR_LIT).matcher(itemValue).matches()) {
                    throw new VariableException("incorrect String assignment.");
                }
                break;
        }
    }

    private boolean isTypeCompatible(String targetType, String sourceType) {
        if (targetType.equals(sourceType)){
            return true;
        }
        if (targetType.equals("double") && sourceType.equals("int")) return true;
        if (targetType.equals("boolean") && (sourceType.equals("int") ||
                sourceType.equals("double"))) {
            return true;
        }
        return false;
    }
}