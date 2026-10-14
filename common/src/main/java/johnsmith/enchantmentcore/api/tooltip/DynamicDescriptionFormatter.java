package johnsmith.enchantmentcore.api.tooltip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import johnsmith.enchantmentcore.api.tooltip.formatting.StyleModifier;
import johnsmith.enchantmentcore.api.tooltip.formatting.ValueModifier;
import johnsmith.enchantmentcore.api.util.RomanNumeralConverter;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Parses and formats dynamic enchantment descriptions using registered {@link ValueModifier}s and {@link StyleModifier}s.
 * <p>
 * Evaluates nested tag syntax in three sequential passes:
 * <ol>
 *   <li>Pointer interpolation ({@code <val:key>} and {@code <array:key>}) via {@link DynamicDescriptionParser}.</li>
 *   <li>Inside-out mathematical value transformation (e.g., {@code <percent:<val:key>>}, {@code <fixed:2:<div:20:<val:ticks>>>}).</li>
 *   <li>Linear stack-based rich text style application (e.g., {@code <rgb=#FF5555:content>}, {@code <obf:text>}, {@code <sga:runes>}).</li>
 * </ol>
 */
public class DynamicDescriptionFormatter {
    /** Registry of global mathematical value modifiers keyed by tag name. */
    private static final Map<String, ValueModifier> VALUE_MODIFIERS = new HashMap<>();

    /** Registry of global rich text style modifiers keyed by tag name. */
    private static final Map<String, StyleModifier> GLOBAL_STYLE_MODIFIERS = new HashMap<>();

    /** Lazily compiled regex pattern matching innermost value modifier tags. */
    private static Pattern valueModifierPattern = null;

    static {
        registerValueModifier("int", (arg, val) -> String.valueOf(Math.round(val)));
        registerValueModifier("percent", (arg, val) -> Math.round(val * 100) + "%");
        registerValueModifier("roman", (arg, val) -> RomanNumeralConverter.toRoman(Math.round(val)));
        registerValueModifier("div", (arg, val) -> formatNumber(val / Float.parseFloat(arg)));
        registerValueModifier("add", (arg, val) -> formatNumber(val + Float.parseFloat(arg)));
        registerValueModifier("ceil", (arg, val) -> {
            int dec = arg != null ? Integer.parseInt(arg) : 0;
            double fac = Math.pow(10, dec);
            return formatNumber((float) (Math.ceil(val * fac) / fac));
        });
        registerValueModifier("floor", (arg, val) -> {
            int dec = arg != null ? Integer.parseInt(arg) : 0;
            double fac = Math.pow(10, dec);
            return formatNumber((float) (Math.floor(val * fac) / fac));
        });
        registerValueModifier("fixed", (arg, val) -> {
            int dec = arg != null ? Integer.parseInt(arg) : 0;
            return String.format(java.util.Locale.US, "%." + dec + "f", val);
        });

        registerStyleModifier("obf", (style, arg) -> style.withObfuscated(true));
        registerStyleModifier("sga", (style, arg) -> style.withFont(new FontDescription.Resource(ResourceLocation.withDefaultNamespace("alt"))));
        registerStyleModifier("rgb", (style, arg) -> {
            if (arg == null) return style;
            if (arg.startsWith("#")) {
                try { return style.withColor((int) Long.parseLong(arg.substring(1), 16)); } catch (Exception ignored) {}
            } else {
                try {
                    String[] rgb = arg.split(",");
                    if (rgb.length == 3) {
                        int r = Integer.parseInt(rgb[0].trim());
                        int g = Integer.parseInt(rgb[1].trim());
                        int b = Integer.parseInt(rgb[2].trim());
                        return style.withColor((r << 16) | (g << 8) | b);
                    }
                } catch (Exception ignored) {}
            }
            return style;
        });
        registerStyleModifier("argb", (style, arg) -> {
            if (arg != null && arg.startsWith("#")) {
                try { return style.withColor((int) Long.parseLong(arg.substring(1), 16)); } catch (Exception ignored) {}
            }
            return style;
        });
    }

    /**
     * Registers a global {@link ValueModifier} available to all dynamic descriptions.
     * Invalidates the cached regex pattern so it is rebuilt on the next parse call.
     *
     * @param name     The command identifier used in description tags (e.g., {@code "percent"}).
     * @param modifier The {@link ValueModifier} implementation.
     */
    public static void registerValueModifier(String name, ValueModifier modifier) {
        VALUE_MODIFIERS.put(name, modifier);
        valueModifierPattern = null;
    }

    /**
     * Registers a global {@link StyleModifier} available to all dynamic descriptions.
     *
     * @param name     The style command identifier used in description tags (e.g., {@code "rgb"}).
     * @param modifier The {@link StyleModifier} implementation.
     */
    public static void registerStyleModifier(String name, StyleModifier modifier) {
        GLOBAL_STYLE_MODIFIERS.put(name, modifier);
    }

    private static Pattern getValueModifierPattern() {
        if (valueModifierPattern == null) {
            if (VALUE_MODIFIERS.isEmpty()) {
                valueModifierPattern = Pattern.compile("<(?!):([^><]+)>");
            } else {
                String keys = String.join("|", VALUE_MODIFIERS.keySet());
                valueModifierPattern = Pattern.compile("<(" + keys + ")(?::([^><:]+))?:([^><]+)>");
            }
        }
        return valueModifierPattern;
    }

    /**
     * Evaluates a raw description string and constructs a formatted rich text {@link MutableComponent}.
     * <p>
     * Executes pointer resolution, nested mathematical value modifications, and rich style tag
     * applications sequentially.
     *
     * @param rawDescription The unformatted translation string containing dynamic tag syntax.
     * @param currentLevel   The active enchantment level for {@code <val:key>} pointer evaluation.
     * @param maxLevel       The maximum enchantment level for {@code <array:key>} bracket generation.
     * @param args           The map of dynamic {@link LevelBasedValue} pointers extracted from the enchantment.
     * @param baseStyle      The foundational {@link Style} applied to unstyled root text segments.
     * @param contextStyles  An optional map of context-specific {@link StyleModifier}s that override global style modifiers.
     * @return The fully evaluated and styled {@link MutableComponent}.
     */
    public static MutableComponent parseAndFormat(String rawDescription, int currentLevel, int maxLevel, Map<String, LevelBasedValue> args, Style baseStyle, Map<String, StyleModifier> contextStyles) {
        String evaluatedText = rawDescription;
        if (args != null && !args.isEmpty()) {
            evaluatedText = DynamicDescriptionParser.parse(rawDescription, currentLevel, maxLevel, args);
        }

        String modifierEvaluatedText = processValueModifiers(evaluatedText);
        return processStyleModifiers(modifierEvaluatedText, baseStyle, contextStyles);
    }

    private static String processValueModifiers(String input) {
        Matcher matcher = getValueModifierPattern().matcher(input);
        boolean changed = false;
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String cmd = matcher.group(1);
            String arg = matcher.group(2);
            String content = matcher.group(3);
            String result = applyCommand(cmd, arg, content);
            matcher.appendReplacement(sb, result);
            changed = true;
        }
        matcher.appendTail(sb);

        if (changed) {
            return processValueModifiers(sb.toString());
        }
        return input;
    }

    private static String applyCommand(String cmd, String arg, String content) {
        ValueModifier modifier = VALUE_MODIFIERS.get(cmd);
        if (modifier == null) return "<" + cmd + (arg != null ? ":" + arg : "") + ":" + content + ">";

        if (content.startsWith("[") && content.endsWith("]")) {
            String inner = content.substring(1, content.length() - 1);
            String[] parts = inner.split(",\\s*");
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < parts.length; i++) {
                sb.append(applySingleModifier(modifier, arg, parts[i].trim()));
                if (i < parts.length - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
        return applySingleModifier(modifier, arg, content);
    }

    private static String applySingleModifier(ValueModifier modifier, String arg, String value) {
        try {
            float fVal = Float.parseFloat(value);
            return modifier.apply(arg, fVal);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static String formatNumber(float value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.valueOf(value).replaceAll("0*$", "").replaceAll("\\.$", "");
        }
    }

    private static MutableComponent processStyleModifiers(String input, Style baseStyle, Map<String, StyleModifier> contextModifiers) {
        MutableComponent root = Component.empty();
        Stack<Style> styleStack = new Stack<>();
        styleStack.push(baseStyle);

        int depth = 0;
        Stack<Integer> colorTagDepths = new Stack<>();
        StringBuilder currentText = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '<') {
                int colonIdx = input.indexOf(':', i);
                int bracketIdx = input.indexOf('>', i);

                if (colonIdx != -1 && (bracketIdx == -1 || colonIdx < bracketIdx)) {
                    String fullTag = input.substring(i + 1, colonIdx);
                    String cmd = fullTag;
                    String arg = null;

                    int eqIdx = fullTag.indexOf('=');
                    if (eqIdx != -1) {
                        cmd = fullTag.substring(0, eqIdx);
                        arg = fullTag.substring(eqIdx + 1);
                    }

                    StyleModifier mod = contextModifiers != null ? contextModifiers.get(cmd) : null;
                    if (mod == null) mod = GLOBAL_STYLE_MODIFIERS.get(cmd);

                    if (mod != null) {
                        Style newStyle = mod.apply(styleStack.peek(), arg);
                        flushText(root, currentText, styleStack.peek());
                        depth++;
                        colorTagDepths.push(depth);
                        styleStack.push(newStyle);
                        i = colonIdx;
                        continue;
                    }
                }
                depth++;
                currentText.append('<');
            } else if (input.charAt(i) == '>') {
                if (!colorTagDepths.isEmpty() && colorTagDepths.peek() == depth) {
                    flushText(root, currentText, styleStack.peek());
                    styleStack.pop();
                    colorTagDepths.pop();
                } else {
                    currentText.append('>');
                }
                depth = Math.max(0, depth - 1);
            } else {
                currentText.append(input.charAt(i));
            }
        }
        flushText(root, currentText, styleStack.peek());
        return root;
    }

    private static void flushText(MutableComponent root, StringBuilder text, Style style) {
        if (text.length() > 0) {
            root.append(Component.literal(text.toString()).withStyle(style));
            text.setLength(0);
        }
    }

    /**
     * Wraps a styled rich text {@link Component} into multiple lines constrained by a maximum character count,
     * preserving individual token styles across line breaks.
     *
     * @param root         The source rich text {@link Component} to wrap.
     * @param maxChars     The maximum character length permitted per line.
     * @param nestingDepth The number of leading spaces prepended to each wrapped line.
     * @return A list of wrapped line {@link Component}s.
     */
    public static List<Component> wrapRich(Component root, int maxChars, int nestingDepth) {
        List<Component> lines = new ArrayList<>();
        MutableComponent[] currentLine = {Component.empty()};
        int[] currentLength = {0};
        String indent = " ".repeat(nestingDepth);

        root.visit((style, string) -> {
            String[] tokens = string.split("(?<=\\s)|(?=\\s)");
            for (String token : tokens) {
                if (token.isEmpty()) continue;

                if (currentLength[0] + token.length() > maxChars && currentLength[0] > 0) {
                    lines.add(Component.literal(indent).append(currentLine[0]));
                    currentLine[0] = Component.empty();
                    currentLength[0] = 0;
                    if (token.isBlank()) continue;
                }

                currentLine[0].append(Component.literal(token).withStyle(style));
                currentLength[0] += token.length();
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        if (currentLength[0] > 0) {
            lines.add(Component.literal(indent).append(currentLine[0]));
        }
        return lines;
    }
}