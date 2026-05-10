package com.example.neoblueprints.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player temporary recipe unlocks used when {@code singleUseBP = false}.
 *
 * <p>Stores expiry times (wall-clock ms) in memory — nothing persists across restarts
 * by design. A server-tick listener checks for expired entries every second and
 * re-syncs the affected player's unlock set so the client overlay updates immediately.
 */
public final class TemporaryUnlockManager {

    private TemporaryUnlockManager() {}

    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<ResourceLocation, Long>> TEMP =
            new ConcurrentHashMap<>();

    private static int tickCounter = 0;
    private static final int TICK_INTERVAL = 20;

    // --------------------------------------------------------------------- API

    /** Grant (or refresh) a temporary unlock for each recipe in {@code recipeIds}. */
    public static void grant(UUID playerId, Collection<ResourceLocation> recipeIds, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        ConcurrentHashMap<ResourceLocation, Long> map =
                TEMP.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        for (ResourceLocation id : recipeIds) {
            map.put(id, expiry);
        }
    }

    /** Returns true if {@code recipeId} has an active (non-expired) temporary unlock. */
    public static boolean isActive(UUID playerId, ResourceLocation recipeId) {
        ConcurrentHashMap<ResourceLocation, Long> map = TEMP.get(playerId);
        if (map == null) return false;
        Long expiry = map.get(recipeId);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    /** Returns all recipe IDs that currently have a non-expired temporary unlock. */
    public static Set<ResourceLocation> getActive(UUID playerId) {
        ConcurrentHashMap<ResourceLocation, Long> map = TEMP.get(playerId);
        if (map == null) return Collections.emptySet();
        long now = System.currentTimeMillis();
        Set<ResourceLocation> active = new HashSet<>();
        for (Map.Entry<ResourceLocation, Long> e : map.entrySet()) {
            if (now < e.getValue()) active.add(e.getKey());
        }
        return active;
    }

    /** Returns true if the player has any active (non-expired) temporary unlock for this recipe. */
    public static boolean hasAnyActive(UUID playerId, Collection<ResourceLocation> recipeIds) {
        ConcurrentHashMap<ResourceLocation, Long> map = TEMP.get(playerId);
        if (map == null) return false;
        long now = System.currentTimeMillis();
        for (ResourceLocation id : recipeIds) {
            Long expiry = map.get(id);
            if (expiry != null && now < expiry) return true;
        }
        return false;
    }

    /** Called on player disconnect to free memory. */
    public static void clearPlayer(UUID playerId) {
        TEMP.remove(playerId);
    }

    // --------------------------------------------------------------------- Tick

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        long now = System.currentTimeMillis();
        MinecraftServer server = event.getServer();

        for (Iterator<Map.Entry<UUID, ConcurrentHashMap<ResourceLocation, Long>>> outer =
             TEMP.entrySet().iterator(); outer.hasNext(); ) {

            Map.Entry<UUID, ConcurrentHashMap<ResourceLocation, Long>> entry = outer.next();
            UUID uuid = entry.getKey();
            ConcurrentHashMap<ResourceLocation, Long> map = entry.getValue();

            boolean anyExpired = map.values().stream().anyMatch(exp -> now >= exp);
            if (!anyExpired) continue;

            map.entrySet().removeIf(e -> now >= e.getValue());

            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) {
                UnlockHelper.syncTo(sp);
            }

            if (map.isEmpty()) outer.remove();
        }
    }
}
