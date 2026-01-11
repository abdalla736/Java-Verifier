package ex5.handleVariables;

public class Variable {

    private String varType;
    private boolean initialized;
    private boolean isFinal;

    public Variable(boolean initialized, String varType, boolean isFinal)
    {
        this.initialized = initialized;
        this.varType = varType;
        this.isFinal = isFinal;
    }

    public String getVarType() { return varType; }
    public boolean isFinal() { return isFinal; }
    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean initialized) { this.initialized = initialized; }

}
