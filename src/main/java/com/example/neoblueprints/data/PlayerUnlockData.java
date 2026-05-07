package com.example.neoblueprints.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-player set of unlocked recipe IDs. Stored as an attachment on the player.
 * Persists across deaths (copyOnDeath) and server restarts (serialized via Codec).
 */
public final class PlayerUnlockData {

    public static final Codec<PlayerUnlockData> CODEC = ResourceLocation.CODEC
            .listOf()
            .xmap(
                    list -> new PlayerUnlockData(new HashSet<>(list)),
                    data -> new ArrayList<>(data.unlocked)
            );

    private final Set<ResourceLocation> unlocked;

    public PlayerUnlockData() {
        this(new HashSet<>());
    }

    public PlayerUnlockData(Set<ResourceLocation> unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isUnlocked(ResourceLocation recipeId) {
        return unlocked.contains(recipeId);
    }

    /** Returns true if the recipe was newly added, false if already present. */
    public boolean unlock(ResourceLocation recipeId) {
        return unlocked.add(recipeId);
    }

    public void reset() {
        unlocked.clear();
    }

    public Set<ResourceLocation> getUnlocked() {
        return Collections.unmodifiableSet(unlocked);
    }

    public int size() {
        return unlocked.size();
    }
}
