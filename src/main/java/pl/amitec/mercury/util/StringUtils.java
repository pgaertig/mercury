package pl.amitec.mercury.util;

public class StringUtils {
    private static final int DEFAULT_TRUNCATION_MAX_LENGTH = 40;

    public static String truncate(String input, int maxLength) {
        return truncate(input, maxLength, false);
    }

    public static String truncate(String input, boolean showRemaining) {
        return truncate(input, DEFAULT_TRUNCATION_MAX_LENGTH, showRemaining);
    }

    public static String truncate(String input) {
        return truncate(input, DEFAULT_TRUNCATION_MAX_LENGTH);
    }

    public static String truncate(String input, int maxLength, boolean showRemaining) {
        if (input == null) {
            return null;
        }

        if ((showRemaining && input.length() - maxLength <= 10) || input.length() - maxLength <= 3) {
            return input;
        }

        String truncatedString = input.substring(0, maxLength) + "…";
        if (showRemaining) {
            int remainingCharacters = input.length() - maxLength;
            truncatedString += "<" + remainingCharacters + " more>";
        }

        return truncatedString;
    }
}
