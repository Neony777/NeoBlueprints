package com.example.neoblueprints.data;

import com.example.neoblueprints.network.NetworkHandler;
import com.example.neoblueprints.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Convenience accessors over the player's PlayerUnlockData attachment.
 * Mutations automatically push a sync packet to the player so the client UI
 * (locked-recipe overlay) reflects the new state immediately.
 */
public final class UnlockHelper {

    public static PlayerUnlockData get(Player player) {
        return player.getData(ModAttachments.PLAYER_UNLOCKS.get());
    }

    public static boolean isUnlocked(Player player, ResourceLocation recipeId) {
        return get(player).isUnlocked(recipeId);
    }

    /** Returns true if the recipe was newly added (false if already unlocked). */
    public static boolean unlock(Player player, ResourceLocation recipeId) {
        PlayerUnlockData data = get(player);
        boolean changed = data.unlock(recipeId);
        if (changed) {
            player.setData(ModAttachments.PLAYER_UNLOCKS.get(), data);
            syncTo(player);
        }
        return changed;
    }

    public static void reset(Player player) {
        PlayerUnlockData data = get(player);
        data.reset();
        player.setData(ModAttachments.PLAYER_UNLOCKS.get(), data);
        syncTo(player);
    }

    private static void syncTo(Player player) {
        if (player instanceof ServerPlayer sp) {
            NetworkHandler.sendUnlocksTo(sp, get(sp).getUnlocked());
        }
    }

    private UnlockHelper() {}
}
