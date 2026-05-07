package com.example.neoblueprints.inventory;

import com.example.neoblueprints.config.BlueprintConfig;
import com.example.neoblueprints.data.UnlockHelper;
import com.example.neoblueprints.event.CraftingLockHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;

/**
 * Replacement for the vanilla {@link ResultSlot} that refuses pickup when the
 * matched crafting recipe is locked and not yet unlocked for the player.
 *
 * <p>Server-authoritative — on the client we always return {@code true} to allow
 * normal UI prediction; the server check is the source of truth. If the server
 * rejects, the client's prediction snaps back on the next sync.
 *
 * <p>This is the primary enforcement point. Because {@code mayPickup} is consulted
 * before the result is removed from the slot, locked crafts cannot complete and
 * no items are ever consumed or duplicated.
 */
public class LockedResultSlot extends ResultSlot {

    private final CraftingContainer matrix;

    public LockedResultSlot(Player player, CraftingContainer matrix, Container resultContainer, int slot, int x, int y) {
        super(player, matrix, resultContainer, slot, x, y);
        this.matrix = matrix;
    }

    @Override
    public boolean mayPickup(Player player) {
        if (!super.mayPickup(player)) return false;
        if (player.level().isClientSide) return true;

        Optional<RecipeHolder<CraftingRecipe>> recipeOpt = player.level()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, matrix.asCraftInput(), player.level());
        if (recipeOpt.isEmpty()) return true;

        ResourceLocation recipeId = recipeOpt.get().id();
        if (!BlueprintConfig.isLocked(recipeId)) return true;
        if (UnlockHelper.isUnlocked(player, recipeId)) return true;

        CraftingLockHandler.notifyLocked(player, recipeId);
        return false;
    }
}
