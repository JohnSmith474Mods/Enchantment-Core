package johnsmith.enchantmentcore.mixin.world.level;

import java.util.Optional;

import johnsmith.enchantmentcore.config.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the ExplosionDamageCalculator to modify environmental resistance logic.
 * Eliminates blast suffocation when an explosion is triggered perfectly inside a solid block.
 */
@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {

    /**
     * Intercepts the block explosion resistance calculation.
     * Forces the resistance of the exact epicenter block to zero if it is not waterlogged.
     * This guarantees spell fields triggering on block impact correctly destroy the impacted block.
     *
     * @param explosion The active explosion instance.
     * @param level     The block getter environment.
     * @param pos       The block coordinate being evaluated.
     * @param state     The block state being evaluated.
     * @param fluid     The fluid state at the coordinate.
     * @param cir       The callback information storing the calculated resistance.
     */
    @Inject(method = "getBlockExplosionResistance", at = @At("RETURN"), cancellable = true)
    private void nullifyEpicenterResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
        // Halt execution if the global configuration is disabled or the block contains fluid.
        if (!Config.ENHANCE_BLOCK_EXPLOSIONS.get() || !fluid.isEmpty()) return;

        Optional<Float> resistance = cir.getReturnValue();

        // Target blocks with standard breakable resistance limits (excludes Bedrock/End Portal Frames).
        if (resistance.isPresent() && resistance.get() < 1200.0F) {
            BlockPos epicenter = BlockPos.containing(explosion.center());

            // Nullify resistance strictly for the origin block.
            if (pos.equals(epicenter)) {
                cir.setReturnValue(Optional.of(0.0F));
            }
        }
    }
}