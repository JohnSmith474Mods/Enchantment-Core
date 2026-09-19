package johnsmith.enchantmentcore.event;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;
import johnsmith.enchantmentcore.util.TransientBlockTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeGameEvents {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Orphanage.notifyPlayerIfPrivileged(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level instanceof ServerLevel serverLevel) {
            SpellFieldTaskScheduler.tick(serverLevel);
            TransientBlockTracker.get(serverLevel).tick(serverLevel);
        }
    }
}