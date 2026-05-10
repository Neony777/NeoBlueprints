package com.example.neoblueprints.command;

import com.example.neoblueprints.config.BlueprintConfig;
import com.example.neoblueprints.data.UnlockHelper;
import com.example.neoblueprints.item.BlueprintItem;
import com.example.neoblueprints.network.NetworkHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class BlueprintCommand {

    private BlueprintCommand() {}

    private static final SuggestionProvider<CommandSourceStack> BLUEPRINT_ID_SUGGESTIONS =
            (ctx, builder) -> {
                String input = builder.getRemainingLowerCase();
                for (ResourceLocation id : BlueprintConfig.knownBlueprintIds()) {
                    String full = id.toString();
                    if (full.toLowerCase().startsWith(input)
                            || id.getPath().toLowerCase().startsWith(input)) {
                        builder.suggest(full);
                    }
                }
                return builder.buildFuture();
            };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event);
    }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("blueprint")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("blueprint_id", StringArgumentType.greedyString())
                                        .suggests(BLUEPRINT_ID_SUGGESTIONS)
                                        .executes(BlueprintCommand::give))))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                        .suggests(SuggestionProviders.ALL_RECIPES)
                                        .executes(BlueprintCommand::unlock))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(BlueprintCommand::reset)))
                .then(Commands.literal("reload")
                        .executes(BlueprintCommand::reload));

        event.getDispatcher().register(root);
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        String raw = StringArgumentType.getString(ctx, "blueprint_id");
        ResourceLocation blueprintId = BlueprintConfig.parseId(raw);

        if (blueprintId == null || BlueprintConfig.getRecipesFor(blueprintId).isEmpty()) {
            ctx.getSource().sendFailure(
                    Component.translatable("command.neoblueprints.give.unknown", raw));
            return 0;
        }

        for (ServerPlayer player : players) {
            ItemStack stack = BlueprintItem.create(blueprintId);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "command.neoblueprints.give.success",
                            blueprintId.toString(),
                            player.getName().getString()),
                    true);
        }
        return players.size();
    }

    private static int unlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        ResourceLocation recipeId = ResourceLocationArgument.getId(ctx, "recipe");

        for (ServerPlayer player : players) {
            boolean changed = UnlockHelper.unlock(player, recipeId);
            if (changed) {
                ctx.getSource().sendSuccess(
                        () -> Component.translatable(
                                "command.neoblueprints.unlock.success",
                                recipeId.toString(),
                                player.getName().getString()),
                        true);
            } else {
                ctx.getSource().sendSuccess(
                        () -> Component.translatable(
                                "command.neoblueprints.unlock.already",
                                recipeId.toString(),
                                player.getName().getString()),
                        false);
            }
        }
        return players.size();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        for (Player player : players) {
            UnlockHelper.reset(player);
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            "command.neoblueprints.reset.success",
                            player.getName().getString()),
                    true);
        }
        return players.size();
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        try {
            int count = BlueprintConfig.reload();
            // Re-sync to all online players.
            if (ctx.getSource().getServer() != null) {
                for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                    NetworkHandler.sendBlueprintsTo(p);
                }
            }
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("command.neoblueprints.reload.success", count), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                    Component.translatable("command.neoblueprints.reload.error", e.getMessage()));
            return 0;
        }
    }
}
