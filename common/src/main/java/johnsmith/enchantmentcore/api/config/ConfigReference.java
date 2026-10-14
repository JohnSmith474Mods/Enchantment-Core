package johnsmith.enchantmentcore.api.config;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

/**
 * Represents a reference to a dynamic configuration property, supporting both direct targeting and
 * conditional mod-dependent fallback resolution ({@code "try_config"} / {@code "else_config"}).
 * <p>
 * During resolution, if a {@code fallback} is defined and the primary {@code target} mod is not loaded
 * according to {@link ModLoadEvaluator}, the reference transparently falls back to the secondary target.
 * If the resulting target's {@link ConfigManager} is absent, the reference is routed through
 * {@link OrphanHandler} for adoption.
 *
 * @param target   The primary {@link ConfigDescription} target to evaluate first.
 * @param fallback An optional secondary {@link ConfigDescription} used when the primary target's mod is not loaded.
 */
public record ConfigReference(ConfigDescription target, Optional<ConfigDescription> fallback) {
    /**
     * Internal codec for parsing conditional {@code "try_config"} and {@code "else_config"} JSON objects.
     */
    private static final Codec<ConfigReference> TRY_ELSE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigDescription.CODEC.fieldOf("try_config").forGetter(ConfigReference::target),
            ConfigDescription.CODEC.fieldOf("else_config").forGetter(ref -> ref.fallback.orElse(null))
    ).apply(instance, (tryConfig, elseConfig) -> new ConfigReference(tryConfig, Optional.ofNullable(elseConfig))));

    /**
     * Primary codec capable of serializing and deserializing either a direct {@link ConfigDescription}
     * object or a conditional {@code "try_config"} / {@code "else_config"} structure.
     */
    public static final Codec<ConfigReference> CODEC = Codec.either(ConfigDescription.CODEC, TRY_ELSE_CODEC)
            .xmap(
                    either -> either.map(
                            single -> new ConfigReference(single, Optional.empty()),
                            tryElse -> tryElse
                    ),
                    ref -> ref.fallback.isPresent() ? Either.right(ref) : Either.left(ref.target)
            );

    /**
     * Resolves the active {@link ConfigDescription} for the current runtime environment.
     * <p>
     * Evaluates {@code fallback} if present and the primary target's mod is not loaded. If no
     * {@link ConfigManager} is registered for the active target's namespace, the description is passed
     * to {@link OrphanHandler#adopt(ConfigDescription)}.
     *
     * @return The resolved, valid {@link ConfigDescription}.
     */
    public ConfigDescription resolve() {
        ConfigDescription activeTarget = this.target;

        if (this.fallback.isPresent() && !ModLoadEvaluator.Provider.get().isLoaded(this.target.modId())) {
            activeTarget = this.fallback.get();
        }

        ConfigManager manager = ConfigRegistry.getManager(activeTarget.modId());
        if (manager == null) {
            return OrphanHandler.Provider.get().adopt(activeTarget);
        }

        return activeTarget;
    }
}