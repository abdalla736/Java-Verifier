package ex5.handleVariables;

import ex5.main.Sjavac;

public class VariableLine {

    private final String varLine;

    public VariableLine(String varLine) {
        this.varLine=varLine;
    }

    public int compileVariableLine(){
        String line=varLine==null?"":varLine.trim();

        if(!line.endsWith(";")){
            return Sjavac.OUT_SYNTAX_ERROR;
        }

        if(line.contains("//")){
            return Sjavac.OUT_SYNTAX_ERROR;
        }

        line=line.substring(0,line.length()-1).trim();



    }
}
