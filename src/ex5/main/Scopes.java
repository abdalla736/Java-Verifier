package ex5.main;

import ex5.handleVariables.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


/**
 * Manages nested variable scopes for method bodies and control-flow blocks.
 *
 * <p>The {@code Scopes} class maintains a stack of local scopes, where each scope
 * maps variable names to their corresponding {@link Variable} descriptors.</p>
 *
 * <p>Variable lookup follows S-Java scoping rules:
 * <ul>
 *   <li>Search the innermost (most recent) local scope first</li>
 *   <li>Continue outward through enclosing scopes</li>
 *   <li>Fall back to the global variables map if not found locally</li>
 * </ul>
 *
 * <p>This class does not perform semantic checks itself; it only provides
 * storage and lookup utilities for variable resolution.</p>
 */
public class Scopes {

    private static final String GLOBAL_ERROR = "globals cannot be null";

    private final Stack<Map<String, Variable>> scopes = new Stack<>();
    private final Map<String, Variable> globals;

    /**
     * Constructs a new {@code Scopes} manager.
     *
     * <p>The constructor initializes the scope stack with a base scope,
     * representing the method-level scope.</p>
     *
     * @param globals map of global variables; must not be {@code null}
     *
     * @throws IllegalArgumentException if {@code globals} is {@code null}
     */
    public Scopes(Map<String, Variable> globals) {
        if (globals == null) throw new IllegalArgumentException(GLOBAL_ERROR);
        this.globals = globals;
        pushScope(); // base scope (method scope)
    }


    /**
     * Pushes a new empty local scope onto the scope stack.
     *
     * <p>This method should be called when entering a new block
     * (e.g., method body, if/while block).</p>
     */
    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    /**
     * Pops the current local scope from the scope stack.
     *
     * @return {@code true} if a scope was successfully removed,
     *         {@code false} if the stack was already empty
     */
    public boolean popScope() {
        if (scopes.isEmpty()) {
            return false;
        }
        scopes.pop();
        return true;
    }

    /**
     * Declares a variable in the current (innermost) local scope.
     *
     * @param name the variable name
     * @param info the variable descriptor
     *
     * @return {@code true} if the declaration succeeded,
     *         {@code false} if no scope exists or the name is already declared
     *         in the current scope
     */
    public boolean declareLocal(String name, Variable info) {
        if (scopes.isEmpty()) return false;
        Map<String, Variable> top = scopes.peek();
        if (top.containsKey(name)) return false;
        top.put(name, info);
        return true;
    }

    /**
     * Checks whether a variable is declared in the current (innermost) scope.
     *
     * @param name the variable name
     * @return {@code true} if the variable is declared in the current scope,
     *         {@code false} otherwise
     */
    public boolean isDeclaredInCurrentScope(String name) {
        return !scopes.isEmpty() && scopes.peek().containsKey(name);
    }


    /**
     * Resolves a variable name according to scoping rules.
     *
     * <p>The search order is:
     * <ol>
     *   <li>Innermost local scope</li>
     *   <li>Enclosing local scopes</li>
     *   <li>Global variables</li>
     * </ol>
     *
     * @param name the variable name
     * @return the corresponding {@link Variable}, or {@code null} if not found
     */
    public Variable resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Variable v = scopes.get(i).get(name);
            if (v != null) return v;
        }
        return globals.get(name);
    }
}
