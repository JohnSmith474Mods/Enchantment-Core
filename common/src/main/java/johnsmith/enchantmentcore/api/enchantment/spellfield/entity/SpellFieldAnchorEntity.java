package johnsmith.enchantmentcore.api.enchantment.spellfield.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import johnsmith.enchantmentcore.api.enchantment.spellfield.SpellFieldComponent;
import johnsmith.enchantmentcore.api.entity.SpellFieldAnchor;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
                        owner = ((ServerLevel) this.level()).getEntity(this.ownerUUID);
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTime = tag.getInt("LifeTime").orElse(100);
        this.tickCount = tag.getInt("Age").orElse(0);
        this.tickRate = Math.max(1, tag.getInt("TickRate").orElse(1));
        this.activationDelay = tag.getInt("ActivationDelay").orElse(0);
        this.activePhase = tag.getInt("ActivePhase").orElse(0);
        this.restPhase = tag.getInt("RestPhase").orElse(0);
        this.enchantmentLevel = tag.getInt("EnchantmentLevel").orElse(0);

        if (tag.contains("VisualConfig")) {
            AnchorVisualConfig.CODEC.parse(NbtOps.INSTANCE, tag.get("VisualConfig"))
                    .result()
                    .ifPresent(this::setVisualConfig);
        }

        if (tag.contains("OwnerUUID")) {
            UUIDUtil.CODEC.parse(NbtOps.INSTANCE, tag.get("OwnerUUID"))
                    .result()
                    .ifPresent(uuid -> this.ownerUUID = uuid);
        }

        if (tag.contains("Item")) {
            ItemStack.OPTIONAL_CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("Item"))
                    .result()
                    .ifPresent(item -> this.itemStack = item);
        }

        tag.getString("Slot").ifPresent(slotName -> this.slot = EquipmentSlot.byName(slotName));

        if (tag.contains("SpellField")) {
            SpellFieldComponent.CODEC.codec().parse(NbtOps.INSTANCE, tag.get("SpellField"))
                    .result()
                    .ifPresent(field -> this.spellField = field);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTime", this.lifeTime);
        tag.putInt("Age", this.tickCount);
        tag.putInt("TickRate", this.tickRate);
        tag.putInt("ActivationDelay", this.activationDelay);
        tag.putInt("ActivePhase", this.activePhase);
        tag.putInt("RestPhase", this.restPhase);
        tag.putInt("EnchantmentLevel", this.enchantmentLevel);

        String textureStr = this.entityData.get(VISUAL_TEXTURE);
        String modelIdStr = this.entityData.get(MODEL_ID);

        AnchorVisualConfig config = new AnchorVisualConfig(
                textureStr.isEmpty() ? Optional.empty() : Optional.of(ResourceLocation.parse(textureStr)),
                modelIdStr.isEmpty() ? Optional.empty() : Optional.of(ResourceLocation.parse(modelIdStr)),
                this.entityData.get(VISUAL_SCALE),
                this.entityData.get(VISUAL_FRAMES),
                this.entityData.get(VISUAL_TICK_RATE),
                this.entityData.get(VISUAL_TINT)
        );

        AnchorVisualConfig.CODEC.encodeStart(NbtOps.INSTANCE, config)
                .result()
                .ifPresent(visualNbt -> tag.put("VisualConfig", visualNbt));

        if (this.ownerUUID != null) {
            UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, this.ownerUUID)
                    .result()
                    .ifPresent(uuidNbt -> tag.put("OwnerUUID", uuidNbt));
        }

        if (!this.itemStack.isEmpty()) {
            ItemStack.OPTIONAL_CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.itemStack)
                    .result()
                    .ifPresent(itemNbt -> tag.put("Item", itemNbt));
        }

        tag.putString("Slot", this.slot.getName());

        if (this.spellField != null) {
            SpellFieldComponent.CODEC.codec().encodeStart(NbtOps.INSTANCE, this.spellField)
                    .result()
                    .ifPresent(fieldNbt -> tag.put("SpellField", fieldNbt));
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