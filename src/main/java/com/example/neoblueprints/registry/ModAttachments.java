package com.example.neoblueprints.registry;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.example.neoblueprints.data.PlayerUnlockData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NeoBlueprintsMod.MODID);

    public static final Supplier<AttachmentType<PlayerUnlockData>> PLAYER_UNLOCKS =
            ATTACHMENT_TYPES.register("player_unlocks", () ->
                    AttachmentType.builder((Supplier<PlayerUnlockData>) PlayerUnlockData::new)
                            .serialize(PlayerUnlockData.CODEC)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }

    private ModAttachments() {}
}
