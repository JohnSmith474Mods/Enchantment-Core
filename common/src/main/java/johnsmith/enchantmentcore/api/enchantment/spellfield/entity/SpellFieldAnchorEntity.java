package johnsmith.enchantmentcore.api.enchantment.spellfield.entity;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import johnsmith.enchantmentcore.api.enchantment.spellfield.SpellFieldComponent;
import johnsmith.enchantmentcore.api.entity.SpellFieldAnchor;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Non-physical, invulnerable marker entity that acts as a persistent or delayed world-space origin
 * for a {@link SpellFieldComponent}.
 * <p>
 * Supports configurable activation delays, duty-cycle pulsing ({@code activePhase} and {@code restPhase}),
 * custom tick intervals ({@code tickRate}), scheduled sub-tasks, and client-synchronized visual rendering
 * parameters exposed through {@link SpellFieldAnchor}.
 */
public class SpellFieldAnchorEntity extends Entity implements SpellFieldAnchor {
    /** Synchronized entity data accessor for the billboard sprite texture location string. */
    private static final EntityDataAccessor<String> VISUAL_TEXTURE = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.STRING);

    /** Synchronized entity data accessor for the custom 3D model renderer identifier string. */
    private static final EntityDataAccessor<String> MODEL_ID = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.STRING);

    /** Synchronized entity data accessor for the visual rendering scale factor. */
    private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.FLOAT);

    /** Synchronized entity data accessor for the total number of vertical sprite animation frames. */
    private static final EntityDataAccessor<Integer> VISUAL_FRAMES = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.INT);

    /** Synchronized entity data accessor for the number of ticks per sprite animation frame. */
    private static final EntityDataAccessor<Integer> VISUAL_TICK_RATE = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.INT);

    /** Synchronized entity data accessor for the 24-bit RGB color tint. */
    private static final EntityDataAccessor<Integer> VISUAL_TINT = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.INT);

    /** Total lifespan of the anchor entity in ticks before automatic removal. */
    private int lifeTime = 100;

    /** Interval in ticks between successive evaluations of {@link #spellField} during an active phase. */
    private int tickRate = 1;

    /** Initial delay in ticks after spawning before the first active phase begins. */
    private int activationDelay = 0;

    /** Duration in ticks of each active pulsing window (or {@code <= 0} for continuous activation). */
    private int activePhase = 0;

    /** Duration in ticks of the dormant rest window between active phases. */
    private int restPhase = 0;

    /** The spell field component evaluated by this anchor. */
    private SpellFieldComponent spellField;

    /** The enchantment level passed to the spell field evaluation. */
    private int enchantmentLevel;

    /** Copy of the enchanted item stack that spawned this anchor. */
    private ItemStack itemStack = ItemStack.EMPTY;

    /** UUID of the caster/owner entity, if present. */
    private UUID ownerUUID;

    /** Equipment slot in which the triggering item was held or worn. */
    private EquipmentSlot slot = EquipmentSlot.MAINHAND;

    /** Internal queue of delayed actions scheduled relative to {@link #tickCount}. */
    private final List<ScheduledTask> taskQueue = new ArrayList<>();

    /**
     * Constructs a new {@link SpellFieldAnchorEntity} with physics disabled.
     *
     * @param entityType The entity type definition.
     * @param level      The level in which the entity is instantiated.
     */
    public SpellFieldAnchorEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    /**
     * Configures the operational lifecycle and spell field payload for this anchor entity.
     *
     * @param lifeTime         Total lifetime of the anchor in ticks.
     * @param tickRate         Interval in ticks between spell field applications.
     * @param activationDelay  Initial delay in ticks before the anchor activates.
     * @param activePhase      Duration in ticks of each active phase in the duty cycle.
     * @param restPhase        Duration in ticks of each rest phase in the duty cycle.
     * @param spellField       The {@link SpellFieldComponent} payload to execute.
     * @param enchantmentLevel The enchantment level context.
     * @param itemStack        The source enchanted item stack.
     * @param slot             The equipment slot associated with the item.
     * @param owner            The entity that triggered or owns this spell field anchor, or {@code null}.
     */
    public void setAnchorData(int lifeTime, int tickRate, int activationDelay, int activePhase, int restPhase, SpellFieldComponent spellField, int enchantmentLevel, ItemStack itemStack, EquipmentSlot slot, Entity owner) {
        this.lifeTime = lifeTime;
        this.tickRate = tickRate;
        this.activationDelay = activationDelay;
        this.activePhase = activePhase;
        this.restPhase = restPhase;
        this.spellField = spellField;
        this.enchantmentLevel = enchantmentLevel;
        this.itemStack = itemStack.copy();
        this.slot = slot;
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    /**
     * Applies and synchronizes the provided {@link AnchorVisualConfig} to this entity's tracked data parameters.
     *
     * @param config The visual configuration to apply, or {@code null} for no-op.
     */
    public void setVisualConfig(AnchorVisualConfig config) {
        if (config != null) {
            config.texture().ifPresent(tex -> this.entityData.set(VISUAL_TEXTURE, tex.toString()));
            config.modelId().ifPresent(id -> this.entityData.set(MODEL_ID, id.toString()));
            this.entityData.set(VISUAL_SCALE, config.scale());
            this.entityData.set(VISUAL_FRAMES, Math.max(1, config.frameCount()));
            this.entityData.set(VISUAL_TICK_RATE, Math.max(1, config.ticksPerFrame()));
            this.entityData.set(VISUAL_TINT, config.tint());
        }
    }

    /**
     * Inflates the render culling bounding box by the configured visual scale so large anchor visuals
     * are not prematurely frustum-culled.
     *
     * @return The inflated culling {@link AABB}.
     */
    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel serverlevel, DamageSource source, float amount) {
        return false;
    }

    @Override
    public String getVisualTexture() { return this.entityData.get(VISUAL_TEXTURE); }

    @Override
    public String getModelId() { return this.entityData.get(MODEL_ID); }

    @Override
    public float getVisualScale() { return this.entityData.get(VISUAL_SCALE); }

    @Override
    public int getVisualFrames() { return this.entityData.get(VISUAL_FRAMES); }

    @Override
    public int getVisualTickRate() { return this.entityData.get(VISUAL_TICK_RATE); }

    @Override
    public int getVisualTint() { return this.entityData.get(VISUAL_TINT); }

    /**
     * Schedules a runnable task to be executed on the server thread after the specified number of ticks.
     *
     * @param ticksFromNow Delay in ticks relative to the entity's current {@link #tickCount}.
     * @param action       The runnable action to execute.
     */
    public void schedule(int ticksFromNow, Runnable action) {
        this.taskQueue.add(new ScheduledTask(this.tickCount + ticksFromNow, action));
    }

    /**
     * Advances the anchor's position, executes any due scheduled tasks, evaluates duty-cycle timing,
     * and dispatches the bound {@link SpellFieldComponent} on the server thread.
     */
    @Override
    public void tick() {
        super.tick();

        Vec3 delta = this.getDeltaMovement();
        if (delta.lengthSqr() > 1.0E-6D) {
            this.setPos(this.getX() + delta.x, this.getY() + delta.y, this.getZ() + delta.z);
        }

        if (!this.level().isClientSide()) {
            java.util.Iterator<ScheduledTask> iterator = this.taskQueue.iterator();
            while (iterator.hasNext()) {
                ScheduledTask task = iterator.next();
                if (this.tickCount >= task.executionTick) {
                    task.action.run();
                    iterator.remove();
                }
            }

            if (this.tickCount > this.lifeTime) {
                this.discard();
                return;
            }

            if (this.tickCount <= this.activationDelay) {
                return;
            }

            int activeWindow = this.activePhase <= 0 ? this.lifeTime : this.activePhase;
            int cycleLength = activeWindow + this.restPhase;
            int cycleTime = (this.tickCount - this.activationDelay - 1) % cycleLength;

            if (cycleTime < activeWindow) {
                if (cycleTime % this.tickRate == 0 && this.spellField != null) {
                    Entity owner = null;
                    if (this.ownerUUID != null) {
                        owner = this.level().getEntity(this.ownerUUID);
                    }

                    LivingEntity livingOwner = owner instanceof LivingEntity ? (LivingEntity) owner : null;
                    EnchantedItemInUse context = new EnchantedItemInUse(this.itemStack, this.slot, livingOwner);

                    this.spellField.apply((ServerLevel) this.level(), this.enchantmentLevel, context, this, this.position());
                }
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VISUAL_TEXTURE, "");
        builder.define(MODEL_ID, "");
        builder.define(VISUAL_SCALE, 1.0F);
        builder.define(VISUAL_FRAMES, 1);
        builder.define(VISUAL_TICK_RATE, 1);
        builder.define(VISUAL_TINT, 0xFFFFFF);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.lifeTime = input.getIntOr("LifeTime", 100);
        this.tickCount = input.getIntOr("Age", 0);
        this.tickRate = Math.max(1, input.getIntOr("TickRate", 1));
        this.activationDelay = input.getIntOr("ActivationDelay", 0);
        this.activePhase = input.getIntOr("ActivePhase", 0);
        this.restPhase = input.getIntOr("RestPhase", 0);
        this.enchantmentLevel = input.getIntOr("EnchantmentLevel", 0);

        input.read("VisualConfig", AnchorVisualConfig.CODEC).ifPresent(this::setVisualConfig);
        input.read("OwnerUUID", UUIDUtil.CODEC).ifPresent(uuid -> this.ownerUUID = uuid);
        input.read("Item", ItemStack.OPTIONAL_CODEC).ifPresent(item -> this.itemStack = item);
        input.read("Slot", Codec.STRING).ifPresent(slotName -> this.slot = EquipmentSlot.byName(slotName));
        input.read("SpellField", SpellFieldComponent.CODEC.codec()).ifPresent(field -> this.spellField = field);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("LifeTime", this.lifeTime);
        output.putInt("Age", this.tickCount);
        output.putInt("TickRate", this.tickRate);
        output.putInt("ActivationDelay", this.activationDelay);
        output.putInt("ActivePhase", this.activePhase);
        output.putInt("RestPhase", this.restPhase);
        output.putInt("EnchantmentLevel", this.enchantmentLevel);

        String textureStr = this.entityData.get(VISUAL_TEXTURE);
        String modelIdStr = this.entityData.get(MODEL_ID);
        AnchorVisualConfig config = new AnchorVisualConfig(
                textureStr.isEmpty() ? Optional.empty() : Optional.of(Identifier.parse(textureStr)),
                modelIdStr.isEmpty() ? Optional.empty() : Optional.of(Identifier.parse(modelIdStr)),
                this.entityData.get(VISUAL_SCALE),
                this.entityData.get(VISUAL_FRAMES),
                this.entityData.get(VISUAL_TICK_RATE),
                this.entityData.get(VISUAL_TINT)
        );

        output.store("VisualConfig", AnchorVisualConfig.CODEC, config);

        if (this.ownerUUID != null) {
            output.store("OwnerUUID", UUIDUtil.CODEC, this.ownerUUID);
        }

        if (!this.itemStack.isEmpty()) {
            output.store("Item", ItemStack.OPTIONAL_CODEC, this.itemStack);
        }

        output.putString("Slot", this.slot.getName());

        if (this.spellField != null) {
            output.store("SpellField", SpellFieldComponent.CODEC.codec(), this.spellField);
        }
    }

    /**
     * Internal record pairing a target entity tick timestamp with a deferred action.
     *
     * @param executionTick The entity {@link #tickCount} at which the action should run.
     * @param action        The callback to execute.
     */
    private record ScheduledTask(int executionTick, Runnable action) {}
}