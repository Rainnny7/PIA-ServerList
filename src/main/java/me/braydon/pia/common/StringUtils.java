package me.braydon.pia.common;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author Braydon
 */
@UtilityClass
public final class StringUtils {
    /**
     * Capitalize the first character
     * in each word in the given string.
     *
     * @param input the input to capitalize
     * @return the capitalized string
     */
    @NonNull
    public static String capitalizeFully(@NonNull String input, char delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String part : input.split(String.valueOf(delimiter))) {
            builder.append(part.length() <= 2 ? part.toUpperCase() : capitalize(part)).append(delimiter);
        }
        return builder.substring(0, builder.length() - 1).replace(delimiter, ' ');
    }

    /**
     * Capitalize the first character in the given string.
     *
     * @param input the input to capitalize
     * @return the capitalized string
     */
    @NonNull
    public static String capitalize(@NonNull String input) {
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }
}