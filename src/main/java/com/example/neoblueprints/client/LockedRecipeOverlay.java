package com.example.neoblueprints.client;

import com.example.neoblueprints.config.BlueprintConfig;
import com.example.neoblueprints.config.BlueprintDefinition;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

/**
 * Client-only render hook that fades out + draws a red X on the result slot of
 * any open crafting screen when the would-be recipe is locked behind a blueprint
 * the local player has not yet unlocked. Also injects a tooltip line explaining
 * the lock when the player hovers the result slot.
 */
public final class LockedRecipeOverlay {

    private LockedRecipeOverlay() {}

    /** Returns the blueprint id required for the active result slot's recipe, or null if none/unlocked/no-recipe. */
    public static ResourceLocation lockedBlueprintFor(AbstractContainerScreen<?> screen, Player player) {
        AbstractContainerMenu menu = screen.getMenu();
        ResourceLocation recipeId = currentRecipeId(menu, player);
        if (recipeId == null) return null;
        if (!BlueprintConfig.isLocked(recipeId)) return null;
        if (ClientUnlockCache.isUnlocked(recipeId)) return null;
        return BlueprintConfig.getBlueprintFor(recipeId);
    }

    private static ResourceLocation currentRecipeId(AbstractContainerMenu menu, Player player) {
        if (menu == null) return null;
        net.minecraft.world.inventory.CraftingContainer matrix;
        if (menu instanceof CraftingMenu cm) {
            matrix = cm.craftSlots;
        } else if (menu instanceof InventoryMenu im) {
            matrix = im.craftSlots;
        } else {
            return null;
        }
        if (matrix == null || matrix.isEmpty()) return null;
        RecipeManager rm = player.level().getRecipeManager();
        Optional<RecipeHolder<CraftingRecipe>> opt =
                rm.getRecipeFor(RecipeType.CRAFTING, matrix.asCraftInput(), player.level());
        return opt.map(RecipeHolder::id).orElse(null);
    }

    /** Render the faded result + red X over the result slot if the recipe is locked. */
    public static void renderForScreen(GuiGraphics graphics, AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(screen instanceof CraftingScreen) && !(screen instanceof InventoryScreen)) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (menu.slots.isEmpty()) return;
        Slot result = menu.slots.get(0);

        ResourceLocation recipeId = currentRecipeId(menu, mc.player);
        if (recipeId == null) return;
        if (!BlueprintConfig.isLocked(recipeId)) return;
        if (ClientUnlockCache.isUnlocked(recipeId)) return;

        // Compute the recipe's would-be result so we can show the player what they CAN'T craft yet.
        ItemStack ghost = result.getItem();
        if (ghost.isEmpty()) {
            Optional<RecipeHolder<CraftingRecipe>> opt = mc.player.level().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING,
                            (menu instanceof CraftingMenu cm ? cm.craftSlots : ((InventoryMenu) menu).craftSlots).asCraftInput(),
                            mc.player.level());
            if (opt.isPresent()) {
                ghost = opt.get().value().getResultItem(mc.player.level().registryAccess());
            }
        }
        if (ghost.isEmpty()) return;

        // ContainerScreenEvent.Render.Foreground fires after the pose matrix has
        // already been translated by (guiLeft, guiTop), so slot coordinates are
        // GUI-relative — do NOT add guiLeft/guiTop again.
        int x = result.x;
        int y = result.y;

        // Faded item ghost.
        RenderSystem.enableBlend();
        graphics.setColor(1f, 1f, 1f, 0.45f);
        graphics.renderItem(ghost, x, y);
        graphics.setColor(1f, 1f, 1f, 1f);

        // Dim square over the slot.
        graphics.fill(x, y, x + 16, y + 16, 0x66000000);

        // Red X overlay (two diagonal lines, drawn as 1px-wide rotated rectangles via fill quads).
        drawX(graphics, x, y);
        RenderSystem.disableBlend();
    }

    /** If the mouse is hovering the locked result slot, replace the tooltip with a clear lock explanation. */
    public static List<Component> tooltipForLockedSlot(AbstractContainerScreen<?> screen, Slot hovered) {
        if (hovered == null || hovered.index != 0) return null;
        if (!(screen instanceof CraftingScreen) && !(screen instanceof InventoryScreen)) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        ResourceLocation recipeId = currentRecipeId(screen.getMenu(), mc.player);
        if (recipeId == null) return null;
        if (!BlueprintConfig.isLocked(recipeId)) return null;
        if (ClientUnlockCache.isUnlocked(recipeId)) return null;

        ResourceLocation blueprintId = BlueprintConfig.getBlueprintFor(recipeId);
        BlueprintDefinition def = blueprintId != null ? BlueprintConfig.getDefinition(blueprintId) : null;
        Component blueprintName = def != null
                ? Component.literal(def.name()).withStyle(def.rarity().color())
                : (blueprintId != null
                    ? Component.literal(blueprintId.toString()).withStyle(ChatFormatting.AQUA)
                    : Component.translatable("tooltip.neoblueprints.locked.unknown_blueprint").withStyle(ChatFormatting.GRAY));

        return List.of(
                Component.translatable("tooltip.neoblueprints.locked.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.translatable("tooltip.neoblueprints.locked.requires", blueprintName).withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.neoblueprints.locked.note").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );
    }

    private static void drawX(GuiGraphics graphics, int x, int y) {
        int red = 0xFFFF3333;
        int shadow = 0xCC000000;
        // Draw a thicker X by stacking 2-3 pixel-wide diagonals.
        for (int i = 0; i < 16; i++) {
            // top-left -> bottom-right diagonal, 2px thick
            graphics.fill(x + i, y + i, x + i + 2, y + i + 2, shadow);
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, red);
            // top-right -> bottom-left diagonal, 2px thick
            int xr = x + 15 - i;
            graphics.fill(xr, y + i, xr + 2, y + i + 2, shadow);
            graphics.fill(xr, y + i, xr + 1, y + i + 1, red);
        }
    }
}
