package johnsmith.enchantmentcore.api.entity;

/**
 * Contract defining the structural rendering properties of a generated spell field anchor entity.
 */
public interface SpellFieldAnchor {
    /**
     * Retrieves the unique resource location string for the entity's base visual texture.
     *
     * @return The resource location string.
     */
    String getVisualTexture();

    /**
     * Retrieves the unique resource location string for the entity's custom model definition.
     *
     * @return The resource location string, or null if no model is defined.
     */
    String getModelId();

    /**
     * Retrieves the physical scaling scalar applied during the rendering pipeline.
     *
     * @return The visual scale multiplier.
     */
    float getVisualScale();

    /**
     * Retrieves the total frame count defined for the visual texture animation logic.
     *
     * @return The total number of animation frames.
     */
    int getVisualFrames();

    /**
     * Retrieves the duration in ticks applied to each animation frame.
     *
     * @return The number of ticks per frame.
     */
    int getVisualTickRate();

    /**
     * Retrieves the RGB color tint applied across the visual rendering pass.
     *
     * @return The RGB tint color integer.
     */
    int getVisualTint();
}