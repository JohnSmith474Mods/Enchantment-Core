package johnsmith.enchantmentcore.api.util;

/**
 * Utility class for converting between standard integers and classical Roman numeral strings
 * in the range $[1, 3999]$.
 */
public final class RomanNumeralConverter {
    /** Descending lookup table of integer values corresponding to {@link #ROMAN_SYMBOLS}. */
    private static final int[] ROMAN_VALUES = {
            1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1
    };

    /** Descending lookup table of standard and subtractive Roman numeral symbols. */
    private static final String[] ROMAN_SYMBOLS = {
            "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
    };

    /** Private constructor to prevent instantiation of this utility class. */
    private RomanNumeralConverter() {}

    /**
     * Converts an integer into its classical Roman numeral representation.
     * <p>
     * Supports integers in the closed interval $[1, 3999]$. Values outside this range are returned
     * as standard base-10 Arabic numeral strings via {@link String#valueOf(int)}.
     *
     * @param number The integer to convert.
     * @return The Roman numeral string, or the Arabic numeral string if {@code number < 1} or {@code number > 3999}.
     */
    public static String toRoman(int number) {
        if (number < 1 || number > 3999) {
            return String.valueOf(number);
        }

        StringBuilder sb = new StringBuilder();
        int remaining = number;

        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (remaining >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i]);
                remaining -= ROMAN_VALUES[i];
            }
        }

        return sb.toString();
    }

    /**
     * Parses a Roman numeral string into its integer value.
     * <p>
     * Supports numerals in the classical range from {@code "I"} ($1$) to {@code "MMMCMXCIX"} ($3999$).
     *
     * @param romanNumeral The Roman numeral string to parse (case-insensitive).
     * @return The parsed integer value in $[1, 3999]$, or {@code -1} if the value is null, blank, or malformed.
     */
    public static int parse(String romanNumeral) {
        if (romanNumeral == null || romanNumeral.trim().isEmpty()) {
            return -1;
        }

        romanNumeral = romanNumeral.toUpperCase();
        int result = 0;
        int currentIndex = 0;

        for (int i = 0; i < ROMAN_SYMBOLS.length; i++) {
            while (romanNumeral.startsWith(ROMAN_SYMBOLS[i], currentIndex)) {
                result += ROMAN_VALUES[i];
                if (result > 3999) {
                    return -1;
                }
                currentIndex += ROMAN_SYMBOLS[i].length();
            }
        }

        if (currentIndex != romanNumeral.length()) {
            return -1;
        }

        return result;
    }
}