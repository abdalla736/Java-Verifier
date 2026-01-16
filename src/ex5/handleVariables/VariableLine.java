package ex5.handleVariables;

import ex5.main.Scopes;
import ex5.validator.ValidationUtils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ex5.validator.ValidationUtils.TYPE;


/**
 * Validates S-Java variable statements (declarations and assignments).
 *
 * <p>{@code VariableLine} receives a single raw source line that ends with a semicolon
 * and verifies that it is either:</p>
 *
 * <ul>
 *   <li><b>Declaration</b>:
 *     <pre>
 *     (final)? &lt;type&gt; &lt;name&gt; (= &lt;value&gt;)? (, &lt;name&gt; (= &lt;value&gt;)?)* ;
 *     </pre>
 *   </li>
 *   <li><b>Assignment</b>:
 *     <pre>
 *     &lt;name&gt; = &lt;value&gt; (, &lt;name&gt; = &lt;value&gt;)* ;
 *     </pre>
 *   </li>
 * </ul>
 *
 * <p>This class performs both syntactic checks (valid form, semicolon, token patterns)
 * and semantic checks required by the S-Java verifier, including:</p>
 *
 * <ul>
 *   <li>Duplicate variable declarations (global or within the current scope)</li>
 *   <li>{@code final} variables must be initialized at declaration time</li>
 *   <li>Assignments must target already-declared variables</li>
 *   <li>{@code final} variables cannot be reassigned</li>
 *   <li>Reading from a variable reference requires that it is initialized</li>
 *   <li>Type compatibility rules for assignments</li>
 * </ul>
 *
 * <p>The class does not create or manage scopes; it relies on {@link Scopes} to resolve
 * local variables and on the provided global map for global variables.</p>
 */
public class VariableLine {

    // ========= Regex pieces =========
    private static final String NAME = "(?!__)(?:[A-Za-z]\\w*|_[A-Za-z0-9]\\w*)";

    private static final String INT_LIT = "[+-]?\\d+";
    private static final String DOUBLE_LIT = "[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)";
    private static final String BOOL_LIT = "true|false";
    private static final String CHAR_LIT = "'[^']'";
    private static final String STR_LIT = "\"[^\"]*\"";

    private static final String VALUE =
            "(?:" + STR_LIT + "|" + CHAR_LIT + "|" + BOOL_LIT +
                    "|" + DOUBLE_LIT + "|" + INT_LIT + "|" + NAME + ")";

    // ========= Precompiled patterns =========
    private static final Pattern DECL_HEAD =
            Pattern.compile("^\\s*(final\\s+)?(" + TYPE + ")\\s+(.+?)\\s*$");

    private static final Pattern DECL_ITEM =
            Pattern.compile("^(" + NAME + ")(?:\\s*=\\s*(" + VALUE + "))?$");

    private static final Pattern ASSIGN_ITEM =
            Pattern.compile("^(" + NAME + ")\\s*=\\s*(" + VALUE + ")$");

    // Atomic patterns (anchored)
    private static final Pattern NAME_PATTERN = Pattern.compile("^" + NAME + "$");

    // ========= Error message constants =========
    private static final String ERR_MISSING_SEMI = "missing ';'.";
    private static final String ERR_EMPTY_LINE = "empty line";
    private static final String ERR_INVALID_DECL = "invalid declaration";
    private static final String ERR_DUP_GLOBAL_PREFIX = "duplicate global variable: ";
    private static final String ERR_DUP_SCOPE_PREFIX = "duplicate variable in scope: ";
    private static final String ERR_FINAL_MUST_INIT = "final must be initialized";
    private static final String ERR_BAD_ASSIGN_SYNTAX = "bad assignment syntax";
    private static final String ERR_ASSIGN_UNDECL_PREFIX = "assign to undeclared variable: ";
    private static final String ERR_REASSIGN_FINAL_PREFIX = "cannot reassign final variable: ";
    private static final String ERR_REF_UNDECL_PREFIX = "reference to undeclared variable: ";
    private static final String ERR_REF_UNINIT_PREFIX = "reference to uninitialized variable: ";
    private static final String ERR_TYPE_MISMATCH = "type mismatch in assignment";
    private static final String ERR_DECL_WITHOUT_SCOPE =
            "internal error: local declaration without scopes";
    private static final String ERR_UNKNOWN_TARGET_TYPE = "unknown target type: ";


    private final String varLine;

    /**
     * Constructs a validator for a single variable-related source line.
     *
     * @param varLine the raw source line representing a declaration or assignment
     */
    public VariableLine(String varLine) {
        this.varLine = varLine;
    }

    /**
     * Validates and applies a variable declaration or assignment statement.
     *
     * <p>This method determines whether the provided line is a declaration or an assignment,
     * validates it, and updates either the global variables map or the current scope accordingly.</p>
     *
     * <p><b>Declaration rules:</b>
     * <ul>
     *   <li>Supports optional {@code final}</li>
     *   <li>Supports multiple declarations separated by commas</li>
     *   <li>{@code final} variables must be initialized</li>
     *   <li>Rejects duplicate names in the relevant scope</li>
     * </ul>
     *
     * <p><b>Assignment rules:</b>
     * <ul>
     *   <li>Supports multiple assignments separated by commas</li>
     *   <li>Target variable must be declared and not {@code final}</li>
     *   <li>Referenced variables on RHS must be initialized</li>
     *   <li>Type compatibility is enforced</li>
     * </ul>
     *
     * @param variables the global variable table (name -> {@link Variable})
     * @param scopes the local scope stack (null when parsing global scope)
     * @param isGlobal {@code true} if this line is in global scope, {@code false} if inside a method/block
     *
     * @throws VariableException if the line violates S-Java syntax or semantic rules
     */
    public void compileVariableLine(HashMap<String, Variable> variables, Scopes scopes, boolean isGlobal)
            throws VariableException {

        String line = (varLine == null) ? "" : varLine.trim();

        if (!line.endsWith(";")) {
            throw new VariableException(ERR_MISSING_SEMI);
        }

        // remove ';'
        line = line.substring(0, line.length() - 1).trim();
        if (line.isEmpty()) {
            throw new VariableException(ERR_EMPTY_LINE);
        }

        Matcher head = DECL_HEAD.matcher(line);
        if (head.matches()) {
            handleDeclaration(head, variables, scopes, isGlobal);
        } else {
            handleAssignment(line, variables, scopes, isGlobal);
        }
    }

    private void handleDeclaration(Matcher head,
                                   HashMap<String, Variable> variables,
                                   Scopes scopes,
                                   boolean isGlobal) throws VariableException {

        boolean isFinal = head.group(1) != null;
        String type = head.group(2);
        String rest = head.group(3).trim();

        String[] parts = rest.split("\\s*,\\s*");
        for (String raw : parts) {
            String part = raw.trim();
            Matcher item = DECL_ITEM.matcher(part);
            if (!item.matches()) {
                throw new VariableException(ERR_INVALID_DECL);
            }

            String name = item.group(1);
            String itemValue = item.group(2); // may be null

            // duplicates
            if (isGlobal) {
                if (variables.containsKey(name)) {
                    throw new VariableException(ERR_DUP_GLOBAL_PREFIX + name);
                }
            } else {
                if (scopes != null && scopes.isDeclaredInCurrentScope(name)) {
                    throw new VariableException(ERR_DUP_SCOPE_PREFIX + name);
                }
            }

            // final must be initialized
            if (isFinal && itemValue == null) {
                throw new VariableException(ERR_FINAL_MUST_INIT);
            }

            boolean init = (itemValue != null);
            if (init) {
                assignmentValidity(type, itemValue.trim(), variables, scopes);
            }

            Variable var = new Variable(init, type, isFinal);

            if (isGlobal) {
                variables.put(name, var);
            } else {
                if (scopes == null) {
                    throw new VariableException(ERR_DECL_WITHOUT_SCOPE);
                }
                boolean ok = scopes.declareLocal(name, var);
                if (!ok) {
                    throw new VariableException(ERR_DUP_SCOPE_PREFIX + name);
                }
            }
        }
    }

    private void handleAssignment(String line,
                                  HashMap<String, Variable> variables,
                                  Scopes scopes,
                                  boolean isGlobal) throws VariableException {

        String[] parts = line.split("\\s*,\\s*");
        for (String raw : parts) {
            String part = raw.trim();

            Matcher item = ASSIGN_ITEM.matcher(part);
            if (!item.matches()) {
                throw new VariableException(ERR_BAD_ASSIGN_SYNTAX);
            }

            String name = item.group(1);
            String itemValue = item.group(2).trim();

            Variable variable = isGlobal
                    ? variables.get(name)
                    : (scopes != null ? scopes.resolve(name) : null);

            if (variable == null) {
                throw new VariableException(ERR_ASSIGN_UNDECL_PREFIX + name);
            }
            if (variable.isFinal()) {
                throw new VariableException(ERR_REASSIGN_FINAL_PREFIX + name);
            }

            assignmentValidity(variable.getVarType(), itemValue, variables, scopes);
            variable.setInitialized(true);
        }
    }

    /**
     * Validate RHS compatibility with target type.
     * Handles both literal RHS and variable-reference RHS.
     */
    private void assignmentValidity(String targetType,
                                    String rhs,
                                    HashMap<String, Variable> variables,
                                    Scopes scopes) throws VariableException {

        if (rhs == null) return;
        rhs = rhs.trim();

        boolean validLiteral = ValidationUtils.validateArgument(rhs,targetType);

        if(validLiteral) return;

        // RHS is a variable name
        if (NAME_PATTERN.matcher(rhs).matches()) {
            Variable refVar = (scopes != null) ? scopes.resolve(rhs) : variables.get(rhs);

            if (refVar == null) {
                throw new VariableException(ERR_REF_UNDECL_PREFIX + rhs);
            }
            if (!refVar.isInitialized()) {
                throw new VariableException(ERR_REF_UNINIT_PREFIX + rhs);
            }
            if (!ValidationUtils.isTypeCompatible(targetType, refVar.getVarType())) {
                throw new VariableException(ERR_TYPE_MISMATCH);
            }
            return;
        }

        throw new VariableException(ERR_UNKNOWN_TARGET_TYPE + targetType);
    }

}
