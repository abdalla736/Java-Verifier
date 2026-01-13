package ex5.main;

import ex5.handleVariables.Variable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Scope {
    private final Stack<Map<String, Variable>> scopes = new Stack<>();
    private final Map<String, Variable> globals;

    public Scope(Map<String, Variable> globals) {
        if (globals == null) throw new IllegalArgumentException("globals cannot be null");
        this.globals = globals;
        pushScope();
    }

    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    public void popScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    public boolean hasAnyScope() {
        return !scopes.isEmpty();
    }

    public boolean declareLocal(String name, Variable info) {
        Map<String, Variable> top = scopes.peek();
        if (top.containsKey(name)) return false;
        top.put(name, info);
        return true;
    }

    public boolean isDeclaredInCurrentScope(String name) {
        if (scopes.isEmpty()) return false;
        return scopes.peek().containsKey(name);
    }

    public Variable resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Variable v = scopes.get(i).get(name);
            if (v != null) return v;
        }
        return globals.get(name);
    }

    public void markInitialized(String name) {
        Variable v = resolve(name);
        if (v != null) {
            v.setInitialized(true);
        }
    }

    public boolean isInitialized(String name) {
        Variable v = resolve(name);
        return v != null && v.isInitialized();
    }
}
