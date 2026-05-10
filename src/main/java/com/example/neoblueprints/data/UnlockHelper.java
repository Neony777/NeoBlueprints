package com.example.neoblueprints.data;

import com.example.neoblueprints.network.NetworkHandler;
import com.example.neoblueprints.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convenience accessors over the player's PlayerUnlockData attachment.
 * Mutations automatically push a sync packet to the player so the client UI
 * (locked-recipe overlay) reflects the new state immediately.
 *
 * <p>Both permanent (attachment-backed) and temporary (in-memory, time-limited)
 * unlocks are considered: {@link #isUnlocked} returns true for either kind.
 */
public final class UnlockHelper {

    public static PlayerUnlockData get(Player player) {
        return player.getData(ModAttachments.PLAYER_UNLOCKS.get());
    }

    /**
     * Returns true if the recipe is unlocked — either permanently (attachment) or
     * temporarily ({@link TemporaryUnlockManager}).
     */
    public static boolean isUnlocked(Player player, ResourceLocation recipeId) {
        if (get(player).isUnlocked(recipeId)) return true;
        return TemporaryUnlockManager.isActive(player.getUUID(), recipeId);
    }

    /** Returns true if the recipe was newly added (false if already permanently unlocked). */
    public static boolean unlock(Player player, ResourceLocation recipeId) {
        PlayerUnlockData data = get(player);
        boolean changed = data.unlock(recipeId);
        if (changed) {
            player.setData(ModAttachments.PLAYER_UNLOCKS.get(), data);
            syncTo(player);
        }
        return changed;
    }

    /**
     * Grants a temporary unlock for all given recipes and syncs the combined set to
     * the client. Does not consume the item and does not touch the persistent attachment.
     */
    public static void grantTemporary(Player player, List<ResourceLocation> recipeIds, long durationMs) {
        TemporaryUnlockManager.grant(player.getUUID(), recipeIds, durationMs);
        syncTo(player);
    }

    public static void reset(Player player) {
        PlayerUnlockData data = get(player);
        data.reset();
        player.setData(ModAttachments.PLAYER_UNLOCKS.get(), data);
        syncTo(player);
    }

    /**
     * Sends the combined permanent + active temporary unlock set to the client.
     */
    public static void syncTo(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        Set<ResourceLocation> permanent = get(sp).getUnlocked();
        Set<ResourceLocation> temp = TemporaryUnlockManager.getActive(sp.getUUID());
        if (temp.isEmpty()) {
            NetworkHandler.sendUnlocksTo(sp, permanent);
        } else {
            Set<ResourceLocation> combined = new HashSet<>(permanent);
            combined.addAll(temp);
            NetworkHandler.sendUnlocksTo(sp, combined);
        }
    }

    private UnlockHelper() {}
}
