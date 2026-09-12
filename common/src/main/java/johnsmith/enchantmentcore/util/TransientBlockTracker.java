package johnsmith.enchantmentcore.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Manages the temporary modification and restoration of block states within a server level.
 * Stores original block states and block entity data. Restores data automatically when the defined
 * expiration tick occurs. Utilizes a time-bucketed schedule map to optimize expiration queries.
 */
public class TransientBlockTracker extends SavedData {
    private static final String DATA_NAME = "enchantment_core_transient_blocks";

    public static final Codec<TransientBlockTracker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TransientBlockRecord.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(tracker -> {
                List<TransientBlockRecord> list = new ArrayList<>();
                for (Map.Entry<BlockPos, TransientBlock> entry : tracker.blocks.entrySet()) {
                    list.add(new TransientBlockRecord(
                            entry.getKey(),
                            entry.getValue().originalState,
                            Optional.ofNullable(entry.getValue().blockEntityData),
                            entry.getValue().decayStartTick,
                            entry.getValue().expiryTick
                    ));
                }
                return list;
            })
    ).apply(instance, list -> {
        TransientBlockTracker tracker = new TransientBlockTracker();
        for (TransientBlockRecord record : list) {
            tracker.blocks.put(record.pos(), new TransientBlock(
                    record.state(),
                    record.blockEntity().orElse(null),
                    record.decayStartTick(),
                    record.expiryTick()
            ));
            tracker.addToSchedule(record.pos(), record.expiryTick());
        }
        return tracker;
    }));

    public static final SavedDataType<TransientBlockTracker> TYPE = new SavedDataType<TransientBlockTracker>(
            DATA_NAME,
            TransientBlockTracker::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    /**
     * Primary storage mapping block coordinates to their respective transient state records.
     */
    private final Map<BlockPos, TransientBlock> blocks = new HashMap<>();

    /**
     * Time-bucketed schedule mapping execution ticks to collections of coordinates.
     * Permits retrieval of all blocks expiring on a specific tick without full collection iteration.
     */
    private final TreeMap<Long, List<BlockPos>> expirySchedule = new TreeMap<>();

    /**
     * Retrieves the active TransientBlockTracker instance for the specified server level.
     * Instantiates a new tracker if one does not exist.
     *
     * @param level The target server level.
     * @return The bound TransientBlockTracker instance.
     */
    public static TransientBlockTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Registers a block position for temporary modification.
     * Extends existing expiration parameters if the block is already tracked.
     *
     * @param level           The server level containing the block.
     * @param pos             The coordinate position of the block.
     * @param originalState   The block state to restore upon expiration.
     * @param blockEntityData The NBT data of the block entity to restore, or null.
     * @param decayStartTick  The absolute game time tick when the decay animation initiates.
     * @param expiryTick      The absolute game time tick when restoration occurs.
     */
    public void addBlock(ServerLevel level, BlockPos pos, BlockState originalState, CompoundTag blockEntityData, long decayStartTick, long expiryTick) {
        TransientBlock existing = this.blocks.get(pos);
        if (existing != null) {
            this.removeFromSchedule(pos, existing.expiryTick);

            existing.decayStartTick = Math.max(existing.decayStartTick, decayStartTick);
            existing.expiryTick = Math.max(existing.expiryTick, expiryTick);

            if (existing.lastProgress >= 0) {
                level.destroyBlockProgress(pos.hashCode(), pos, -1);
                existing.lastProgress = -1;
            }

            this.addToSchedule(pos, existing.expiryTick);
        } else {
            this.blocks.put(pos, new TransientBlock(originalState, blockEntityData, decayStartTick, expiryTick));
            this.addToSchedule(pos, expiryTick);
        }
        this.setDirty();
    }

    /**
     * Appends a block position to the specified temporal schedule bucket.
     *
     * @param pos        The block coordinate.
     * @param expiryTick The target expiration tick.
     */
    private void addToSchedule(BlockPos pos, long expiryTick) {
        this.expirySchedule.computeIfAbsent(expiryTick, k -> new ArrayList<>()).add(pos);
    }

    /**
     * Removes a block position from the specified temporal schedule bucket.
     * Deletes the bucket if it becomes empty.
     *
     * @param pos        The block coordinate.
     * @param expiryTick The target expiration tick.
     */
    private void removeFromSchedule(BlockPos pos, long expiryTick) {
        List<BlockPos> scheduled = this.expirySchedule.get(expiryTick);
        if (scheduled != null) {
            scheduled.remove(pos);
            if (scheduled.isEmpty()) {
                this.expirySchedule.remove(expiryTick);
            }
        }
    }

    /**
     * Executes the primary evaluation loop. Process block expirations and decay animations.
     * Must be invoked per tick per server level.
     *
     * @param level The executing server level.
     */
    public void tick(ServerLevel level) {
        if (this.blocks.isEmpty()) return;

        long currentTick = level.getGameTime();
        boolean changed = false;

        Iterator<Map.Entry<Long, List<BlockPos>>> scheduleIterator = this.expirySchedule.headMap(currentTick, true).entrySet().iterator();
        while (scheduleIterator.hasNext()) {
            Map.Entry<Long, List<BlockPos>> entry = scheduleIterator.next();
            List<BlockPos> expiringPositions = entry.getValue();

            for (BlockPos pos : expiringPositions) {
                TransientBlock block = this.blocks.remove(pos);
                if (block == null) continue;

                if (level.isLoaded(pos)) {
                    level.setBlockAndUpdate(pos, block.originalState);

                    if (block.blockEntityData != null) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) {
                            be.loadWithComponents(block.blockEntityData, level.registryAccess());
                            be.setChanged();
                        }
                    }

                    level.destroyBlockProgress(pos.hashCode(), pos, -1);
                    changed = true;
                }
            }
            scheduleIterator.remove();
        }

        for (Map.Entry<BlockPos, TransientBlock> entry : this.blocks.entrySet()) {
            TransientBlock block = entry.getValue();
            if (currentTick >= block.decayStartTick) {
                long totalDecay = block.expiryTick - block.decayStartTick;
                if (totalDecay > 0) {
                    long elapsedDecay = currentTick - block.decayStartTick;
                    int progress = (int) ((elapsedDecay * 10) / totalDecay);

                    if (progress != block.lastProgress && progress >= 0 && progress < 10) {
                        BlockPos pos = entry.getKey();
                        level.destroyBlockProgress(pos.hashCode(), pos, progress);
                        block.lastProgress = progress;
                    }
                }
            }
        }

        if (changed) {
            this.setDirty();
        }
    }

    /**
     * Internal data structure representing a tracked block state and its expiration parameters.
     */
    private static class TransientBlock {
        final BlockState originalState;
        final CompoundTag blockEntityData;
        long decayStartTick;
        long expiryTick;
        int lastProgress = -1;

        TransientBlock(BlockState originalState, CompoundTag blockEntityData, long decayStartTick, long expiryTick) {
            this.originalState = originalState;
            this.blockEntityData = blockEntityData;
            this.decayStartTick = decayStartTick;
            this.expiryTick = expiryTick;
        }
    }

    /**
     * DTO mapping used solely for serializing tracked block data to disk.
     */
    private record TransientBlockRecord(
            BlockPos pos,
            BlockState state,
            Optional<CompoundTag> blockEntity,
            long decayStartTick,
            long expiryTick
    ) {
        public static final Codec<TransientBlockRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(r -> r.pos().getX()),
                Codec.INT.fieldOf("y").forGetter(r -> r.pos().getY()),
                Codec.INT.fieldOf("z").forGetter(r -> r.pos().getZ()),
                BlockState.CODEC.optionalFieldOf("state", Blocks.AIR.defaultBlockState()).forGetter(TransientBlockRecord::state),
                CompoundTag.CODEC.optionalFieldOf("block_entity").forGetter(TransientBlockRecord::blockEntity),
                Codec.LONG.optionalFieldOf("decay_start", -1L).forGetter(TransientBlockRecord::decayStartTick),
                Codec.LONG.optionalFieldOf("start", -1L).forGetter(r -> -1L),
                Codec.LONG.fieldOf("expiry").forGetter(TransientBlockRecord::expiryTick)
        ).apply(instance, (x, y, z, state, be, decayStart, start, expiry) -> {
            long actualDecay = decayStart != -1L ? decayStart : (start != -1L ? start : 0L);
            return new TransientBlockRecord(new BlockPos(x, y, z), state, be, actualDecay, expiry);
        }));
    }
}