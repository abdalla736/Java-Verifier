package ex5.main;

import ex5.handleMethods.MethodException;
import ex5.handleVariables.VariableException;
import ex5.parser.Parser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Entry point for the S-Java verifier (Sjavac).
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Validating command-line arguments</li>
 *   <li>Validating the input file extension (.sjava)</li>
 *   <li>Opening the source file for reading</li>
 *   <li>Invoking the {@link ex5.parser.Parser} to validate the program</li>
 *   <li>Printing a numeric status code to standard output</li>
 * </ul>
 *
 * <p>Output codes:
 * <ul>
 *   <li>{@link #OUT_SUCCESS} (0): the file is legal S-Java</li>
 *   <li>{@link #OUT_SYNTAX_ERROR} (1): the file contains an S-Java syntax/semantic error</li>
 *   <li>{@link #OUT_IO_ERROR} (2): an I/O error occurred while accessing the file</li>
 * </ul>
 *
 * <p>Note: This implementation also prints diagnostic messages to {@code System.err}.</p>
 */
public class Sjavac {

    /**
     * Output code indicating successful verification (legal file).
     */
    public static final int OUT_SUCCESS = 0;


    /**
     * Output code indicating a syntax/semantic verification error (illegal file).
     */
    public static final int OUT_SYNTAX_ERROR = 1;

    /**
     * Output code indicating an I/O error (e.g., cannot read file).
     */
    public static final int OUT_IO_ERROR = 2;

    private static final String ARG_ERROR = "Error: Expected exactly one argument (source file)";
    private static final String END_NAME_FILE = ".sjava";
    private static final String END_NAME_FILE_ERROR="Error: File must have .sjava extension";

    /**
     * Program entry point.
     *
     * <p>Expected usage: a single argument specifying a path to a {@code .sjava} file.
     * The method prints exactly one of the output codes to {@code System.out}:
     * <ul>
     *   <li>0 if verification succeeded</li>
     *   <li>1 if a {@link VariableException} or {@link MethodException}
     *   was thrown during parsing/validation</li>
     *   <li>2 if an {@link IOException} occurred while opening/reading the file</li>
     * </ul>
     *
     * @param args command-line arguments; expected to contain exactly one file path
     */
    public static void main(String[] args) {
        if (args==null || args.length != 1) {
            System.out.println(OUT_IO_ERROR);
            System.err.println(ARG_ERROR);
            return;
        }

        String fileName = args[0];

        if (fileName==null || !fileName.endsWith(END_NAME_FILE)) {
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
