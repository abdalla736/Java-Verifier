package ex5.handleMethods;


import ex5.handleVariables.Variable;
import java.util.ArrayList;
import java.util.List;

public class Method {
    private String name;
    private List<Parameter> parameters;

    public Method(String name, List<Parameter> parameters) {
        this.name = name;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public static class Parameter {
        private String type;
        private String name;
        private boolean isFinal;

        public Parameter(String type, String name, boolean isFinal) {
            this.type = type;
            this.name = name;
            this.isFinal = isFinal;
        }

        public String getType() { return type; }
        public String getName() { return name; }
        public boolean isFinal() { return isFinal; }
    }
}
