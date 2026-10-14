package johnsmith.enchantmentcore.api.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Utility class for word-wrapping plain or uniformly styled {@link Component}s into multiple lines
 * based on character count limits and optional indentation depth.
 */
public final class TooltipLineWrapper {
    /** Two-space indentation unit applied per nesting level. */
    private static final String INDENT_STRING = "  ";

    /** Private constructor to prevent instantiation of this utility class. */
    private TooltipLineWrapper() {}

    /**
     * Wraps a text {@link Component} to the specified maximum line length with no indentation.
     *
     * @param description The source text {@link Component} to wrap.
     * @param lineLength  The maximum number of characters permitted per line before wrapping.
     * @return A list of wrapped line {@link Component}s preserving the root component's {@link Style}.
     */
    public static List<Component> wrap(Component description, int lineLength) {
        return wrap(description, lineLength, 0);
    }

    /**
     * Wraps a text {@link Component} to the specified maximum line length, prepending {@code nestingDepth}
     * levels of two-space indentation to each resulting line.
     *
     * @param description  The source text {@link Component} to wrap.
     * @param lineLength   The maximum number of characters permitted per line (excluding indentation).
     * @param nestingDepth The number of two-space indentation levels prepended to each line.
     * @return A list of wrapped, indented line {@link Component}s preserving the root component's {@link Style}.
     */
    public static List<Component> wrap(Component description, int lineLength, int nestingDepth) {
        List<Component> wrappedLines = new ArrayList<>();
        String descriptionString = description.getString();
        Style descriptionStyle = description.getStyle();

        if (descriptionString.isEmpty()) {
            return wrappedLines;
        }

        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < nestingDepth; i++) {
            indentBuilder.append(INDENT_STRING);
        }
        Component indentComponent = Component.literal(indentBuilder.toString());

        String remainingString = descriptionString;
        while (remainingString.length() > lineLength) {
            String line;
            int wrapAt = remainingString.lastIndexOf(' ', lineLength);

            if (wrapAt <= 0) {
                line = remainingString.substring(0, lineLength);
                remainingString = remainingString.substring(lineLength);
            } else {
                line = remainingString.substring(0, wrapAt);
                remainingString = remainingString.substring(wrapAt + 1);
            }

            wrappedLines.add(indentComponent.copy().append(Component.literal(line)).withStyle(descriptionStyle));
        }

        if (!remainingString.isEmpty()) {
            wrappedLines.add(indentComponent.copy().append(Component.literal(remainingString)).withStyle(descriptionStyle));
        }

        return wrappedLines;
    }
}