 package ex5.parser;

import ex5.handleMethods.Method;
import ex5.handleMethods.MethodException;
import ex5.handleMethods.MethodLine;
import ex5.handleVariables.Variable;
import ex5.handleVariables.VariableException;
import ex5.handleVariables.VariableLine;
import ex5.main.Sjavac;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

 /**
  * Top-level parser for S-Java source files.
  *
  * <p>The {@code Parser} is responsible for reading the source file line-by-line
  * and validating the program structure according to S-Java rules.</p>
  *
  * <p>Responsibilities include:
  * <ul>
  *   <li>Classifying each line using {@link Line#getLineType(String)}</li>
  *   <li>Dispatching method declarations to {@link MethodLine}</li>
  *   <li>Dispatching global variable declarations to {@link VariableLine}</li>
  *   <li>Enforcing that only legal statements appear in the global scope</li>
  * </ul>
  *
  * <p>The parser maintains:
  * <ul>
  *   <li>A table of global variables</li>
  *   <li>A table of declared methods (signatures)</li>
  * </ul>
  *
  * <p>Any syntax or semantic violation causes an exception to be thrown,
  * which is later translated into an output code by {@link ex5.main.Sjavac}.</p>
  */
public class Parser {

    private static final String INVALID_LINE_SYNTAX = "Invalid line syntax: ";
    private static final String INVALID_STATEMENT = "Invalid statement in global scope: ";

    private final BufferedReader bufferedReader;
    private final HashMap<String, Variable> globalVariables;
    private final HashMap<String, Method> methods;

     /**
      * Constructs a new {@code Parser} for the given input source.
      *
      * @param bufferedReader reader positioned at the beginning of an S-Java source file
      */
    public Parser(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        this.globalVariables = new HashMap<>();
        this.methods = new HashMap<>();
    }

     /**
      * Parses and validates the entire S-Java source file.
      *
      * <p>The method reads the file sequentially until EOF is reached.
      * For each line:
      * <ul>
      *   <li>Empty and comment lines are ignored</li>
      *   <li>Method declarations are validated and their bodies parsed</li>
      *   <li>Global variable declarations are validated</li>
      *   <li>Any other statement in the global scope is rejected</li>
      * </ul>
      *
      * @return {@link Sjavac#OUT_SUCCESS} (0) if the file is legal
      *
      * @throws IOException if an I/O error occurs while reading the file
      * @throws VariableException if a variable-related syntax or semantic error is detected
      * @throws MethodException if a method-related syntax or semantic error is detected
      */
    public int parse() throws IOException, VariableException, MethodException {
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            TypeLineOptions lineType = Line.getLineType(line);

            if (lineType == null) {
                throw new MethodException(INVALID_LINE_SYNTAX + line);
            }

            if (lineType == TypeLineOptions.commentLine || lineType == TypeLineOptions.emptyLine) {
                continue;
            }

            if (lineType == TypeLineOptions.methodLine) {
                MethodLine methodLine = new MethodLine(line);
                methodLine.compileMethod(bufferedReader, globalVariables, methods);
                continue;
            }

            if (lineType == TypeLineOptions.variableLine) {
                VariableLine variableLine = new VariableLine(line);
                variableLine.compileVariableLine(globalVariables, null, true);
                continue;
            }

            // everything else is illegal in global scope
            throw new MethodException(INVALID_STATEMENT + line);
        }

        return Sjavac.OUT_SUCCESS;
    }
}
