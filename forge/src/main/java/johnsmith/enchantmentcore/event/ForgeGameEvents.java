package johnsmith.enchantmentcore.event;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            SpellFieldTaskScheduler.tick(serverLevel);
        }
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            SpellFieldTaskScheduler.tick(serverLevel);
            johnsmith.enchantmentcore.util.TransientBlockTracker.get(serverLevel).tick(serverLevel);
        }
    }
}