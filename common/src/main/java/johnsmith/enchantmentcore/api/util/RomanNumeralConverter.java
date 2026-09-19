package johnsmith.enchantmentcore.api.util;

/**
 * Utility class for translating integers into Roman numerals.
 */
public final class RomanNumeralConverter {

    /**
     * Lookup table for Roman numeral conversion values.
     */
    private static final int[] ROMAN_VALUES = {
            1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1
    };

    /**
     * Lookup table for Roman numeral conversion symbols.
     */
    private static final String[] ROMAN_SYMBOLS = {
            "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
    };

    private RomanNumeralConverter() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts an integer to its Roman numeral representation.
     * Supports numbers from 1 to 3999. Values outside this range return as standard Arabic numerals.
     *
     * @param number The integer to convert.
     * @return The Roman numeral string, or the Arabic numeral string if out of bounds.
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
     * Parses a Roman numeral string into its integer representation.
     * Supports numerals in the classical range of 'I' to 'MMMCMXCIX'.
     *
     * @param romanNumeral The Roman numeral string to parse.
     * @return The integer value of the Roman numeral, or -1 if the input is invalid.
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