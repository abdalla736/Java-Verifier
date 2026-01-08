package ex5.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Line {
    private static final String VAR_REGEX = "^(int|double|String|boolean|char)\\s+" +
            "(([a-zA-Z])\\w*|_[a-zA-Z0-9]\\w*)"+
            "(\\s*=\\s*[^;])?;";

    public static TypeLineOptions getLineType(String line){
        switch(line){
            Pattern pattern= Pattern.compile(VAR_REGEX);
            Matcher matcher = pattern.matcher(line);

            if(matcher.matches()){//then variable line
                return TypeLineOptions.variableLine;
            }
        }
    }
}
