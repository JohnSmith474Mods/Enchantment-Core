package johnsmith.enchantmentcore.api.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for wrapping text components into multiple lines based on character limits.
 */
public final class TooltipLineWrapper {
    private static final String INDENT_STRING = "  ";

    private TooltipLineWrapper() {}

    /**
     * Wraps a text component to the specified line length without indentation.
     *
     * @param description The source text component.
     * @param lineLength  The maximum character count per line.
     * @return A list of wrapped text components.
     */
    public static List<Component> wrap(Component description, int lineLength) {
        return wrap(description, lineLength, 0);
    }

    /**
     * Wraps a text component to the specified line length with applied indentation.
     *
     * @param description  The source text component.
     * @param lineLength   The maximum character count per line.
     * @param nestingDepth The indentation multiplier applied to each line.
     * @return A list of wrapped text components.
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