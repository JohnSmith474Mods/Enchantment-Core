package johnsmith.enchantmentcore.api.tooltip;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class responsible for injecting evaluated LevelBasedValues into translation strings.
 * <p>
 * Supports two distinct syntaxes within language files:
 * - {@code <val:key>} : Calculates the value for the *current* level.
 * - {@code <array:key>} : Generates a bracketed array {@code [lvl 1, lvl 2, ...]} up to the enchantment's max level.
 */
public class DynamicDescriptionParser {

    private static final Pattern VAL_PATTERN = Pattern.compile("<val:([^>]+)>");
    private static final Pattern ARRAY_PATTERN = Pattern.compile("<array:([^>]+)>");

    /**
     * Parses a translated string and injects the evaluated arguments.
     *
     * @param translatedBase The raw string containing the pointer syntax.
     * @param currentLevel   The level at which to evaluate standard {@code <val>} pointers.
     * @param maxLevel       The maximum level of the enchantment, used to construct {@code <array>} brackets.
     * @param args           The map of String pointers to LevelBasedValues (typically fetched from the enchantment's DataComponents).
     * @return The formatted string ready for rendering.
     */
    public static String parse(String translatedBase, int currentLevel, int maxLevel, Map<String, LevelBasedValue> args) {
        // If no arguments are defined, return the base string untouched for performance
        if (args == null || args.isEmpty()) {
            return translatedBase;
        }

        // 1. Process <val:...> syntax
        Matcher valMatcher = VAL_PATTERN.matcher(translatedBase);
        StringBuilder sb = new StringBuilder();

        while (valMatcher.find()) {
            String key = valMatcher.group(1);
            LevelBasedValue lbv = args.get(key);

            if (lbv != null) {
                float val = lbv.calculate(currentLevel);
                valMatcher.appendReplacement(sb, formatNumber(val));
            } else {
                // If the key is missing, leave the syntax visible so the dev can debug it
                valMatcher.appendReplacement(sb, valMatcher.group(0));
            }
        }
        valMatcher.appendTail(sb);
        String result = sb.toString();

        // 2. Process <array:...> syntax
        Matcher arrMatcher = ARRAY_PATTERN.matcher(result);
        sb = new StringBuilder();

        while (arrMatcher.find()) {
            String key = arrMatcher.group(1);
            LevelBasedValue lbv = args.get(key);

            if (lbv != null) {
                StringBuilder arrStr = new StringBuilder("[");

                for (int i = 1; i <= maxLevel; i++) {
                    arrStr.append(formatNumber(lbv.calculate(i)));
                    if (i < maxLevel) arrStr.append(", ");
                }
                arrStr.append("]");
                arrMatcher.appendReplacement(sb, arrStr.toString());
            } else {
                arrMatcher.appendReplacement(sb, arrMatcher.group(0));
            }
        }
        arrMatcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Formats floats cleanly, dropping unnecessary `.0` decimals.
     */
    private static String formatNumber(float value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            // Strip trailing zeroes and trailing decimals if they exist
            return String.valueOf(value).replaceAll("0*$", "").replaceAll("\\.$", "");
        }
    }
}