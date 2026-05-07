package com.example.neoblueprints.registry;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.example.neoblueprints.condition.BlueprintUnlockedCondition;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, NeoBlueprintsMod.MODID);

    public static final Supplier<MapCodec<BlueprintUnlockedCondition>> BLUEPRINT_UNLOCKED =
            CONDITION_CODECS.register("blueprint_unlocked", () -> BlueprintUnlockedCondition.CODEC);

    public static void register(IEventBus bus) {
        CONDITION_CODECS.register(bus);
    }

    private ModConditions() {}
}
