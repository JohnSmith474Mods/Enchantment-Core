package johnsmith.enchantmentcore.api.config;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

/**
 * Defines a configuration target and an optional fallback.
 * Resolves the active target based on mod load status and handles orphaned configurations.
 *
 * @param target   The primary configuration target.
 * @param fallback The optional fallback configuration target.
 */
public record ConfigReference(ConfigDescription target, Optional<ConfigDescription> fallback) {
    private static final Codec<ConfigReference> TRY_ELSE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigDescription.CODEC.fieldOf("try_config").forGetter(ConfigReference::target),
            ConfigDescription.CODEC.fieldOf("else_config").forGetter(ref -> ref.fallback.orElse(null))
    ).apply(instance, (tryConfig, elseConfig) -> new ConfigReference(tryConfig, Optional.ofNullable(elseConfig))));

    /**
     * Primary codec for serializing and deserializing configuration references.
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
     * Resolves the active configuration description.
     * Evaluates the fallback if the primary target mod is missing. Adopts orphaned configurations if the manager is absent.
     *
     * @return The resolved configuration description.
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