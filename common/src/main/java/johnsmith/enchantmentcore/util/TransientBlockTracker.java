package johnsmith.enchantmentcore.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import johnsmith.enchantmentcore.Constants;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the temporary modification, decay animation, and restoration of block states within a {@link ServerLevel}.
 * <p>
 * To survive chunk unloads and reloads without losing restoration state or desynchronizing from frozen
 * {@code SpellFieldAnchorEntity} instances in unloaded chunks, tracked blocks are partitioned into
 * chunk-keyed buckets ({@link ChunkPos#asLong(BlockPos)}) and evaluated using relative tick counters
 * ({@code remainingStableTicks} and {@code remainingDecayTicks}).
 * <p>
 * Progression and block restoration are strictly gated behind {@link ServerLevel#shouldTickBlocksAt(long)},
 * ensuring that blocks in unloaded or border chunks pause their timers until the chunk re-enters an
 * active block-ticking state.
 */
public class TransientBlockTracker extends SavedData {

    /**
     * The persistent storage identifier used to save and load this tracker in the level's {@code data} directory.
     */
    private static final String DATA_NAME = "enchantment_core_transient_blocks";

    /**
     * Codec responsible for serializing and deserializing all chunk-partitioned transient block records.
     */
    public static final Codec<TransientBlockTracker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TransientBlockRecord.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(tracker -> {
                List<TransientBlockRecord> list = new ArrayList<>();
                for (Map<BlockPos, TransientBlock> bucket : tracker.chunkBuckets.values()) {
                    for (Map.Entry<BlockPos, TransientBlock> entry : bucket.entrySet()) {
                        TransientBlock block = entry.getValue();
                        list.add(new TransientBlockRecord(
                                entry.getKey(),
                                block.originalState,
                                Optional.ofNullable(block.blockEntityData),
                                block.remainingStableTicks,
                                block.remainingDecayTicks,
                                block.totalDecayTicks
                        ));
                    }
                }
                return list;
            })
    ).apply(instance, list -> {
        TransientBlockTracker tracker = new TransientBlockTracker();
        for (TransientBlockRecord record : list) {
            BlockPos pos = record.pos().immutable();
            long chunkKey = ChunkPos.asLong(pos);
            tracker.chunkBuckets
                    .computeIfAbsent(chunkKey, k -> new HashMap<>())
                    .put(pos, new TransientBlock(
                            record.state(),
                            record.blockEntity().orElse(null),
                            record.remainingStableTicks(),
                            record.remainingDecayTicks(),
                            record.totalDecayTicks()
                    ));
        }
        return tracker;
    }));

    /**
     * Persistent data type definition registered with the level's data storage.
     */
    public static final SavedDataType<TransientBlockTracker> TYPE = new SavedDataType<>(
            DATA_NAME,
            TransientBlockTracker::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    /**
     * Chunk-partitioned storage mapping packed chunk coordinates ({@link ChunkPos#asLong(BlockPos)})
     * to a map of individual block positions and their corresponding {@link TransientBlock} state records.
     */
    private final Long2ObjectMap<Map<BlockPos, TransientBlock>> chunkBuckets = new Long2ObjectOpenHashMap<>();

    /**
     * Retrieves the active {@link TransientBlockTracker} instance for the specified server level,
     * instantiating and registering a new tracker if one does not already exist.
     *
     * @param level The target server level.
     * @return The bound {@link TransientBlockTracker} instance.
     */
    public static TransientBlockTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Checks whether the specified block coordinate is currently undergoing a temporary modification
     * managed by this tracker.
     *
     * @param pos The block coordinate to query.
     * @return {@code true} if the position is actively tracked and awaiting restoration, {@code false} otherwise.
     */
    public boolean isTracked(BlockPos pos) {
        Map<BlockPos, TransientBlock> bucket = this.chunkBuckets.get(ChunkPos.asLong(pos));
        return bucket != null && bucket.containsKey(pos);
    }

    /**
     * Retrieves the original pre-modification {@link BlockState} recorded for the specified coordinate.
     * <p>
     * Used by temporary block effects during repeated spell field ticks or post-chunk-reload ticks to prevent
     * overwriting the true original block state with the temporary block's own state.
     *
     * @param pos The block coordinate to query.
     * @return The original {@link BlockState} scheduled for restoration, or {@code null} if the position is not tracked.
     */
    public @Nullable BlockState getOriginalState(BlockPos pos) {
        Map<BlockPos, TransientBlock> bucket = this.chunkBuckets.get(ChunkPos.asLong(pos));
        if (bucket == null) {
            return null;
        }
        TransientBlock block = bucket.get(pos);
        return block != null ? block.originalState : null;
    }

    /**
     * Registers a block position for temporary modification using absolute game-time timestamps.
     * <p>
     * Converts the provided timestamps into relative tick durations and delegates to
     * {@link #addBlock(ServerLevel, BlockPos, BlockState, CompoundTag, int, int)}.
     *
     * @param level           The server level containing the block.
     * @param pos             The coordinate position of the modified block.
     * @param originalState   The original block state to restore upon expiration.
     * @param blockEntityData The serialized NBT metadata of the original block entity, or {@code null} if none existed.
     * @param decayStartTick  The absolute game time tick when the block-breaking decay animation should begin.
     * @param expiryTick      The absolute game time tick when the original block state should be restored.
     */
    public void addBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState originalState,
            @Nullable CompoundTag blockEntityData,
            long decayStartTick,
            long expiryTick
    ) {
        long currentTick = level.getGameTime();
        int stableTicks = (int) Math.max(0L, decayStartTick - currentTick);
        int decayTicks = (int) Math.max(0L, expiryTick - Math.max(currentTick, decayStartTick));
        this.addBlock(level, pos, originalState, blockEntityData, stableTicks, decayTicks);
    }

    /**
     * Registers a block position for temporary modification using relative tick durations.
     * <p>
     * If the position is already tracked, its original {@link BlockState} and {@link BlockEntity} NBT are
     * preserved while its remaining lifetime and decay timers are refreshed to the maximum of the existing
     * and incoming durations.
     *
     * @param level           The server level containing the block.
     * @param pos             The coordinate position of the modified block.
     * @param originalState   The original block state to restore upon expiration.
     * @param blockEntityData The serialized NBT metadata of the original block entity, or {@code null} if none existed.
     * @param stableTicks     The number of active chunk ticks before decay animation begins.
     * @param decayTicks      The number of active chunk ticks over which the decay animation plays before restoration.
     */
    public void addBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState originalState,
            @Nullable CompoundTag blockEntityData,
            int stableTicks,
            int decayTicks
    ) {
        long chunkKey = ChunkPos.asLong(pos);
        Map<BlockPos, TransientBlock> bucket = this.chunkBuckets.computeIfAbsent(chunkKey, k -> new HashMap<>());
        BlockPos immutablePos = pos.immutable();

        TransientBlock existing = bucket.get(immutablePos);
        if (existing != null) {
            existing.remainingStableTicks = Math.max(existing.remainingStableTicks, Math.max(0, stableTicks));
            existing.remainingDecayTicks = Math.max(existing.remainingDecayTicks, Math.max(0, decayTicks));
            existing.totalDecayTicks = Math.max(1, existing.remainingDecayTicks);

            // Reset the active block-breaking overlay if the block was already visibly decaying.
            if (existing.lastProgress >= 0) {
                level.destroyBlockProgress(getBreakerId(immutablePos), immutablePos, -1);
                existing.lastProgress = -1;
            }
        } else {
            bucket.put(immutablePos, new TransientBlock(
                    originalState,
                    blockEntityData,
                    Math.max(0, stableTicks),
                    Math.max(0, decayTicks),
                    Math.max(1, decayTicks)
            ));
        }
        this.setDirty();
    }

    /**
     * Executes the per-tick evaluation loop for all tracked transient blocks in the given level.
     * <p>
     * Chunks that are currently unloaded or outside the active block-ticking simulation distance
     * ({@link ServerLevel#shouldTickBlocksAt(long)} returns {@code false}) are skipped without
     * decrementing their timers or discarding their restoration entries.
     *
     * @param level The executing server level.
     */
    public void tick(ServerLevel level) {
        if (this.chunkBuckets.isEmpty()) {
            return;
        }

        boolean changed = false;
        var chunkIterator = this.chunkBuckets.long2ObjectEntrySet().iterator();

        while (chunkIterator.hasNext()) {
            Long2ObjectMap.Entry<Map<BlockPos, TransientBlock>> chunkEntry = chunkIterator.next();
            long chunkKey = chunkEntry.getLongKey();

            // Do not advance timers or attempt world writes if the chunk is unloaded or non-ticking.
            if (!level.shouldTickBlocksAt(chunkKey)) {
                continue;
            }

            Map<BlockPos, TransientBlock> bucket = chunkEntry.getValue();
            Iterator<Map.Entry<BlockPos, TransientBlock>> blockIterator = bucket.entrySet().iterator();

            while (blockIterator.hasNext()) {
                Map.Entry<BlockPos, TransientBlock> entry = blockIterator.next();
                BlockPos pos = entry.getKey();
                TransientBlock block = entry.getValue();

                if (block.remainingStableTicks > 0) {
                    block.remainingStableTicks--;
                    changed = true;
                    continue;
                }

                if (block.remainingDecayTicks > 0) {
                    block.remainingDecayTicks--;
                    changed = true;

                    int elapsedDecay = block.totalDecayTicks - block.remainingDecayTicks;
                    int progress = (elapsedDecay * 10) / block.totalDecayTicks;

                    if (progress != block.lastProgress && progress >= 0 && progress < 10) {
                        level.destroyBlockProgress(getBreakerId(pos), pos, progress);
                        block.lastProgress = progress;
                    }
                }

                if (block.remainingStableTicks <= 0 && block.remainingDecayTicks <= 0) {
                    this.restoreBlock(level, pos, block);
                    blockIterator.remove();
                    changed = true;
                }
            }

            if (bucket.isEmpty()) {
                chunkIterator.remove();
                changed = true;
            }
        }

        if (changed) {
            this.setDirty();
        }
    }

    /**
     * Restores a single expired transient block and its associated {@link BlockEntity} metadata in the world,
     * clearing any active block-breaking crack overlay.
     *
     * @param level The active server level.
     * @param pos   The target block coordinate.
     * @param block The transient block record containing the original state and NBT payload.
     */
    private void restoreBlock(ServerLevel level, BlockPos pos, TransientBlock block) {
        if (block.lastProgress >= 0) {
            level.destroyBlockProgress(getBreakerId(pos), pos, -1);
        }

        level.setBlock(pos, block.originalState, Block.UPDATE_ALL);

        if (block.blockEntityData != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(be.problemPath(), Constants.LOG)) {
                    be.loadWithComponents(TagValueInput.create(reporter, level.registryAccess(), block.blockEntityData));
                }
                be.setChanged();
            } else {
                BlockEntity restoredBe = BlockEntity.loadStatic(pos, block.originalState, block.blockEntityData, level.registryAccess());
                if (restoredBe != null) {
                    level.setBlockEntity(restoredBe);
                    restoredBe.setChanged();
                }
            }
        }
    }

    /**
     * Computes a deterministic synthetic entity breaker ID for a given {@link BlockPos} to avoid
     * colliding with real entity IDs when broadcasting block destruction progress packets.
     *
     * @param pos The target block position.
     * @return A hashed integer identifier unique to the block position.
     */
    private static int getBreakerId(BlockPos pos) {
        return Long.hashCode(pos.asLong()) ^ 0x5F3759DF;
    }

    /**
     * Internal data structure holding the original state and relative countdown timers for a single modified block.
     */
    private static class TransientBlock {
        /** The original block state to restore when both countdown timers reach zero. */
        final BlockState originalState;
        /** Optional NBT data of the original block entity prior to replacement. */
        final @Nullable CompoundTag blockEntityData;
        /** Ticks remaining in the stable phase before crack animations begin. */
        int remainingStableTicks;
        /** Ticks remaining in the decay phase before block restoration occurs. */
        int remainingDecayTicks;
        /** The total duration of the decay phase used to normalize crack stage progress (0-9). */
        int totalDecayTicks;
        /** The last broadcasted destruction progress stage, or -1 if no crack overlay is active. */
        int lastProgress = -1;

        /**
         * Constructs a new {@link TransientBlock} record.
         *
         * @param originalState        The block state to restore upon expiration.
         * @param blockEntityData      The serialized block entity NBT, or {@code null}.
         * @param remainingStableTicks The initial stable phase tick count.
         * @param remainingDecayTicks  The initial decay phase tick count.
         * @param totalDecayTicks      The total decay phase duration for crack interpolation.
         */
        TransientBlock(
                BlockState originalState,
                @Nullable CompoundTag blockEntityData,
                int remainingStableTicks,
                int remainingDecayTicks,
                int totalDecayTicks
        ) {
            this.originalState = originalState;
            this.blockEntityData = blockEntityData;
            this.remainingStableTicks = remainingStableTicks;
            this.remainingDecayTicks = remainingDecayTicks;
            this.totalDecayTicks = Math.max(1, totalDecayTicks);
        }
    }

    /**
     * DTO mapping used for serializing and deserializing tracked block data to disk.
     * Supports both the v1.1.0 relative-tick schema and fallback migration from legacy v1.0.0 absolute-tick saves.
     */
    private record TransientBlockRecord(
            BlockPos pos,
            BlockState state,
            Optional<CompoundTag> blockEntity,
            int remainingStableTicks,
            int remainingDecayTicks,
            int totalDecayTicks
    ) {
        public static final Codec<TransientBlockRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(r -> r.pos().getX()),
                Codec.INT.fieldOf("y").forGetter(r -> r.pos().getY()),
                Codec.INT.fieldOf("z").forGetter(r -> r.pos().getZ()),
                BlockState.CODEC.optionalFieldOf("state", Blocks.AIR.defaultBlockState()).forGetter(TransientBlockRecord::state),
                CompoundTag.CODEC.optionalFieldOf("block_entity").forGetter(TransientBlockRecord::blockEntity),
                Codec.INT.optionalFieldOf("remaining_stable", -1).forGetter(TransientBlockRecord::remainingStableTicks),
                Codec.INT.optionalFieldOf("remaining_decay", -1).forGetter(TransientBlockRecord::remainingDecayTicks),
                Codec.INT.optionalFieldOf("total_decay", -1).forGetter(TransientBlockRecord::totalDecayTicks),
                Codec.LONG.optionalFieldOf("decay_start", -1L).forGetter(r -> -1L),
                Codec.LONG.optionalFieldOf("start", -1L).forGetter(r -> -1L),
                Codec.LONG.optionalFieldOf("expiry", -1L).forGetter(r -> -1L)
        ).apply(instance, (x, y, z, state, be, remainingStable, remainingDecay, totalDecay, decayStart, start, expiry) -> {
            int resolvedStable;
            int resolvedDecay;
            int resolvedTotalDecay;

            if (remainingStable >= 0 && remainingDecay >= 0) {
                resolvedStable = remainingStable;
                resolvedDecay = remainingDecay;
                resolvedTotalDecay = Math.max(1, totalDecay);
            } else {
                long actualDecayStart = decayStart != -1L ? decayStart : (start != -1L ? start : 0L);
                resolvedStable = 20;
                resolvedDecay = (int) Math.max(1L, expiry - actualDecayStart);
                resolvedTotalDecay = resolvedDecay;
            }

            return new TransientBlockRecord(
                    new BlockPos(x, y, z),
                    state,
                    be,
                    resolvedStable,
                    resolvedDecay,
                    resolvedTotalDecay
            );
        }));
    }
}