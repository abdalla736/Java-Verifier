package ex5.validator;

import java.util.regex.Pattern;

/**
 * Utility class providing shared validation logic for S-Java type checking.
 *
 * <p>{@code ValidationUtils} centralizes regular expressions and helper methods
 * used to validate literals, argument compatibility, and type assignability.
 * This avoids duplication of validation logic across variable assignments,
 * method calls, and control-flow conditions.</p>
 *
 * <p>This class is <b>stateless</b> and <b>non-instantiable</b>; all members are static.</p>
 */
public class ValidationUtils {

    /** Matches a valid integer literal (optional sign). */
    public static final Pattern INT_PATTERN = Pattern.compile("^[+-]?\\d+$");

    /** Matches a valid double literal, including integer-like doubles. */
    public static final Pattern DOUBLE_PATTERN =
            Pattern.compile("^[+-]?(?:\\d+\\.\\d*|\\d*\\.\\d+|\\d+)$");

    /** Matches a boolean literal ({@code true} or {@code false}). */
    public static final Pattern BOOL_PATTERN = Pattern.compile("^(true|false)$");

    /** Matches a single-character literal enclosed in single quotes. */
    public static final Pattern CHAR_PATTERN = Pattern.compile("^'[^']'$");

    /** Matches a string literal enclosed in double quotes. */
    public static final Pattern STR_PATTERN = Pattern.compile("^\"[^\"]*\"$");

    /** Supported S-Java primitive and reference types. */
    public static final String TYPE = "int|double|boolean|char|String";



    /**
     * Validates whether a given argument literal is compatible with a parameter type.
     *
     * <p>This method checks only <b>literal arguments</b> (not variable references)
     * and follows the S-Java type promotion rules:</p>
     *
     * <ul>
     *   <li>{@code int} ← integer literal</li>
     *   <li>{@code double} ← integer or double literal</li>
     *   <li>{@code boolean} ← boolean, integer, or double literal</li>
     *   <li>{@code char} ← character literal</li>
     *   <li>{@code String} ← string literal</li>
     * </ul>
     *
     * @param arg
     *     The argument literal as it appears in source code.
     *
     * @param paramType
     *     The expected parameter type.
     *
     * @return
     *     {@code true} if the argument is a valid literal compatible with the parameter type;
     *     {@code false} otherwise.
     */
    public static boolean validateArgument(String arg,
                                  String paramType)
            {

        return (paramType.equals("int") && INT_PATTERN.matcher(arg).matches())
                        || (paramType.equals("double") &&
                        (DOUBLE_PATTERN.matcher(arg).matches() || INT_PATTERN.matcher(arg).matches()))
                        || (paramType.equals("boolean") &&
                        (BOOL_PATTERN.matcher(arg).matches() || DOUBLE_PATTERN.matcher(arg).matches()
                                || INT_PATTERN.matcher(arg).matches()))
                        || (paramType.equals("char") && CHAR_PATTERN.matcher(arg).matches())
                        || (paramType.equals("String") && STR_PATTERN.matcher(arg).matches());

    }

    /**
     * Determines whether a value of one type can be assigned to a target type.
     *
     * <p>This method is used when assigning variables or passing variables
     * as method arguments (non-literal cases).</p>
     *
     * <p>Supported compatibility rules:</p>
     * <ul>
     *   <li>Exact type match</li>
     *   <li>{@code int → double}</li>
     *   <li>{@code int → boolean}</li>
     *   <li>{@code double → boolean}</li>
     * </ul>
     *
     * @param targetType
     *     The type of the assignment target.
     *
     * @param sourceType
     *     The type of the value being assigned.
     *
     * @return
     *     {@code true} if the source type is assignable to the target type;
     *     {@code false} otherwise.
     */
    public static boolean isTypeCompatible(String targetType, String sourceType) {
        if (targetType.equals(sourceType)) return true;
        if (targetType.equals("double") && sourceType.equals("int")) return true;
        return targetType.equals("boolean") && (sourceType.equals("int") || sourceType.equals("double"));
    }
}
