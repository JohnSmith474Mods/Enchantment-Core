package johnsmith.enchantmentcore.client.debug;

import net.minecraft.world.entity.Entity;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.LocalVolume;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpellFieldDebugTracker {
    private static final Map<Integer, TrackedSpellField> ACTIVE_SHAPES = new ConcurrentHashMap<>();

    public static void addShapes(Entity entity, List<LocalVolume> volumes, List<FieldAxis> vectorFields, int enchantmentLevel, int ticks) {
        ACTIVE_SHAPES.put(entity.getId(), new TrackedSpellField(entity, volumes, vectorFields, enchantmentLevel, ticks));
    }

    public static void tick() {
        Iterator<Map.Entry<Integer, TrackedSpellField>> iterator = ACTIVE_SHAPES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedSpellField> entry = iterator.next();
            int newTicks = entry.getValue().ticksRemaining - 1;

            if (newTicks <= 0) {
                iterator.remove();
            } else {
                entry.getValue().ticksRemaining = newTicks;
            }
        }
    }

    public static Map<Integer, TrackedSpellField> getActiveShapes() {
        return ACTIVE_SHAPES;
    }

    public static class TrackedSpellField {
        public final Entity anchorEntity;
        public final List<LocalVolume> volumes;
        public final List<FieldAxis> vectorFields;
        public final int enchantmentLevel;
        public int ticksRemaining;

        public TrackedSpellField(Entity anchorEntity, List<LocalVolume> volumes, List<FieldAxis> vectorFields, int enchantmentLevel, int ticksRemaining) {
            this.anchorEntity = anchorEntity;
            this.volumes = volumes;
            this.vectorFields = vectorFields;
            this.enchantmentLevel = enchantmentLevel;
            this.ticksRemaining = ticksRemaining;
        }
    }
}