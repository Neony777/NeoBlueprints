package com.example.neoblueprints.network;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.example.neoblueprints.client.ClientUnlockCache;
import com.example.neoblueprints.config.BlueprintConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkHandler {

    private NetworkHandler() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NetworkHandler::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();

        registrar.playToClient(
                SyncBlueprintsPayload.TYPE,
                SyncBlueprintsPayload.STREAM_CODEC,
                NetworkHandler::handleSyncBlueprints
        );
        registrar.playToClient(
                SyncUnlocksPayload.TYPE,
                SyncUnlocksPayload.STREAM_CODEC,
                NetworkHandler::handleSyncUnlocks
        );
    }

    private static void handleSyncBlueprints(SyncBlueprintsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BlueprintConfig.replaceClientCache(
                payload.definitions(), payload.singleUse(), payload.tempUnlockSeconds()));
    }

    private static void handleSyncUnlocks(SyncUnlocksPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientUnlockCache.set(payload.unlocked()));
    }

    public static void sendBlueprintsTo(ServerPlayer player) {
        try {
            PacketDistributor.sendToPlayer(player, new SyncBlueprintsPayload(
                    BlueprintConfig.all(), BlueprintConfig.isSingleUse(), BlueprintConfig.getTempUnlockSeconds()));
        } catch (Exception e) {
            NeoBlueprintsMod.LOGGER.warn("Failed to send blueprint sync to {}: {}", player.getName().getString(), e.toString());
        }
    }

    public static void sendUnlocksTo(ServerPlayer player, java.util.Set<net.minecraft.resources.ResourceLocation> unlocked) {
        try {
            PacketDistributor.sendToPlayer(player, new SyncUnlocksPayload(unlocked));
        } catch (Exception e) {
            NeoBlueprintsMod.LOGGER.warn("Failed to send unlock sync to {}: {}", player.getName().getString(), e.toString());
        }
    }
}
