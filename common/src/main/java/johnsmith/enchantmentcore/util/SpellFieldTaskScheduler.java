package johnsmith.enchantmentcore.util;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class SpellFieldTaskScheduler {
    private static final Map<ServerLevel, List<ScheduledTask>> TASKS = new WeakHashMap<>();

    public static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        if (delayTicks <= 0) {
            action.run();
            return;
        }

        TASKS.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new ScheduledTask(level.getServer().getTickCount() + delayTicks, action));
    }

    public static void tick(ServerLevel level) {
        List<ScheduledTask> levelTasks = TASKS.get(level);
        if (levelTasks == null || levelTasks.isEmpty()) return;

        int currentTick = level.getServer().getTickCount();
        Iterator<ScheduledTask> iterator = levelTasks.iterator();

        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            if (currentTick >= task.executionTick) {
                task.action.run();
                iterator.remove();
            }
        }
    }

    private record ScheduledTask(int executionTick, Runnable action) {}
}