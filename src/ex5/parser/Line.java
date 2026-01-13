package ex5.parser;

import ex5.parser.TypeLineOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Line {
    private static final String NAME = "([a-zA-Z]\\w*|_[a-zA-Z0-9]\\w*)";
    private static final String TYPE = "(int|double|String|boolean|char)";

    private static final Pattern EMPTY_PATTERN = Pattern.compile("^\\s*$");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^\\s*void\\s+" + NAME + "\\s*\\(.*\\)\\s*\\{\\s*$");
    private static final Pattern VAR_PATTERN = Pattern.compile("^\\s*(?:final\\s+)?" + TYPE + "\\s+.+;\\s*$");
    private static final Pattern IF_PATTERN = Pattern.compile("^\\s*if\\s*\\(.+\\)\\s*\\{\\s*$");
    private static final Pattern WHILE_PATTERN = Pattern.compile("^\\s*while\\s*\\(.+\\)\\s*\\{\\s*$");
    private static final Pattern RETURN_PATTERN = Pattern.compile("^\\s*return\\s*;\\s*$");
    private static final Pattern CLOSING_PATTERN = Pattern.compile("^\\s*\\}\\s*$");
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile("^\\s*[a-zA-Z]\\w*\\s*\\(.*\\)\\s*;\\s*$");
    private static final Pattern ASSIGN_PATTERN = Pattern.compile("^\\s*[a-zA-Z]\\w*\\s*=.+;\\s*$");

    public static TypeLineOptions getLineType(String line) {
        if (line.trim().startsWith("//")) {
            return TypeLineOptions.commentLine;
        }

        if (EMPTY_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.emptyLine;
        }

        if (METHOD_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.methodLine;
        }

        if (RETURN_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.returnLine;
        }

        if (CLOSING_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.closingBrackets;
        }

        if (IF_PATTERN.matcher(line).matches() || WHILE_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.ifWhileLine;
        }

        if (VAR_PATTERN.matcher(line).matches() || ASSIGN_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.variableLine;
        }

        if (METHOD_CALL_PATTERN.matcher(line).matches()) {
            return TypeLineOptions.methodCallLine;
        }

        return null;
    }
}