package com.example.neoblueprints.network;

import com.example.neoblueprints.NeoBlueprintsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/** S2C: the local player's full unlocked-recipe set. Sent on login and on each unlock change. */
public record SyncUnlocksPayload(Set<ResourceLocation> unlocked) implements CustomPacketPayload {

    public static final Type<SyncUnlocksPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NeoBlueprintsMod.MODID, "sync_unlocks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUnlocksPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
            SyncUnlocksPayload::unlocked,
            SyncUnlocksPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
