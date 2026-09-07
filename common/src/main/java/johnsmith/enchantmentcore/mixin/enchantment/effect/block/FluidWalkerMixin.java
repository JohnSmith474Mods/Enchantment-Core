package johnsmith.enchantmentcore.mixin.enchantment.effect.block;

import java.util.List;

import johnsmith.enchantmentcore.api.enchantment.effect.FluidWalkerDefinition;
import johnsmith.enchantmentcore.api.entity.accessor.LivingEntityStateAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting LiquidBlock to dynamically alter fluid collision shapes.
 * Allows entities with active fluid walking effects to treat specific fluids as solid surfaces
 * without modifying the actual block state in the world.
 */
@Mixin(LiquidBlock.class)
public abstract class FluidWalkerMixin {

    /**
     * Intercepts the retrieval of the block's collision shape.
     * Evaluates if the colliding entity has a valid fluid walker effect and dynamically
     * returns a solid voxel shape matching the fluid's surface height.
     *
     * @param state   The block state of the fluid.
     * @param level   The block getter environment.
     * @param pos     The coordinate of the fluid block.
     * @param context The collision context containing the intersecting entity.
     * @param cir     The callback information used to override the returned shape.
     */
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void enchantment_core$solidifyFluid(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity livingEntity) {

            // Retrieve all fluid walker definitions actively applied to the entity.
            List<FluidWalkerDefinition> effects = ((LivingEntityStateAccessor) livingEntity).enchantment_core$getActiveFluidWalkers();
            if (effects.isEmpty()) return;

            FluidState fluidState = state.getFluidState();
            if (fluidState.isEmpty()) return;

            // Verify if the current fluid matches any of the entity's allowed walker definitions.
            boolean fluidMatched = false;
            for (FluidWalkerDefinition effect : effects) {
                if (fluidState.is(effect.allowedFluids())) {
                    fluidMatched = true;
                    break;
                }
            }

            if (!fluidMatched) return;

            // Determine the visual surface height of the fluid to align the collision box accurately.
            double height = fluidState.getHeight(level, pos);
            if (height <= 0.0) return;

            // Add a marginal buffer (0.015D) to prevent the entity from repeatedly clipping
            // slightly into the fluid and losing the "onGround" state.
            double shapeMaxY = height + 0.015D;
            VoxelShape fluidShape = Shapes.create(0.0D, 0.0D, 0.0D, 1.0D, shapeMaxY, 1.0D);

            // Check if the entity is already standing strictly on top of the calculated shape.
            boolean isAbove = context.isAbove(fluidShape, pos, true);

            if (!isAbove) {
                // Determine if the entity is currently submerged in fluid.
                // Submerged entities cannot walk on the surface until they breach it.
                boolean isSubmerged = false;
                BlockPos entityPos = livingEntity.blockPosition();
                FluidState entityFluid = livingEntity.level().getFluidState(entityPos);

                if (!entityFluid.isEmpty()) {
                    double entityFluidTop = entityPos.getY() + entityFluid.getHeight(livingEntity.level(), entityPos);
                    // Evaluate if the player's base is below the fluid surface (with a small floating buffer).
                    if (livingEntity.getY() < entityFluidTop - 0.02D) {
                        isSubmerged = true;
                    }
                }

                // Only evaluate step-height solidification if the player is NOT submerged.
                // This allows players to step up onto the fluid surface from an adjacent solid block.
                if (!isSubmerged) {
                    double entityY = livingEntity.getY();
                    double blockY = pos.getY();
                    double surfaceY = blockY + shapeMaxY;

                    // Verify if the top of the fluid is within the entity's maximum step height.
                    if (entityY + livingEntity.maxUpStep() >= surfaceY && entityY >= blockY - 0.5) {
                        // Ensure the entity is not already clipping horizontally into the fluid block.
                        boolean isInside = livingEntity.getBoundingBox().deflate(0.05, 0.0, 0.05).intersects(
                                pos.getX(), pos.getY(), pos.getZ(),
                                pos.getX() + 1.0D, surfaceY, pos.getZ() + 1.0D
                        );

                        if (!isInside) {
                            isAbove = true;
                        }
                    }
                }
            }

            // Fallback escape clauses: The entity must be positioned above the fluid and
            // not attempting to intentionally sink (crouching) or swim.
            if (!isAbove) return;
            if (livingEntity.isCrouching() || livingEntity.isSwimming()) return;

            // Override the default empty/liquid collision shape with the generated solid surface.
            cir.setReturnValue(fluidShape);
        }
    }
}