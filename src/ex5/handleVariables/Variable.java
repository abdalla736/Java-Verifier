package ex5.handleVariables;

public class Variable {
    private String varType;
    private boolean initialized;
    private boolean isFinal;
    private String varValue;
    private String varOldValue;

    public Variable(boolean initialized, String varType, boolean isFinal) {
        this.initialized = initialized;
        this.varType = varType;
        this.isFinal = isFinal;
    }

    public Variable(boolean initialized, String varType, boolean isFinal, String varValue, String varOldValue) {
        this.initialized = initialized;
        this.varType = varType;
        this.isFinal = isFinal;
        this.varValue = varValue;
        this.varOldValue = varOldValue;
    }

    public String getVarType() { return varType; }
    public boolean isFinal() { return isFinal; }
    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean initialized) { this.initialized = initialized; }
}



