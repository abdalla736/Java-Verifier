package ex5.handleMethods;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single method declaration in an S-Java source file.
 *
 * <p>A {@code Method} stores only the method's signature information:
 * its name and the ordered list of parameters. The method body itself
 * is validated separately during parsing.</p>
 *
 * <p>This class is used during semantic validation to:</p>
 * <ul>
 *   <li>Detect duplicate method declarations</li>
 *   <li>Validate method calls (argument count and types)</li>
 * </ul>
 *
 * <p>The method return type is implicitly {@code void}, according to
 * the S-Java specification.</p>
 */
public class Method {
    private final String name;
    private final List<Parameter> parameters;

    /**
     * Constructs a new {@code Method} instance.
     *
     * @param name the method name
     * @param parameters the list of parameters (may be {@code null}, treated as empty)
     */
    public Method(String name, List<Parameter> parameters) {
        this.name = name;
        this.parameters = (parameters != null) ? parameters : new ArrayList<>();
    }

    /**
     * @return the method name
     */
    public String getName() {
        return name;
    }


    /**
     * @return the ordered list of parameters of this method
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Represents a single parameter in a method declaration.
     *
     * <p>Each parameter has:</p>
     * <ul>
     *   <li>A type (int, double, boolean, char, or String)</li>
     *   <li>A name</li>
     *   <li>An optional {@code final} modifier</li>
     * </ul>
     *
     * <p>Parameters are considered initialized upon method entry.</p>
     */
    public static class Parameter {
        private final String type;
        private final String name;
        private final boolean isFinal;

        /**
         * Constructs a new method parameter.
         *
         * @param type the parameter type
         * @param name the parameter name
         * @param isFinal whether the parameter is final
         */
        public Parameter(String type, String name, boolean isFinal) {
            this.type = type;
            this.name = name;
            this.isFinal = isFinal;
        }

        /**
         * @return the parameter type
         */
        public String getType() { return type; }

        /**
         * @return the parameter name
         */
        public String getName() { return name; }

        /**
         * @return {@code true} if the parameter is final, {@code false} otherwise
         */
        public boolean isFinal() { return isFinal; }
    }
}
