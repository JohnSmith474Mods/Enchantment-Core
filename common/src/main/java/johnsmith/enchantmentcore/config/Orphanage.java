package johnsmith.enchantmentcore.config;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import johnsmith.configoverhauled.api.data.ConfigDescription;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.config.OrphanHandler;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class Orphanage implements OrphanHandler {
    private static final Set<ConfigDescription> UNNOTIFIED_ORPHANS = ConcurrentHashMap.newKeySet();

    public ConfigDescription adopt(ConfigDescription orphan) {
        if (UNNOTIFIED_ORPHANS.add(orphan)) {
            Constants.LOG.error("Datapack Configuration Error: ConfigManager for mod id '{}' not found. " +
                    "Adopting orphaned property '{}' into {}.", orphan.modId(), orphan, Constants.MOD_NAME);
        }

        return new ConfigDescription(
                Constants.MOD_ID,
                Config.ORPHANS.id(),
                orphan.modId() + "." + orphan.category(),
                orphan.group() + "." + orphan.property()
        );
    }

    public static void notifyPlayerIfPrivileged(ServerPlayer player) {
        if (UNNOTIFIED_ORPHANS.isEmpty() || !player.hasPermissions(2)) {
            return;
        }

        int count = UNNOTIFIED_ORPHANS.size();
        String noun = count == 1 ? "property" : "properties";

        MutableComponent message = Component.literal(String.format("[%s] ", Constants.MOD_NAME))
                .append(Component.literal(String.format("Datapack Error: Adopted %d orphaned config %s. Find them in the ", count, noun))
                        .withColor(Config.ALERT_COLOR.get()))
                .append(Component.literal(Constants.MOD_NAME))
                .append(Component.literal(" config screen under the ")
                        .withColor(Config.ALERT_COLOR.get()))
                .append(Component.translatable(Config.ORPHANS.translationKey()).getString())
                .append(Component.literal(" tab.")
                        .withColor(Config.ALERT_COLOR.get()));

        if (player.getServer().isDedicatedServer()) {
            message.append(Component.literal("\nCheck server logs.").withColor(Config.ALERT_COLOR.get()));
        } else {
            String targetDir = new File("logs").getAbsolutePath();

            message.append(Component.literal("\nClick to copy log path to clipboard.")
                    .withStyle(style -> style.withUnderlined(true)
                            .withColor(Config.LINK_COLOR.get())
                            .withClickEvent(new ClickEvent.CopyToClipboard(targetDir))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy directory path.")))));
        }

        player.sendSystemMessage(message);
        UNNOTIFIED_ORPHANS.clear();
    }
}