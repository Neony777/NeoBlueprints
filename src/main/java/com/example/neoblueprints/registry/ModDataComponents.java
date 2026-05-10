package com.example.neoblueprints.registry;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class ModDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(NeoBlueprintsMod.MODID);

    public static final Supplier<DataComponentType<ResourceLocation>> BLUEPRINT_ID =
            register("blueprint_id", builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(ResourceLocation.CODEC)));

    private static <T> Supplier<DataComponentType<T>> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builder
    ) {
        return COMPONENTS.registerComponentType(name, builder);
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModDataComponents() {}
}
