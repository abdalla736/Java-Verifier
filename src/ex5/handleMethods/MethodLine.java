package ex5.handleMethods;

import ex5.handleVariables.Variable;
import ex5.handleVariables.VariableException;
import ex5.handleVariables.VariableLine;
import ex5.main.Scopes;
import ex5.parser.Line;
import ex5.parser.TypeLineOptions;
import ex5.validator.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ex5.validator.ValidationUtils.TYPE;

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

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^\\s*void\\s+" + METHOD_NAME + "\\s*\\((.*)\\)\\s*\\{\\s*$");

    private static final Pattern PARAM_PATTERN =
            Pattern.compile("^\\s*(final\\s+)?(" +  TYPE + ")\\s+([A-Za-z]\\w*)\\s*$");

    private static final Pattern CALL_PATTERN =
            Pattern.compile("^\\s*([A-Za-z]\\w*)\\s*\\((.*)\\)\\s*;\\s*$");

    // ===== Error constants =====
    private static final String ERR_INVALID_METHOD_DECL = "invalid method declaration";
    private static final String ERR_DUP_PARAMETER_PREFIX = "duplicate parameter name: " ;
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
     * Parses a single S-Java method header line and returns its method signature (name + parameters),
     * without reading or validating the method body.
     *
     * <p><b>Purpose:</b> This is intended for the parser's first pass, where all method signatures are
     * collected into the global methods table before validating method calls in a later pass.</p>
     *
     * <p><b>Accepted input format:</b> A line that matches {@code METHOD_PATTERN}, typically:
     * <pre>
     *     void <methodName>(<paramList>) {
     * </pre>
     * where {@code <paramList>} is either empty or a comma-separated list of parameters of the form:
     * <pre>
     *     (final)? <type> <name>
     * </pre>
     *
     * <p><b>Validation performed:</b>
     * <ul>
     *   <li>Ensures the header matches the required method declaration syntax.</li>
     *   <li>Extracts the method name and raw parameter list.</li>
     *   <li>Delegates parameter parsing/validation to {@link #parseParameters(String)}.</li>
     * </ul>
     *
     * <p><b>What this method does NOT do:</b>
     * <ul>
     *   <li>Does not register the method in the methods table.</li>
     *   <li>Does not validate the method body or enforce that it ends with {@code return;}.</li>
     *   <li>Does not create scopes or validate variable usage.</li>
     * </ul>
     *
     * @param headerLine
     *     The raw source line containing a method declaration header (including the opening '{').
     *     Must not be {@code null}; it should be a single line of code.
     *
     * @return
     *     A {@link Method} object representing the parsed method signature, containing:
     *     <ul>
     *       <li>the method name</li>
     *       <li>an ordered list of {@link Method.Parameter} objects</li>
     *     </ul>
     *
     * @throws MethodException
     *     If {@code headerLine} does not match the method header syntax, or if the parameter list
     *     contains an invalid parameter (as determined by {@link #parseParameters(String)}).
     */
    public static Method parseSignature(String headerLine)
            throws MethodException {

        Matcher m = METHOD_PATTERN.matcher(headerLine);
        if (!m.matches()) {
            throw new MethodException(ERR_INVALID_METHOD_DECL);
        }

        String methodName = m.group(1);
        String paramsStr = m.group(2).trim();

        List<Method.Parameter> params = parseParameters(paramsStr);
        return new Method(methodName, params);
    }


    /**
     * Validates a method declaration and its body, and registers its signature.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Checks method header syntax using {@link #METHOD_PATTERN}</li>
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
     * @param lines
     *     The full list of source lines of the s-Java file.
     * @param bodyFromLine
     *     The index (inclusive) of the first line inside the method body
     *     (the line immediately after the opening '{').
     * @param bodyToLine
     *     The index (inclusive) of the closing '}' of the method.
     * @param globalVars
     *     A map of all global variables that are visible to this method.
     * @param methods
     *     A map of all declared method signatures in the file, used for
     *     validating method calls.
     *
     * @throws MethodException if a method-related validation error occurs
     * @throws VariableException if a variable-related validation error occurs inside the method body
     */
    public void compileMethod(List<String> lines,
                                int bodyFromLine,
                                int bodyToLine,
                                HashMap<String, Variable> globalVars,
                                HashMap<String, Method> methods)
            throws MethodException, VariableException {


        Method method = parseSignature(line);
        List<Method.Parameter> params = method.getParameters();

        Scopes scopes = new Scopes(globalVars);

        // declare parameters as initialized locals
        for (Method.Parameter p : params) {
            Variable v = new Variable(true, p.getType(), p.isFinal());
            boolean ok = scopes.declareLocal(p.getName(), v);
            if (!ok) {
                throw new MethodException(ERR_DUP_PARAMETER_PREFIX+ p.getName());
            }
        }

        compileMethodBody(lines,bodyFromLine,bodyToLine, scopes, globalVars, methods);

    }

    private static List<Method.Parameter> parseParameters(String paramsStr)
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

    private void compileMethodBody(List<String> lines,
                                   int  bodyFromLine,
                                   int bodyToLine,
                                   Scopes scopes,
                                   HashMap<String, Variable> globalVars,
                                   HashMap<String, Method> methods)
            throws MethodException, VariableException {

        String bodyLine;
        boolean hasReturn = false;
        int depth = 1; // method opened '{' already

        for (int idx = bodyFromLine; idx <= bodyToLine; idx++) {
            bodyLine = lines.get(idx);
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
                IfWhileLine ifWhileLine = new IfWhileLine(bodyLine);
                ifWhileLine.compileIfWhile(scopes, globalVars);
                scopes.pushScope();
                depth++;
                continue;
            }

            if (lineType == TypeLineOptions.variableLine ||
                    lineType == TypeLineOptions.assignmentLine) {
                // Your VariableLine supports both decl + assignment already.
                VariableLine variableLine = new VariableLine(bodyLine);
                variableLine.compileVariableLine(globalVars, scopes, false);
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

        boolean validLiteral = ValidationUtils.validateArgument(arg, paramType);

        if(validLiteral) return;

        if (NAME_PATTERN.matcher(arg).matches()) {
            Variable v = (scopes != null) ? scopes.resolve(arg) : globalVars.get(arg);
            if (v == null) {
                throw new MethodException(ERR_UNDEFINED_VAR_IN_CALL_PREFIX + arg);
            }
            if (!v.isInitialized()) {
                throw new MethodException(ERR_UNINIT_VAR_IN_CALL_PREFIX + arg);
            }
            if (!ValidationUtils.isTypeCompatible(paramType, v.getVarType())) {
                throw new MethodException(ERR_CALL_TYPE_MISMATCH);
            }
            return;
        }

        throw new MethodException(ERR_INVALID_ARG_TYPE);
    }

}
