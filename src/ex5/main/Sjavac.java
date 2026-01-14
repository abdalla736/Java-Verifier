package ex5.main;

import ex5.handleMethods.MethodException;
import ex5.handleVariables.VariableException;
import ex5.parser.Parser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Sjavac {
    public static final int OUT_SUCCESS = 0;
    public static final int OUT_SYNTAX_ERROR = 1;
    public static final int OUT_IO_ERROR = 2;
    private static final String ARG_ERROR = "Error: Expected exactly one argument (source file)";
    private static final String END_NAME_FILE = ".sjava";
    private static final String END_NAME_FILE_ERROR="Error: File must have .sjava extension";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(OUT_IO_ERROR);
            System.err.println(ARG_ERROR);
            return;
        }

        String fileName = args[0];

        if (!fileName.endsWith(END_NAME_FILE)) {
            System.out.println(OUT_IO_ERROR);
            System.err.println(END_NAME_FILE_ERROR);
            return;
        }

        try (BufferedReader buffer = new BufferedReader(new FileReader(fileName))) {
            Parser parser = new Parser(buffer);
            int result = parser.parse();
            System.out.println(result);
        } catch (IOException e) {
            System.out.println(OUT_IO_ERROR);
            System.err.println("IO Error: " + e.getMessage());
        } catch (VariableException | MethodException e) {
            System.out.println(OUT_SYNTAX_ERROR);
            System.err.println("Syntax Error: " + e.getMessage());
        }
    }
}
