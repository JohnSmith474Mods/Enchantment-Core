package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the evaluated spatial geometry and topological rules for a spell field volume
 * expressed relative to an anchor entity's local origin for frame-interpolated client debug rendering.
 *
 * @param bounds       The relative {@link AABB} enclosing the volume offset from the anchor entity's position.
 * @param topology     The mathematical {@link Topology} governing internal field strength distribution, or {@code null} if uniform.
 * @param volumeCenter The relative epicenter vector offset from the anchor entity's position.
 */
public record LocalVolume(AABB bounds, @Nullable Topology topology, Vec3 volumeCenter) {}