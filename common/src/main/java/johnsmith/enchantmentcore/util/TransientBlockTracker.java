package johnsmith.enchantmentcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Manages the temporary modification and restoration of block states within a server level.
 * Stores original block states and block entity data. Restores data automatically when the defined
 * expiration tick occurs. Utilizes a time-bucketed schedule map to optimize expiration queries.
 */
public class TransientBlockTracker extends SavedData {
    private static final String DATA_NAME = "enchantment_core_transient_blocks";

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
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TransientBlockTracker::new, TransientBlockTracker::load, null),
                DATA_NAME
        );
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
            // Remove block from current schedule bucket before modifying expiration parameters.
            this.removeFromSchedule(pos, existing.expiryTick);

            existing.decayStartTick = Math.max(existing.decayStartTick, decayStartTick);
            existing.expiryTick = Math.max(existing.expiryTick, expiryTick);

            // Reset block break animation if it is actively rendering.
            if (existing.lastProgress >= 0) {
                level.destroyBlockProgress(pos.hashCode(), pos, -1);
                existing.lastProgress = -1;
            }

            // Insert block into the new schedule bucket.
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
            // Delete empty temporal buckets to prevent memory leaks.
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

        // headMap isolates temporal buckets strictly less than or equal to currentTick.
        // Bypasses iteration over future scheduled events.
        Iterator<Map.Entry<Long, List<BlockPos>>> scheduleIterator = this.expirySchedule.headMap(currentTick, true).entrySet().iterator();
        while (scheduleIterator.hasNext()) {
            Map.Entry<Long, List<BlockPos>> entry = scheduleIterator.next();
            List<BlockPos> expiringPositions = entry.getValue();

            for (BlockPos pos : expiringPositions) {
                TransientBlock block = this.blocks.remove(pos);
                if (block == null) continue;

                if (level.isLoaded(pos)) {
                    // Restore original block state.
                    level.setBlockAndUpdate(pos, block.originalState);

                    // Restore block entity data if present.
                    if (block.blockEntityData != null) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) {
                            be.loadWithComponents(block.blockEntityData, level.registryAccess());
                            be.setChanged();
                        }
                    }

                    // Terminate block break animation.
                    level.destroyBlockProgress(pos.hashCode(), pos, -1);
                    changed = true;
                }
            }
            // Remove the processed temporal bucket from the schedule map.
            scheduleIterator.remove();
        }

        // Iteration required to calculate and transmit dynamic block-breaking animation progress.
        for (Map.Entry<BlockPos, TransientBlock> entry : this.blocks.entrySet()) {
            TransientBlock block = entry.getValue();
            if (currentTick >= block.decayStartTick) {
                long totalDecay = block.expiryTick - block.decayStartTick;
                if (totalDecay > 0) {
                    long elapsedDecay = currentTick - block.decayStartTick;

                    // Calculate linear progress integer from 0 to 9.
                    int progress = (int) ((elapsedDecay * 10) / totalDecay);

                    // Transmit packet only if progress increments.
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
     * Serializes the active tracker state into NBT format for disk storage.
     *
     * @param tag      The root compound tag.
     * @param provider The registry lookup provider.
     * @return The populated compound tag.
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, TransientBlock> entry : this.blocks.entrySet()) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("x", entry.getKey().getX());
            blockTag.putInt("y", entry.getKey().getY());
            blockTag.putInt("z", entry.getKey().getZ());
            blockTag.put("state", NbtUtils.writeBlockState(entry.getValue().originalState));
            blockTag.putLong("decay_start", entry.getValue().decayStartTick);
            blockTag.putLong("expiry", entry.getValue().expiryTick);

            if (entry.getValue().blockEntityData != null) {
                blockTag.put("block_entity", entry.getValue().blockEntityData);
            }

            list.add(blockTag);
        }
        tag.put("blocks", list);
        return tag;
    }

    /**
     * Deserializes tracker state from NBT format. Reconstructs the temporal schedule map.
     *
     * @param tag      The root compound tag containing saved state.
     * @param provider The registry lookup provider.
     * @return The reconstructed TransientBlockTracker instance.
     */
    public static TransientBlockTracker load(CompoundTag tag, HolderLookup.Provider provider) {
        TransientBlockTracker tracker = new TransientBlockTracker();
        ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
        HolderGetter<Block> blockGetter = provider.lookupOrThrow(Registries.BLOCK);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag blockTag = list.getCompound(i);
            BlockPos pos = new BlockPos(blockTag.getInt("x"), blockTag.getInt("y"), blockTag.getInt("z"));
            BlockState state = NbtUtils.readBlockState(blockGetter, blockTag.getCompound("state"));

            // Support legacy NBT key format "start".
            long decayStart = blockTag.contains("decay_start") ? blockTag.getLong("decay_start") : blockTag.getLong("start");
            long expiry = blockTag.getLong("expiry");
            CompoundTag beData = blockTag.contains("block_entity") ? blockTag.getCompound("block_entity") : null;

            // Correct state resolution failure for air blocks missing Name tag data.
            if (state.isAir() && !blockTag.getCompound("state").contains("Name")) {
                state = Blocks.AIR.defaultBlockState();
            }

            tracker.blocks.put(pos, new TransientBlock(state, beData, decayStart, expiry));

            // Rebuild the temporal map schedule for the loaded block.
            tracker.addToSchedule(pos, expiry);
        }
        return tracker;
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
}