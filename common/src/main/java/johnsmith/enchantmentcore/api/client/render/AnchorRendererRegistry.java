package johnsmith.enchantmentcore.api.client.render;

import java.util.HashMap;
import java.util.Map;

import johnsmith.enchantmentcore.api.enchantment.spellfield.entity.AnchorVisualConfig;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

/**
 * Client-side registry mapping unique model identifiers to custom {@link AnchorModelRenderer} delegates.
 * <p>
 * Allows dependent mods to register bespoke 3D entity models or procedural geometry renderers for
 * spell field anchors referenced via {@link AnchorVisualConfig#modelId()}.
 */
public final class AnchorRendererRegistry {
    /** Internal lookup table mapping model resource locations to their corresponding renderer delegates. */
    private static final Map<Identifier, AnchorModelRenderer> RENDERERS = new HashMap<>();

    /** Private constructor to prevent instantiation of this static registry utility. */
    private AnchorRendererRegistry() {}

    /**
     * Registers a custom model renderer under the specified resource location identifier.
     *
     * @param modelId  The unique {@link Identifier} matching the anchor's {@code model_id} configuration.
     * @param renderer The {@link AnchorModelRenderer} delegate responsible for rendering the custom model.
     */
    public static void register(Identifier modelId, AnchorModelRenderer renderer) {
        RENDERERS.put(modelId, renderer);
    }

    /**
     * Retrieves the registered model renderer associated with the given identifier.
     *
     * @param modelId The unique {@link Identifier} identifier of the model.
     * @return The bound {@link AnchorModelRenderer} delegate, or {@code null} if no renderer is registered for the identifier.
     */
    public static @Nullable AnchorModelRenderer get(Identifier modelId) {
        return RENDERERS.get(modelId);
    }
}