package johnsmith.enchantmentcore.command;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.JsonOps;

import java.util.Collection;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.enchantment.spellfield.SpellFieldComponent;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;

public class SpellFieldCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Constants.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spellfield")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("payload", StringArgumentType.greedyString())
                                                .executes(context -> execute(
                                                        context.getSource(),
                                                        EntityArgument.getEntities(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "level"),
                                                        StringArgumentType.getString(context, "payload")
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private static int execute(CommandSourceStack source, Collection<? extends Entity> targets, int level, String payload) {
        SpellFieldComponent spellField;
        try {
            RegistryOps<com.google.gson.JsonElement> ops = source.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            spellField = SpellFieldComponent.CODEC.codec()
                    .parse(ops, JsonParser.parseString(payload))
                    .getOrThrow(IllegalStateException::new);
        } catch (Exception e) {
            source.sendFailure(Component.literal("SpellField JSON parsing failed: " + e.getMessage()));
            return 0;
        }

        ServerLevel serverLevel = source.getLevel();
        int successCount = 0;

        for (Entity target : targets) {
            LivingEntity owner = target instanceof LivingEntity living ? living : null;
            EnchantedItemInUse context = new EnchantedItemInUse(ItemStack.EMPTY, EquipmentSlot.MAINHAND, owner);

            spellField.apply(serverLevel, level, context, target, target.position());
            successCount++;
        }

        final int finalSuccessCount = successCount;
        source.sendSuccess(() -> Component.literal("SpellField executed on " + finalSuccessCount + " entities."), true);
        return finalSuccessCount;
    }
}