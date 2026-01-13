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

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(OUT_IO_ERROR);
            System.err.println("Error: Expected exactly one argument (source file)");
            return;
        }

        String fileName = args[0];

        if (!fileName.endsWith(".sjava")) {
            System.out.println(OUT_IO_ERROR);
            System.err.println("Error: File must have .sjava extension");
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
