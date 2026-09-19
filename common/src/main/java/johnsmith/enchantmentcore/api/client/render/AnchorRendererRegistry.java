package johnsmith.enchantmentcore.api.client.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Registry mapping unique model identifiers to custom {@link AnchorModelRenderer} implementations.
 * <p>
 * Provides global registration and retrieval mechanics for anchor rendering delegates.
 */
public final class AnchorRendererRegistry {

    private static final Map<ResourceLocation, AnchorModelRenderer> RENDERERS = new HashMap<>();

    private AnchorRendererRegistry() {}

    /**
     * Registers a custom model renderer under the specified identifier.
     *
     * @param modelId  The unique {@link ResourceLocation} matching the anchor's model data configuration.
     * @param renderer The {@link AnchorModelRenderer} delegate responsible for rendering the model.
     */
    public static void register(ResourceLocation modelId, AnchorModelRenderer renderer) {
        RENDERERS.put(modelId, renderer);
    }

    /**
     * Retrieves the registered model renderer associated with the given identifier.
     *
     * @param modelId The unique {@link ResourceLocation} identifier.
     * @return The bound {@link AnchorModelRenderer}, or {@code null} if no renderer matches the identifier.
     */
    public static @Nullable AnchorModelRenderer get(ResourceLocation modelId) {
        return RENDERERS.get(modelId);
    }
}