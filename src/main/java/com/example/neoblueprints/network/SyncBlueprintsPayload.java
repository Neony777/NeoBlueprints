package com.example.neoblueprints.network;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.example.neoblueprints.config.BlueprintDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** S2C: full blueprint config snapshot, sent on player login and on /blueprint reload. */
public record SyncBlueprintsPayload(List<BlueprintDefinition> definitions) implements CustomPacketPayload {

    public static final Type<SyncBlueprintsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoBlueprintsMod.MODID, "sync_blueprints"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBlueprintsPayload> STREAM_CODEC = StreamCodec.composite(
            BlueprintDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncBlueprintsPayload::definitions,
            SyncBlueprintsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
