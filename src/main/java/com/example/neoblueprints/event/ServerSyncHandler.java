package com.example.neoblueprints.event;

import com.example.neoblueprints.config.BlueprintConfig;
import com.example.neoblueprints.data.UnlockHelper;
import com.example.neoblueprints.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Server-side: load JSON config on startup and (re)sync to clients on every
 * lifecycle event where the client may have lost its mirror — login, dimension
 * change, and respawn.
 */
public final class ServerSyncHandler {

    private ServerSyncHandler() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        BlueprintConfig.load();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncAll(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncAll(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncAll(event.getEntity());
    }

    private static void syncAll(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        NetworkHandler.sendBlueprintsTo(sp);
        NetworkHandler.sendUnlocksTo(sp, UnlockHelper.get(sp).getUnlocked());
    }
}
