package com.example.neoblueprints.event;

import com.example.neoblueprints.config.BlueprintConfig;
import com.example.neoblueprints.data.TemporaryUnlockManager;
import com.example.neoblueprints.data.UnlockHelper;
import com.example.neoblueprints.inventory.LockedResultSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side enforcement glue.
 *
 * <p>The actual lock check lives in {@link LockedResultSlot#mayPickup(Player)}.
 * This handler's job is to make sure every relevant crafting menu has its
 * vanilla {@link ResultSlot} swapped out for the locked variant, plus the
 * tooltip enrichment and chat-message rate limiting.
 */
public final class CraftingLockHandler {

    private CraftingLockHandler() {}

    private static final Map<UUID, Long> LAST_NOTIFY = new ConcurrentHashMap<>();
    private static final long NOTIFY_COOLDOWN_TICKS = 20L;

    /** Sends a rate-limited "Blueprint required" chat message to the player. */
    public static void notifyLocked(Player player, ResourceLocation recipeId) {
        long now = player.level().getGameTime();
        Long last = LAST_NOTIFY.get(player.getUUID());
        if (last != null && now - last < NOTIFY_COOLDOWN_TICKS) return;
        LAST_NOTIFY.put(player.getUUID(), now);

        ResourceLocation blueprintId = BlueprintConfig.getBlueprintFor(recipeId);
        Component msg = blueprintId != null
                ? Component.translatable("message.neoblueprints.recipe_locked", blueprintId.toString())
                : Component.translatable("message.neoblueprints.recipe_locked.short");
        player.sendSystemMessage(msg.copy().withStyle(ChatFormatting.RED));
    }

    /**
     * Once per tick, ensure both the player's default 2x2 inventory crafting
     * result slot and any open external crafting menu's result slot are wrapped
     * by {@link LockedResultSlot}. The check is a single instanceof comparison
     * so cost is negligible.
     */
    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer)) return;

        AbstractContainerMenu invMenu = player.inventoryMenu;
        ensureLockedResultSlot(invMenu, player);

        AbstractContainerMenu open = player.containerMenu;
        if (open != null && open != invMenu) {
            ensureLockedResultSlot(open, player);
        }
    }

    private static void ensureLockedResultSlot(AbstractContainerMenu menu, Player player) {
        if (menu == null || menu.slots.isEmpty()) return;
        Slot first = menu.slots.get(0);
        if (first instanceof LockedResultSlot) return;
        if (!(first instanceof ResultSlot)) return;

        if (menu instanceof InventoryMenu im) {
            LockedResultSlot replacement = new LockedResultSlot(
                    player, im.craftSlots, first.container, 0, first.x, first.y);
            replacement.index = first.index;
            menu.slots.set(0, replacement);
        } else if (menu instanceof CraftingMenu cm) {
            LockedResultSlot replacement = new LockedResultSlot(
                    player, cm.craftSlots, first.container, 0, first.x, first.y);
            replacement.index = first.index;
            menu.slots.set(0, replacement);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_NOTIFY.remove(event.getEntity().getUUID());
        TemporaryUnlockManager.clearPlayer(event.getEntity().getUUID());
    }

    /**
     * Adds a "Blueprint required" tooltip line to items whose recipe is locked
     * and not yet unlocked for the viewing player.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        var level = player.level();
        if (level == null) return;

        for (ResourceLocation recipeId : BlueprintConfig.getLockedRecipes()) {
            if (UnlockHelper.isUnlocked(player, recipeId)) continue;
            var holder = level.getRecipeManager().byKey(recipeId).orElse(null);
            if (holder == null) continue;
            ItemStack out = holder.value().getResultItem(level.registryAccess());
            if (!out.isEmpty() && ItemStack.isSameItem(out, stack)) {
                event.getToolTip().add(Component.translatable("tooltip.neoblueprints.locked")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                break;
            }
        }
    }
}
