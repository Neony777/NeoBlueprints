package com.example.neoblueprints.config;

import com.example.neoblueprints.item.BlueprintRarity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * In-memory representation of one blueprint entry from the JSON config.
 *
 * @param id        unique blueprint id (e.g. {@code neoblueprints:iron_tools})
 * @param name      human-readable display name
 * @param rarity    rarity tier
 * @param recipes   recipe IDs unlocked by consuming this blueprint
 */
public record BlueprintDefinition(
        ResourceLocation id,
        String name,
        BlueprintRarity rarity,
        List<ResourceLocation> recipes
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintDefinition> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, BlueprintDefinition::id,
            ByteBufCodecs.STRING_UTF8, BlueprintDefinition::name,
            ByteBufCodecs.idMapper(i -> BlueprintRarity.values()[i], BlueprintRarity::tier), BlueprintDefinition::rarity,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), BlueprintDefinition::recipes,
            BlueprintDefinition::new
    );
}
