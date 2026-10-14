package johnsmith.enchantmentcore.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateDescriptionKeysCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final SuggestionProvider<CommandSourceStack> SOURCE_SUGGESTIONS = (context, builder) -> {
        Registry<Enchantment> registry = context.getSource().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Set<String> namespaces = registry.keySet().stream()
                .map(ResourceLocation::getNamespace)
                .collect(Collectors.toSet());
        return SharedSuggestionProvider.suggest(namespaces, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Constants.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("generate_description_keys")
                        .then(Commands.argument("target_namespace", StringArgumentType.word())
                                .suggests(SOURCE_SUGGESTIONS)
                                .executes(context -> execute(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "target_namespace")
                                ))
                        )
                )
        );
    }

    private static int execute(CommandSourceStack source, String targetNamespace) {
        Registry<Enchantment> registry = source.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Path outputDir = Paths.get("enchantment_core", "export", targetNamespace);

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Directory generation failed: " + e.getMessage()));
            return 0;
        }

        JsonObject root = new JsonObject();
        int processedCount = 0;

        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!id.getNamespace().equals(targetNamespace)) {
                continue;
            }

            Enchantment enchantment = entry.getValue();
            Map<String, LevelBasedValue> descriptionArgs = enchantment.effects().get(EnchantmentEffectComponents.DESCRIPTION_ARGS);

            if (descriptionArgs != null && !descriptionArgs.isEmpty()) {
                // Idiomatic standard translation key format: "enchantment.namespace.path.desc"
                String translationKey = "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc";

                String pointers = descriptionArgs.keySet().stream()
                        .map(key -> "<val:" + key + ">")
                        .collect(Collectors.joining(" "));

                root.addProperty(translationKey, pointers);
                processedCount++;
            }
        }

        if (processedCount == 0) {
            source.sendSuccess(() -> Component.literal("No enchantments with PointerValues found in namespace: " + targetNamespace), false);
            return 0;
        }

        File outputFile = outputDir.resolve("description_keys.json").toFile();
        try (FileWriter writer = new FileWriter(outputFile)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            Constants.LOG.error("I/O write failure for description_keys.json", e);
            source.sendFailure(Component.literal("Failed to write description_keys.json"));
            return 0;
        }

        final int finalCount = processedCount;
        String absolutePath = outputDir.toAbsolutePath().toString();
        source.sendSuccess(() -> Component.literal("Generated description keys for " + finalCount + " enchantments at\n")
                .append(Component.literal(absolutePath)
                        .withStyle(style -> style
                                .withColor(Config.LINK_COLOR.get())
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, absolutePath))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy directory path")))
                        )
                ), true);

        return finalCount;
    }
}