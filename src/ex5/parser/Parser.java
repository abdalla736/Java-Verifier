package ex5.parser;

import ex5.handleMethods.Method;
import ex5.handleMethods.MethodException;
import ex5.handleMethods.MethodLine;
import ex5.handleVariables.*;
import ex5.main.Sjavac;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class Parser {

    private final static String INVALID_LINE_SYNTAX = "Invalid line syntax: ";
    private static final String INVALID_STATEMENT = "Invalid statement in global scope: ";

    private final BufferedReader bufferedReader;
    private final HashMap<String, Variable> globalVariables;
    private final HashMap<String, Method> methods;

    public Parser(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        this.globalVariables = new HashMap<>();
        this.methods = new HashMap<>();
    }

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
                Method method = methodLine.compileMethod(bufferedReader, globalVariables, methods);
                methods.put(method.getName(), method);
            } else if (lineType == TypeLineOptions.variableLine) {
                VariableLine variableLine = new VariableLine(line);
                variableLine.compileVariableLine(globalVariables, null, true);
            } else {
                throw new MethodException(INVALID_STATEMENT + line);
            }
        }

        return Sjavac.OUT_SUCCESS;
    }
}