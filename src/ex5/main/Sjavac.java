package ex5.main;


import ex5.parser.Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Sjavac {

    private static final String INCORRECT_NUM_ARG = "num arguments invalid";
    public static final int OUT_SUCCESS = 0;
    public static final int OUT_SYNTAX_ERROR = 1;
    public static final int OUT_IO_ERROR = 2;


    public static void main(String[] args) {
        //num arguments is incorrect
        if(args.length!=1){
            System.out.println(OUT_IO_ERROR);
            System.err.println(INCORRECT_NUM_ARG);
            return;
        }

        String fileName=args[0];

        try (BufferedReader buffer = new BufferedReader(new FileReader(fileName))){
            Parser parser=new Parser(buffer);
            int result = parser.parse();
            System.out.println(result);


        }catch(IOException e){
            System.err.println(""+e.getMessage());
            System.out.println(Sjavac.OUT_IO_ERROR);

        }

    }
}
