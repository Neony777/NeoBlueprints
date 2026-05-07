package com.example.neoblueprints.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Datapack-attachable condition declaring that a recipe is gated behind a
 * specific blueprint. At <em>load time</em> the condition always passes so the
 * recipe is registered and remains available to be unlocked at runtime; the
 * actual per-player gating is enforced by
 * {@link com.example.neoblueprints.inventory.LockedResultSlot} at
 * {@code mayPickup} time.
 *
 * <p>Recipes that include this condition in their JSON document explicitly which
 * blueprint a player needs, which is useful for modpack authors who want the
 * lock relationship to live next to the recipe rather than only in
 * {@code blueprint-crafting-server.toml}.
 *
 * <p>Example recipe JSON snippet:
 * <pre>
 * {
 *   "neoforge:conditions": [
 *     { "type": "neoblueprints:blueprint_unlocked", "blueprint": "neoblueprints:blueprint_iron_tools" }
 *   ],
 *   "type": "minecraft:crafting_shaped",
 *   ...
 * }
 * </pre>
 */
public record BlueprintUnlockedCondition(ResourceLocation blueprint) implements ICondition {

    public static final MapCodec<BlueprintUnlockedCondition> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("blueprint")
                            .forGetter(BlueprintUnlockedCondition::blueprint)
            ).apply(instance, BlueprintUnlockedCondition::new)
    );

    @Override
    public boolean test(IContext context) {
        // Always pass at load time. Per-player runtime enforcement lives in
        // LockedResultSlot.mayPickup. Removing the recipe at load would deny it
        // for all players forever, defeating the unlock system.
        return true;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
