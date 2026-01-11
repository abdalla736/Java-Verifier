package ex5.handleVariables;

import ex5.main.Sjavac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableLine {

    private static final String NAME = "(?:[A-Za-z]\\w*|_[A-Za-z0-9]\\w*)";
    private static final String TYPE = "(?:int|double|boolean|char|String)";

    private static final String INT_LIT = "[+-]?\\d+";
    private static final String DOUBLE_LIT = "[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)";
    private static final String BOOL_LIT = "(?:true|false)";
    private static final String CHAR_LIT = "'[^']'";
    private static final String STR_LIT = "\"[^\"]*\"";

    // One-token RHS only (operators not supported)
    private static final String VALUE =
            "(?:" + STR_LIT + "|" + CHAR_LIT + "|" + BOOL_LIT + "|" + DOUBLE_LIT + "|" + INT_LIT + "|" + NAME + ")";

    private static final Pattern DECL_HEAD =
            Pattern.compile("^\\s*(final\\s+)?(" + TYPE + ")\\s+(.+?)\\s*$");
    private static final Pattern DECL_ITEM =
            Pattern.compile("^(" + NAME + ")(?:\\s*=\\s*(" + VALUE + "))?$");

    private static final Pattern ASSIGN_ITEM =
            Pattern.compile("^(" + NAME + ")\\s*=\\s*(" + VALUE + ")$");


    private final String varLine;

    public VariableLine(String varLine) {
        this.varLine=varLine;
    }

    public int compileVariableLine(HashMap<String,Variable> variables) throws VariableException {
        String line=varLine==null?"":varLine.trim();

        if(!line.endsWith(";")){
            throw new VariableException("missing ';'.");
        }

        if(line.contains("//")){
            throw new VariableException("comment inside variable line");
        }

        line=line.substring(0,line.length()-1).trim();

        if(line.isEmpty()){
            throw new VariableException("empty line");
        }

        Matcher head = DECL_HEAD.matcher(line);

        if(head.matches()){
            boolean isFinal=head.group(1)!=null;
            String type=head.group(2);
            String rest=head.group(3).trim();

            String[] parts=rest.split("\\s*,\\s*");
            if(parts.length == 0){
                throw new VariableException("empty declaration list");
            }
            for(String part:parts){
                Matcher item = DECL_ITEM.matcher(part);

                if(!item.matches()){
                    throw new VariableException("invalid declaration");
                }

                String name=item.group(1);
                String itemValue=item.group(2);

                if(variables.containsKey(name)){
                    throw new VariableException("duplicate variable name");
                }

                if(isFinal && itemValue==null){
                    throw new VariableException("final must be initialized");
                }

                variables.put(name,new Variable(itemValue!=null, type, isFinal));
            }
            return Sjavac.OUT_SUCCESS;
        }

        String[] parts=line.split("\\s*,\\s*");
        if(parts.length == 0){
            throw new VariableException("empty declaration list");
        }

        for(String part:parts){
            Matcher item = ASSIGN_ITEM.matcher(part);
            if(!item.matches()){
                throw new VariableException("bad assignment");
            }

            String name=item.group(1);
            String itemValue=item.group(2);

            Variable variable=variables.get(name);
            if(variable==null){
                throw new VariableException("assign to undeclared variable");
            }
            if(variable.isFinal()){
                throw new VariableException("final must be initialized");
            }
            variable.setInitialized(true);
        }
        return Sjavac.OUT_SUCCESS;
    }
}
