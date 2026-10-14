package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the evaluated spatial geometry and topological rules for a spell field volume
 * expressed in absolute world coordinates.
 *
 * @param bounds       The absolute world-space {@link AABB} enclosing the volume.
 * @param topology     The mathematical {@link Topology} governing internal field strength distribution, or {@code null} if uniform.
 * @param volumeCenter The absolute world-space epicenter of the volume evaluation logic.
 */
public record GlobalVolume(AABB bounds, @Nullable Topology topology, Vec3 volumeCenter) {}