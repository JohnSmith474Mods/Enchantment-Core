package johnsmith.enchantmentcore.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.JsonOps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.registry.DataTransformerRegistry;

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

public class ImportCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final SuggestionProvider<CommandSourceStack> SOURCE_SUGGESTIONS = (context, builder) -> {
        Registry<Enchantment> registry = context.getSource().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Set<String> namespaces = registry.keySet().stream()
                .map(ResourceLocation::getNamespace)
                .collect(Collectors.toSet());
        return SharedSuggestionProvider.suggest(namespaces, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> TARGET_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(ConfigRegistry.getDependentModIDs(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Constants.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("import")
                        .then(Commands.argument("source_namespace", StringArgumentType.word())
                                .suggests(SOURCE_SUGGESTIONS)
                                .then(Commands.argument("target_namespace", StringArgumentType.word())
                                        .suggests(TARGET_SUGGESTIONS)
                                        .executes(context -> execute(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "source_namespace"),
                                                StringArgumentType.getString(context, "target_namespace")
                                        ))
                                )
                        )
                )
        );
    }

    private static int execute(CommandSourceStack source, String sourceNamespace, String targetNamespace) {
        Registry<Enchantment> registry = source.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Path outputDir = Paths.get("enchantment_core", "import", sourceNamespace, "enchantment");

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Directory generation failed: " + e.getMessage()));
            return 0;
        }

        int processedCount = 0;
        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!id.getNamespace().equals(sourceNamespace)) {
                continue;
            }

            JsonElement encoded = Enchantment.DIRECT_CODEC.encodeStart(
                    source.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                    entry.getValue()
            ).getOrThrow();

            if (!encoded.isJsonObject()) {
                continue;
            }

            JsonObject root = encoded.getAsJsonObject();
            String name = id.getPath();

            for (DataTransformerRegistry.Transformer transformer : DataTransformerRegistry.getTransformers()) {
                transformer.applyImport(root, targetNamespace, name);
            }

            File outputFile = outputDir.resolve(name + ".json").toFile();
            try (FileWriter writer = new FileWriter(outputFile)) {
                GSON.toJson(root, writer);
                processedCount++;
            } catch (IOException e) {
                Constants.LOG.error("I/O write failure for {}", id, e);
            }
        }

        final int finalCount = processedCount;
        String absolutePath = outputDir.toAbsolutePath().toString();

        source.sendSuccess(() -> Component.literal("Conversion complete. Modified " + finalCount + " records at\n")
                .append(Component.literal(absolutePath)
                        .withStyle(style -> style
                                .withColor(Config.LINK_COLOR.get())
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, absolutePath))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open directory")))
                        )
                ), true);
        return finalCount;
    }
}