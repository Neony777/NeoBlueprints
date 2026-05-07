package com.example.neoblueprints.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Client-side mirror of the local player's unlocked-recipe set, populated by SyncUnlocksPayload. */
public final class ClientUnlockCache {

    private static volatile Set<ResourceLocation> unlocked = Collections.emptySet();

    private ClientUnlockCache() {}

    public static void set(Set<ResourceLocation> ids) {
        unlocked = Collections.unmodifiableSet(new HashSet<>(ids));
    }

    public static boolean isUnlocked(ResourceLocation recipeId) {
        return unlocked.contains(recipeId);
    }

    public static void clear() {
        unlocked = Collections.emptySet();
    }
}
