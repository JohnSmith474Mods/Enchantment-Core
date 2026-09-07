package johnsmith.enchantmentcore.event;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class NeoForgeGameEvents {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Orphanage.notifyPlayerIfPrivileged(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SpellFieldTaskScheduler.tick(serverLevel);
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SpellFieldTaskScheduler.tick(serverLevel);
            johnsmith.enchantmentcore.util.TransientBlockTracker.get(serverLevel).tick(serverLevel);
        }
    }
}