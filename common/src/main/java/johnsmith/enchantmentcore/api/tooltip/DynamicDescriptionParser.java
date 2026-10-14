package johnsmith.enchantmentcore.api.tooltip;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Utility class responsible for interpolating evaluated {@link LevelBasedValue} pointers into
 * localized enchantment description strings.
 * <p>
 * Supports two distinct placeholder syntaxes within language files:
 * <ul>
 *   <li>{@code <val:key>}: Evaluates the bound {@link LevelBasedValue} at the enchantment's {@code currentLevel}.</li>
 *   <li>{@code <array:key>}: Generates a bracketed progression list {@code [lvl 1, lvl 2, ..., maxLevel]} up to {@code maxLevel}.</li>
 * </ul>
 */
public class DynamicDescriptionParser {
    /** Regex pattern matching single-level {@code <val:key>} placeholders. */
    private static final Pattern VAL_PATTERN = Pattern.compile("<val:([^>]+)>");

    /** Regex pattern matching multi-level {@code <array:key>} placeholders. */
    private static final Pattern ARRAY_PATTERN = Pattern.compile("<array:([^>]+)>");

    /**
     * Parses a raw translation string and replaces all {@code <val:key>} and {@code <array:key>}
     * placeholders using the provided map of {@link LevelBasedValue} arguments.
     * <p>
     * If a referenced key is missing from {@code args}, the raw placeholder token is left intact in
     * the output string to aid debugging.
     *
     * @param translatedBase The raw localized string containing pointer placeholders.
     * @param currentLevel   The active level of the enchantment used to evaluate {@code <val:key>} tags.
     * @param maxLevel       The maximum level of the enchantment used to evaluate {@code <array:key>} tags.
     * @param args           The map of string pointer IDs to {@link LevelBasedValue} instances (from {@link johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents#DESCRIPTION_ARGS}).
     * @return The interpolated string with all resolved numeric values formatted cleanly.
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
     * Formats floating-point numbers cleanly, stripping trailing zeroes and unnecessary {@code .0} decimal suffixes.
     *
     * @param value The float value to format.
     * @return The formatted numeric string.
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