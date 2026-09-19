package johnsmith.enchantmentcore.event;

import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;
import johnsmith.enchantmentcore.util.TransientBlockTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class ForgeGameEvents {

    /**
     * Binds internal event listeners explicitly to their static buses.
     */
    public static void initialize() {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ForgeGameEvents::onPlayerJoin);
        TickEvent.LevelTickEvent.Post.BUS.addListener(ForgeGameEvents::onLevelTick);
    }

    private static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Orphanage.notifyPlayerIfPrivileged(serverPlayer);
        }
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level() instanceof ServerLevel serverLevel) {
            SpellFieldTaskScheduler.tick(serverLevel);
            TransientBlockTracker.get(serverLevel).tick(serverLevel);
        }
    }
}