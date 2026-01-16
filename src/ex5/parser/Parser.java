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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    private static final String ERR_DUP_METHOD_PREFIX = "duplicate method name: ";
    private static final String ERR_MISSING_CLOSING_BRACKETS = "method not closed properly";

    private final BufferedReader bufferedReader;
    private final HashMap<String, Variable> globalVariables;
    private final HashMap<String, Boolean> globalVariablesValues;
    private final HashMap<String, Method> methods;

     /**
      * Constructs a new {@code Parser} for the given input source.
      *
      * @param bufferedReader reader positioned at the beginning of an S-Java source file
      */
    public Parser(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        this.globalVariables = new HashMap<>();
        this.globalVariablesValues = new HashMap<>();
        this.methods = new HashMap<>();
    }

     /**
      * Parses and validates the entire S-Java source file.
      *
      * <p>The method reads the file sequentially until EOF is reached.
      * Then the method makes two passes,the first one reading all the methods,
      * global variables and the second one parse all the file.
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

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }

        // -------- PASS 1: collect method signatures + build global variables table --------
        for (int i = 0; i < lines.size(); i++) {
            String cur = lines.get(i);
            TypeLineOptions type = Line.getLineType(cur);

            if (type == null) {
                throw new MethodException(INVALID_LINE_SYNTAX + cur);
            }

            if (type == TypeLineOptions.commentLine || type == TypeLineOptions.emptyLine) {
                continue;
            }

            if (type == TypeLineOptions.methodLine) {
                // register method signature (forward calls supported)
                Method signature = MethodLine.parseSignature(cur);
                String name = signature.getName();
                if (methods.containsKey(name)) {
                    throw new MethodException(ERR_DUP_METHOD_PREFIX + name);
                }
                methods.put(name, signature);

                // skip method body in pass1
                int end = findMatchingClosingBrace(lines, i);
                i = end; // jump to end of method
                continue;
            }

            // Global scope: allow declarations AND assignments
            if (type == TypeLineOptions.variableLine || type == TypeLineOptions.assignmentLine) {
                VariableLine variableLine = new VariableLine(cur);
                variableLine.compileVariableLine(globalVariables, null, true);
                continue;
            }

            // everything else illegal globally
            throw new MethodException(INVALID_STATEMENT + cur);
        }

        // save if a global variable is initialized or not
        for ( String name : globalVariables.keySet() ) {
            Variable variable = globalVariables.get(name);
            globalVariablesValues.put(name, variable.isInitialized());
        }

        // -------- PASS 2: validate method bodies (now we know all methods + globals) --------
        for (int i = 0; i < lines.size(); i++) {
            String header = lines.get(i);
            TypeLineOptions lineType = Line.getLineType(header);

            if (lineType == null) {
                throw new MethodException(INVALID_LINE_SYNTAX + header);
            }

            if (lineType == TypeLineOptions.commentLine || lineType == TypeLineOptions.emptyLine) {
                continue;
            }

            if (lineType == TypeLineOptions.methodLine) {
                int end = findMatchingClosingBrace(lines, i);

                MethodLine methodLine = new MethodLine(header);

                methodLine.compileMethod(lines, i + 1, end, globalVariables, methods);

                i = end; // skip the method block

                //now may be the global variables values has changed then we have to reset them
                for ( String name : globalVariables.keySet() ) {
                    boolean isInitialized = globalVariablesValues.get(name);
                    Variable variable = globalVariables.get(name);
                    variable.setInitialized(isInitialized);
                }
            }

            // global lines were already validated in pass1; we ignore them here
            // (or you can re-validate again; not needed)
        }

        return Sjavac.OUT_SUCCESS;
    }

     private int findMatchingClosingBrace(List<String> lines, int startIndex) throws MethodException {
         int depth = 1;
         for (int i = startIndex + 1; i < lines.size(); i++) {
             TypeLineOptions typeLine = Line.getLineType(lines.get(i));
             if (typeLine == null) {
                 throw new MethodException(INVALID_LINE_SYNTAX + lines.get(i));
             }

             if (typeLine == TypeLineOptions.commentLine || typeLine == TypeLineOptions.emptyLine) {
                 continue;
             }

             if (typeLine == TypeLineOptions.ifWhileLine || typeLine == TypeLineOptions.methodLine) {
                 // methodLine inside method shouldn't exist by spec, but counting braces is fine
                 depth++;
             } else if (typeLine == TypeLineOptions.closingBrackets) {
                 depth--;
                 if (depth == 0) return i;
             }
         }
         throw new MethodException(ERR_MISSING_CLOSING_BRACKETS);
     }
 }
