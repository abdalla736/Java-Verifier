package ex5.handleMethods;


import ex5.handleVariables.*;
import ex5.main.Scope;
import ex5.parser.Line;
import ex5.parser.TypeLineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodLine {
    private static final String NAME = "([a-zA-Z]\\w*)";
    private static final String TYPE = "(int|double|boolean|char|String)";
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^\\s*void\\s+" + NAME + "\\s*\\((.*)\\)\\s*\\{\\s*$"
    );

    private final String line;

    public MethodLine(String line) {
        this.line = line;
    }

    public Method compileMethod(BufferedReader reader, HashMap<String, Variable> globalVars,
                                HashMap<String, Method> methods)
            throws MethodException, IOException, VariableException {
        Matcher m = METHOD_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new MethodException("invalid method declaration");
        }

        String methodName = m.group(1);
        String paramsStr = m.group(2).trim();

        if (methods.containsKey(methodName)) {
            throw new MethodException("duplicate method name: " + methodName);
        }

        List<Method.Parameter> params = parseParameters(paramsStr);
        Method method = new Method(methodName, params);

        Scope scope = new Scope(globalVars);
        for (Method.Parameter p : params) {
            Variable v = new Variable(true, p.getType(), p.isFinal());
            scope.declareLocal(p.getName(), v);
        }

        compileMethodBody(reader, scope, globalVars, methods);

        return method;
    }

    private List<Method.Parameter> parseParameters(String paramsStr) throws MethodException {
        List<Method.Parameter> params = new ArrayList<>();
        if (paramsStr.isEmpty()) return params;

        String[] parts = paramsStr.split("\\s*,\\s*");
        for (String part : parts) {
            Pattern p = Pattern.compile("^(final\\s+)?(" + TYPE.substring(1) + ")\\s+([a-zA-Z]\\w*)$");
            Matcher m = p.matcher(part.trim());
            if (!m.matches()) {
                throw new MethodException("invalid parameter: " + part);
            }
            boolean isFinal = m.group(1) != null;
            String type = m.group(2);
            String name = m.group(3);
            params.add(new Method.Parameter(type, name, isFinal));
        }
        return params;
    }

    private void compileMethodBody(BufferedReader reader, Scope scope,
                                   HashMap<String, Variable> globalVars,
                                   HashMap<String, Method> methods)
            throws IOException, MethodException, VariableException {
        String bodyLine;
        boolean hasReturn = false;

        while ((bodyLine = reader.readLine()) != null) {
            TypeLineOptions lineType = Line.getLineType(bodyLine);

            if (lineType == TypeLineOptions.commentLine || lineType == TypeLineOptions.emptyLine) {
                continue;
            }

            if (lineType == TypeLineOptions.closingBrackets) {
                scope.popScope();
                if (!scope.hasAnyScope()) {
                    if (!hasReturn) {
                        throw new MethodException("method must end with return");
                    }
                    return;
                }
                continue;
            }

            if (lineType == TypeLineOptions.returnLine) {
                hasReturn = true;
                continue;
            }

            if (lineType == TypeLineOptions.variableLine) {
                VariableLine vl = new VariableLine(bodyLine);
                vl.compileVariableLine(globalVars, scope, false);
                continue;
            }

            if (lineType == TypeLineOptions.ifWhileLine) {
                IfWhileLine iwl = new IfWhileLine(bodyLine);
                iwl.compileIfWhile(scope, globalVars);
                scope.pushScope();
                continue;
            }

            if (lineType == TypeLineOptions.methodCallLine) {
                compileMethodCall(bodyLine, scope, globalVars, methods);
                continue;
            }

            throw new MethodException("unexpected line in method: " + bodyLine);
        }

        throw new MethodException("method not closed properly");
    }

    private void compileMethodCall(String line, Scope scope,
                                   HashMap<String, Variable> globalVars,
                                   HashMap<String, Method> methods)
            throws MethodException {
        Pattern callPattern = Pattern.compile("^\\s*([a-zA-Z]\\w*)\\s*\\((.*)\\)\\s*;\\s*$");
        Matcher m = callPattern.matcher(line);
        if (!m.matches()) {
            throw new MethodException("invalid method call syntax");
        }

        String methodName = m.group(1);
        String argsStr = m.group(2).trim();

        Method method = methods.get(methodName);
        if (method == null) {
            throw new MethodException("call to undefined method: " + methodName);
        }

        List<String> args = new ArrayList<>();
        if (!argsStr.isEmpty()) {
            for (String arg : argsStr.split("\\s*,\\s*")) {
                args.add(arg.trim());
            }
        }

        if (args.size() != method.getParameters().size()) {
            throw new MethodException("wrong number of arguments for method: " + methodName);
        }

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            String paramType = method.getParameters().get(i).getType();
            validateArgument(arg, paramType, scope, globalVars);
        }
    }

    private void validateArgument(String arg, String paramType,
                                  Scope scope, HashMap<String,
                    Variable> globalVars) throws MethodException {
        Pattern intLit = Pattern.compile("[+-]?\\d+");
        Pattern doubleLit = Pattern.compile("[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)");
        Pattern boolLit = Pattern.compile("true|false");
        Pattern charLit = Pattern.compile("'[^']'");
        Pattern strLit = Pattern.compile("\"[^\"]*\"");
        Pattern namePattern = Pattern.compile("[a-zA-Z]\\w*|_[a-zA-Z0-9]\\w*");

        if (namePattern.matcher(arg).matches()) {
            Variable v = scope != null ? scope.resolve(arg) : globalVars.get(arg);
            if (v == null) {
                throw new MethodException("undefined variable in method call: " + arg);
            }
            if (!v.isInitialized()) {
                throw new MethodException("uninitialized variable in method call: " + arg);
            }
            if (!isTypeCompatible(paramType, v.getVarType())) {
                throw new MethodException("type mismatch in method call");
            }
        } else {
            boolean validLiteral = false;
            if (paramType.equals("int") && intLit.matcher(arg).matches()) {
                validLiteral = true;
            }
            if (paramType.equals("double") &&
                    (doubleLit.matcher(arg).matches() || intLit.matcher(arg).matches())){
                validLiteral = true;
            }
            if (paramType.equals("boolean") &&
                    (boolLit.matcher(arg).matches() || doubleLit.matcher(arg).matches()
                            || intLit.matcher(arg).matches())){
                validLiteral = true;
            }
            if (paramType.equals("char") && charLit.matcher(arg).matches()){
                validLiteral = true;
            }
            if (paramType.equals("String") && strLit.matcher(arg).matches()){
                validLiteral = true;
            }

            if (!validLiteral) {
                throw new MethodException("invalid argument type");
            }
        }
    }

    private boolean isTypeCompatible(String targetType, String sourceType) {
        if (targetType.equals(sourceType)) return true;
        if (targetType.equals("double") && sourceType.equals("int")) return true;
        if (targetType.equals("boolean") && (sourceType.equals("int") || sourceType.equals("double"))){
            return true;
        }
        return false;
    }
}
