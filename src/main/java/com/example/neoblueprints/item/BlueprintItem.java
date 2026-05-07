package com.example.neoblueprints.item;

import com.example.neoblueprints.config.BlueprintConfig;
import com.example.neoblueprints.config.BlueprintDefinition;
import com.example.neoblueprints.data.UnlockHelper;
import com.example.neoblueprints.registry.ModDataComponents;
import com.example.neoblueprints.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class BlueprintItem extends Item {

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    /** Helper that creates a stamped blueprint item stack with the given blueprint ID. */
    public static ItemStack create(ResourceLocation blueprintId) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT.get());
        stack.set(ModDataComponents.BLUEPRINT_ID.get(), blueprintId);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation blueprintId = stack.get(ModDataComponents.BLUEPRINT_ID.get());

        if (blueprintId == null) {
            if (!level.isClientSide) {
                player.sendSystemMessage(
                        Component.translatable("item.neoblueprints.blueprint.no_id")
                                .withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        List<ResourceLocation> recipes = BlueprintConfig.getRecipesFor(blueprintId);
        if (recipes.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("item.neoblueprints.blueprint.unknown",
                                    blueprintId.toString())
                            .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        int newlyUnlocked = 0;
        for (ResourceLocation recipeId : recipes) {
            if (UnlockHelper.unlock(player, recipeId)) {
                newlyUnlocked++;
            }
        }

        if (newlyUnlocked == 0) {
            player.sendSystemMessage(
                    Component.translatable("item.neoblueprints.blueprint.already_unlocked")
                            .withStyle(ChatFormatting.YELLOW));
            return InteractionResultHolder.fail(stack);
        }

        player.sendSystemMessage(
                Component.translatable("item.neoblueprints.blueprint.unlocked",
                                BlueprintConfig.getDisplayName(blueprintId), newlyUnlocked)
                        .withStyle(ChatFormatting.GREEN));

        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = stack.get(ModDataComponents.BLUEPRINT_ID.get());
        if (id == null) {
            return super.getName(stack);
        }
        BlueprintDefinition def = BlueprintConfig.getDefinition(id);
        if (def != null) {
            return Component.literal(def.name()).withStyle(def.rarity().color());
        }
        // Unknown blueprint id (e.g. config not synced yet or stale stack) — fall back to id-based name.
        return Component.translatable(this.getDescriptionId(stack))
                .append(": ")
                .append(Component.literal(id.getPath()).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = stack.get(ModDataComponents.BLUEPRINT_ID.get());
        if (id == null) {
            tooltip.add(Component.translatable("item.neoblueprints.blueprint.tooltip.empty")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        BlueprintDefinition def = BlueprintConfig.getDefinition(id);
        if (def != null) {
            tooltip.add(Component.translatable("item.neoblueprints.blueprint.tooltip.rarity", def.rarity().displayName())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.neoblueprints.blueprint.tooltip.id", id.toString())
                .withStyle(ChatFormatting.DARK_GRAY));

        List<ResourceLocation> recipes = BlueprintConfig.getRecipesFor(id);
        if (!recipes.isEmpty()) {
            tooltip.add(Component.translatable("item.neoblueprints.blueprint.tooltip.unlocks")
                    .withStyle(ChatFormatting.GRAY));
            for (ResourceLocation r : recipes) {
                tooltip.add(Component.literal(" - " + r).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("item.neoblueprints.blueprint.tooltip.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
