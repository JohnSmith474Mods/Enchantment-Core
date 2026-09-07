package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Encapsulates the evaluated spatial geometry and topological rules for a spell field volume in absolute world coordinates.
 *
 * @param bounds       The absolute AABB enclosing the volume in world space.
 * @param topology     The mathematical rule set governing internal field strength distribution.
 * @param volumeCenter The absolute epicenter of the volume evaluation logic.
 */
public record GlobalVolume(AABB bounds, Topology topology, Vec3 volumeCenter) {}