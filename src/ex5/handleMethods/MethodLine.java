package ex5.handleMethods;

import ex5.handleVariables.Variable;
import ex5.handleVariables.VariableException;
import ex5.handleVariables.VariableLine;
import ex5.main.Scopes;
import ex5.parser.Line;
import ex5.parser.TypeLineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates and compiles a single S-Java method declaration (header + body).
 *
 * <p>{@code MethodLine} is created from a raw method header line (e.g., {@code void f(int x) { }})
 * and is responsible for:</p>
 *
 * <ul>
 *   <li>Validating the method declaration syntax</li>
 *   <li>Parsing the parameter list into {@link Method.Parameter} objects</li>
 *   <li>Registering the method signature in the global methods table</li>
 *   <li>Validating the method body line-by-line until its matching closing brace</li>
 *   <li>Managing block nesting using {@link Scopes} (method scope + if/while scopes)</li>
 * </ul>
 *
 * <p>The body validation uses {@link Line#getLineType(String)} to classify each line and delegates:
 * <ul>
 *   <li>Variable declarations / assignments to {@link VariableLine}</li>
 *   <li>{@code if}/{@code while} conditions to {@link IfWhileLine}</li>
 *   <li>Method calls to internal validation logic based on the {@code methods} table</li>
 * </ul>
 *
 * <p>This class does not execute S-Java code. It only verifies legality.</p>
 */
public class MethodLine {

    // ===== Regex constants =====
    private static final String METHOD_NAME = "([A-Za-z]\\w*)";
    private static final String TYPE = "(int|double|boolean|char|String)";

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^\\s*void\\s+" + METHOD_NAME + "\\s*\\((.*)\\)\\s*\\{\\s*$");

    private static final Pattern PARAM_PATTERN =
            Pattern.compile("^\\s*(final\\s+)?(" + TYPE + ")\\s+([A-Za-z]\\w*)\\s*$");

    private static final Pattern CALL_PATTERN =
            Pattern.compile("^\\s*([A-Za-z]\\w*)\\s*\\((.*)\\)\\s*;\\s*$");

    // ===== Error constants =====
    private static final String ERR_INVALID_METHOD_DECL = "invalid method declaration";
    private static final String ERR_DUP_METHOD_PREFIX = "duplicate method name: ";
    private static final String ERR_INVALID_PARAM_PREFIX = "invalid parameter: ";
    private static final String ERR_METHOD_NOT_CLOSED = "method not closed properly";
    private static final String ERR_MISSING_RETURN = "method must end with return";
    private static final String ERR_UNEXPECTED_LINE_PREFIX = "unexpected line in method: ";

    private static final String ERR_INVALID_CALL_SYNTAX = "invalid method call syntax";
    private static final String ERR_UNDEFINED_METHOD_PREFIX = "call to undefined method: ";
    private static final String ERR_WRONG_ARG_COUNT_PREFIX =
            "wrong number of arguments for method: ";
    private static final String ERR_UNDEFINED_VAR_IN_CALL_PREFIX =
            "undefined variable in method call: ";
    private static final String ERR_UNINIT_VAR_IN_CALL_PREFIX =
            "uninitialized variable in method call: ";
    private static final String ERR_CALL_TYPE_MISMATCH = "type mismatch in method call";
    private static final String ERR_INVALID_ARG_TYPE = "invalid argument type";
    private static final String EXTRA_CLOSING_BRACES = "too many closing braces";

    // argument patterns
    private static final Pattern INT_LIT = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern DOUBLE_LIT =
            Pattern.compile("^[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)$");
    private static final Pattern BOOL_LIT = Pattern.compile("^(true|false)$");
    private static final Pattern CHAR_LIT = Pattern.compile("^'[^']'$");
    private static final Pattern STR_LIT = Pattern.compile("^\"[^\"]*\"$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^(?!__)(?:[A-Za-z]\\w*|_[A-Za-z0-9]\\w*)$");

    private final String line;

    /**
     * Constructs a validator/compiler for a single method header line.
     *
     * @param line raw source line containing a method declaration header
     */
    public MethodLine(String line) {
        this.line = line;
    }

    /**
     * Validates a method declaration and its body, and registers its signature.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Checks method header syntax using {@link #METHOD_PATTERN}</li>
     *   <li>Ensures the method name is unique in {@code methods}</li>
     *   <li>Parses parameters (including {@code final})</li>
     *   <li>Creates a {@link Scopes} instance for method-local scoping</li>
     *   <li>Declares parameters as initialized local variables</li>
     *   <li>Reads and validates the method body until the method closes</li>
     * </ol>
     *
     * <p>The method body is validated using line-type classification provided by
     * {@link Line#getLineType(String)}. Nested blocks ({@code if}/{@code while})
     * push new scopes, and closing braces pop scopes.</p>
     *
     * @param reader the reader positioned immediately after the method header line;
     *               this method continues consuming lines until the method closes
     * @param globalVars global variable table used for resolution and assignments
     * @param methods global methods table used to validate method calls
     *
     * @return a {@link Method} object representing the validated method signature
     *
     * @throws MethodException if a method-related validation error occurs
     * @throws VariableException if a variable-related validation error occurs inside the method body
     * @throws IOException if an I/O error occurs while reading further lines from {@code reader}
     */
    public Method compileMethod(BufferedReader reader,
                                HashMap<String, Variable> globalVars,
                                HashMap<String, Method> methods)
            throws MethodException, IOException, VariableException {

        Matcher m = METHOD_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new MethodException(ERR_INVALID_METHOD_DECL);
        }

        String methodName = m.group(1);
        String paramsStr = m.group(2).trim();

        if (methods.containsKey(methodName)) {
            throw new MethodException(ERR_DUP_METHOD_PREFIX + methodName);
        }

        List<Method.Parameter> params = parseParameters(paramsStr);
        Method method = new Method(methodName, params);

        methods.put(methodName, method);

        Scopes scopes = new Scopes(globalVars);

        // declare parameters as initialized locals
        for (Method.Parameter p : params) {
            Variable v = new Variable(true, p.getType(), p.isFinal());
            boolean ok = scopes.declareLocal(p.getName(), v);
            if (!ok) {
                throw new MethodException("duplicate parameter name: " + p.getName());
            }
        }

        compileMethodBody(reader, scopes, globalVars, methods);

        return method;
    }

    private List<Method.Parameter> parseParameters(String paramsStr)
            throws MethodException {
        List<Method.Parameter> params = new ArrayList<>();
        if (paramsStr.isEmpty()) return params;

        String[] parts = paramsStr.split("\\s*,\\s*");
        for (String part : parts) {
            Matcher m = PARAM_PATTERN.matcher(part.trim());
            if (!m.matches()) {
                throw new MethodException(ERR_INVALID_PARAM_PREFIX + part);
            }
            boolean isFinal = m.group(1) != null;
            String type = m.group(2);
            String name = m.group(3);
            params.add(new Method.Parameter(type, name, isFinal));
        }
        return params;
    }

    private void compileMethodBody(BufferedReader reader,
                                   Scopes scopes,
                                   HashMap<String, Variable> globalVars,
                                   HashMap<String, Method> methods)
            throws IOException, MethodException, VariableException {

        String bodyLine;
        boolean hasReturn = false;
        int depth = 1; // method opened '{' already

        while ((bodyLine = reader.readLine()) != null) {
            TypeLineOptions lineType = Line.getLineType(bodyLine);

            if (lineType == null) {
                throw new MethodException(ERR_UNEXPECTED_LINE_PREFIX + bodyLine);
            }

            if (lineType == TypeLineOptions.commentLine || lineType == TypeLineOptions.emptyLine) {
                continue;
            }

            if (lineType == TypeLineOptions.closingBrackets) {
                if (!scopes.popScope()) {
                    throw new MethodException(EXTRA_CLOSING_BRACES);
                }

                depth--;
                if (depth == 0) {
                    if (!hasReturn) throw new MethodException(ERR_MISSING_RETURN);
                    return;
                }
                continue;
            }

            if (lineType == TypeLineOptions.returnLine) {
                hasReturn = true;
                continue;
            }

            if (lineType == TypeLineOptions.ifWhileLine) {
                IfWhileLine iwl = new IfWhileLine(bodyLine);
                iwl.compileIfWhile(scopes, globalVars);
                scopes.pushScope();
                depth++;
                continue;
            }

            if (lineType == TypeLineOptions.variableLine ||
                    lineType == TypeLineOptions.assignmentLine) {
                // Your VariableLine supports both decl + assignment already.
                VariableLine vl = new VariableLine(bodyLine);
                vl.compileVariableLine(globalVars, scopes, false);
                continue;
            }

            if (lineType == TypeLineOptions.methodCallLine) {
                compileMethodCall(bodyLine, scopes, globalVars, methods);
                continue;
            }

            throw new MethodException(ERR_UNEXPECTED_LINE_PREFIX + bodyLine);
        }

        throw new MethodException(ERR_METHOD_NOT_CLOSED);
    }

    private void compileMethodCall(String line,
                                   Scopes scopes,
                                   HashMap<String, Variable> globalVars,
                                   HashMap<String, Method> methods)
            throws MethodException {

        Matcher m = CALL_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new MethodException(ERR_INVALID_CALL_SYNTAX);
        }

        String methodName = m.group(1);
        String argsStr = m.group(2).trim();

        Method method = methods.get(methodName);
        if (method == null) {
            throw new MethodException(ERR_UNDEFINED_METHOD_PREFIX + methodName);
        }

        List<String> args = new ArrayList<>();
        if (!argsStr.isEmpty()) {
            for (String arg : argsStr.split("\\s*,\\s*")) {
                args.add(arg.trim());
            }
        }

        if (args.size() != method.getParameters().size()) {
            throw new MethodException(ERR_WRONG_ARG_COUNT_PREFIX + methodName);
        }

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            String paramType = method.getParameters().get(i).getType();
            validateArgument(arg, paramType, scopes, globalVars);
        }
    }

    private void validateArgument(String arg,
                                  String paramType,
                                  Scopes scopes,
                                  HashMap<String, Variable> globalVars)
            throws MethodException {

        if (NAME_PATTERN.matcher(arg).matches()) {
            Variable v = (scopes != null) ? scopes.resolve(arg) : globalVars.get(arg);
            if (v == null) {
                throw new MethodException(ERR_UNDEFINED_VAR_IN_CALL_PREFIX + arg);
            }
            if (!v.isInitialized()) {
                throw new MethodException(ERR_UNINIT_VAR_IN_CALL_PREFIX + arg);
            }
            if (!isTypeCompatible(paramType, v.getVarType())) {
                throw new MethodException(ERR_CALL_TYPE_MISMATCH);
            }
            return;
        }

        boolean validLiteral =
                (paramType.equals("int") && INT_LIT.matcher(arg).matches())
                        || (paramType.equals("double") &&
                        (DOUBLE_LIT.matcher(arg).matches() || INT_LIT.matcher(arg).matches()))
                        || (paramType.equals("boolean") &&
                        (BOOL_LIT.matcher(arg).matches() || DOUBLE_LIT.matcher(arg).matches()
                                || INT_LIT.matcher(arg).matches()))
                        || (paramType.equals("char") && CHAR_LIT.matcher(arg).matches())
                        || (paramType.equals("String") && STR_LIT.matcher(arg).matches());

        if (!validLiteral) {
            throw new MethodException(ERR_INVALID_ARG_TYPE);
        }
    }

    private boolean isTypeCompatible(String targetType, String sourceType) {
        if (targetType.equals(sourceType)) return true;
        if (targetType.equals("double") && sourceType.equals("int")) return true;
        return targetType.equals("boolean") && (sourceType.equals("int") || sourceType.equals("double"));
    }
}
