package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Encapsulates the evaluated spatial geometry and topological rules for a spell field volume relative to an entity's position.
 *
 * @param bounds       The relative AABB enclosing the volume offset from the anchor position.
 * @param topology     The mathematical rule set governing internal field strength distribution.
 * @param volumeCenter The relative epicenter offset from the anchor position.
 */
public record LocalVolume(AABB bounds, Topology topology, Vec3 volumeCenter) {}