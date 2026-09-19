package johnsmith.enchantmentcore.enchantment.spellfield.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import johnsmith.enchantmentcore.api.entity.SpellFieldAnchor;
import johnsmith.enchantmentcore.enchantment.spellfield.SpellFieldComponent;

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

public class SpellFieldAnchorEntity extends Entity implements SpellFieldAnchor {
    private static final EntityDataAccessor<String> VISUAL_TEXTURE = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MODEL_ID = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> VISUAL_FRAMES = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VISUAL_TICK_RATE = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VISUAL_TINT = SynchedEntityData.defineId(SpellFieldAnchorEntity.class, EntityDataSerializers.INT);

    private int lifeTime = 100;
    private int tickRate = 1;
    private int activationDelay = 0;
    private int activePhase = 0;
    private int restPhase = 0;
    private SpellFieldComponent spellField;
    private int enchantmentLevel;
    private ItemStack itemStack = ItemStack.EMPTY;
    private UUID ownerUUID;
    private EquipmentSlot slot = EquipmentSlot.MAINHAND;

    private final List<ScheduledTask> taskQueue = new ArrayList<>();

    public SpellFieldAnchorEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

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

    public void schedule(int ticksFromNow, Runnable action) {
        this.taskQueue.add(new ScheduledTask(this.tickCount + ticksFromNow, action));
    }

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
        this.lifeTime = tag.getInt("LifeTime");
        this.tickCount = tag.getInt("Age");
        this.tickRate = Math.max(1, tag.getInt("TickRate"));
        this.activationDelay = tag.getInt("ActivationDelay");
        this.activePhase = tag.getInt("ActivePhase");
        this.restPhase = tag.getInt("RestPhase");
        this.enchantmentLevel = tag.getInt("EnchantmentLevel");

        if (tag.contains("VisualConfig")) {
            AnchorVisualConfig.CODEC.parse(NbtOps.INSTANCE, tag.get("VisualConfig"))
                    .result()
                    .ifPresent(this::setVisualConfig);
        }

        if (tag.contains("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("Item")) {
            this.itemStack = ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Item"));
        }
        if (tag.contains("Slot")) {
            this.slot = EquipmentSlot.byName(tag.getString("Slot"));
        }

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
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        if (!this.itemStack.isEmpty()) {
            tag.put("Item", this.itemStack.saveOptional(this.registryAccess()));
        }
        tag.putString("Slot", this.slot.getName());

        if (this.spellField != null) {
            SpellFieldComponent.CODEC.codec().encodeStart(NbtOps.INSTANCE, this.spellField)
                    .result()
                    .ifPresent(fieldNbt -> tag.put("SpellField", fieldNbt));
        }
    }

    private record ScheduledTask(int executionTick, Runnable action) {}
}