package johnsmith.enchantmentcore.api.tooltip.formatting;

import net.minecraft.network.chat.Style;

/**
 * Functional contract for mutating rich text {@link Style}s during dynamic enchantment tooltip rendering.
 * <p>
 * Registered in {@link johnsmith.enchantmentcore.api.tooltip.DynamicDescriptionFormatter} and invoked
 * when parsing style tags of the form {@code <modifier=arg:content>} or {@code <modifier:content>}.
 */
@FunctionalInterface
public interface StyleModifier {
    /**
     * Applies a style transformation to the active {@link Style} on top of the formatting stack.
     *
     * @param currentStyle The current inherited {@link Style} in the parsing hierarchy.
     * @param arg          The optional string argument parsed after {@code '='} in the opening tag, or {@code null} if omitted.
     * @return The new derived {@link Style} to push onto the style stack.
     */
    Style apply(Style currentStyle, String arg);
}