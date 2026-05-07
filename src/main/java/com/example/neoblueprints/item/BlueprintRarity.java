package com.example.neoblueprints.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Rarity tier for a blueprint. Each tier has a distinct chat color used when
 * rendering the blueprint item's name (mirroring vanilla item rarity coloring).
 *
 * <p>The default fallback when an unknown rarity string is configured is
 * {@link #COMMON}.
 */
public enum BlueprintRarity {

    COMMON("common", ChatFormatting.WHITE, 0),
    UNCOMMON("uncommon", ChatFormatting.YELLOW, 1),
    RARE("rare", ChatFormatting.AQUA, 2),
    EPIC("epic", ChatFormatting.LIGHT_PURPLE, 3),
    LEGENDARY("legendary", ChatFormatting.GOLD, 4);

    private final String id;
    private final ChatFormatting color;
    private final int tier;

    BlueprintRarity(String id, ChatFormatting color, int tier) {
        this.id = id;
        this.color = color;
        this.tier = tier;
    }

    public String id() {
        return id;
    }

    public ChatFormatting color() {
        return color;
    }

    public int tier() {
        return tier;
    }

    public Component displayName() {
        return Component.translatable("rarity.neoblueprints." + id).withStyle(color);
    }

    /** Lenient parser; unknown values fall back to {@link #COMMON}. */
    public static BlueprintRarity fromString(String s) {
        if (s == null) return COMMON;
        String norm = s.trim().toLowerCase(Locale.ROOT);
        for (BlueprintRarity r : values()) {
            if (r.id.equals(norm)) return r;
        }
        return COMMON;
    }
}
