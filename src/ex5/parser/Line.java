package ex5.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Line {
    private static final String VAR_REGEX = "^(int|double|String|boolean|char)\\s+" +
            "(([a-zA-Z])\\w*|_[a-zA-Z0-9]\\w*)"+
            "(\\s*=\\s*[^;])?;";



    public static TypeLineOptions getLineType(String line){
        Pattern varPattern= Pattern.compile(VAR_REGEX);
        Matcher varMatcher = varPattern.matcher(line);

        if(varMatcher.matches()){ //then variable line
            return TypeLineOptions.variableLine;
        }
        return null;
    }
}
