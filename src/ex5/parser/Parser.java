package ex5.parser;

import ex5.handleMethods.MethodParser;
import ex5.handleVariables.Variable;
import ex5.handleVariables.VariableException;
import ex5.handleVariables.VariableLine;
import ex5.main.Sjavac;

import java.io.BufferedReader;
import java.io.IOException;

public class Parser {

    private final BufferedReader bufferedReader;

    public Parser(BufferedReader bufferedReader) {
        this.bufferedReader=bufferedReader;
    }

    public int parse() throws IOException,
            VariableException {

        String line;
        while((line=this.bufferedReader.readLine()) != null){
            TypeLineOptions lineType = Line.getLineType(line);

            if(lineType == TypeLineOptions.commentLine){
                continue;
            }else if(lineType == TypeLineOptions.methodLine){

                MethodParser methodParser = new MethodParser(line);

            }else if(lineType == TypeLineOptions.variableLine){

                VariableLine variableLine = new VariableLine(line);
                int result = variableLine.compileVariableLine();
            }
            else{
                return Sjavac.OUT_SYNTAX_ERROR;
            }
        }
        return Sjavac.OUT_SUCCESS;
    }

}
