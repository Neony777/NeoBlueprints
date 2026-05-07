package com.example.neoblueprints;

import com.example.neoblueprints.client.ClientEvents;
import com.example.neoblueprints.command.BlueprintCommand;
import com.example.neoblueprints.event.CraftingLockHandler;
import com.example.neoblueprints.event.ServerSyncHandler;
import com.example.neoblueprints.network.NetworkHandler;
import com.example.neoblueprints.registry.ModAttachments;
import com.example.neoblueprints.registry.ModConditions;
import com.example.neoblueprints.registry.ModCreativeTabs;
import com.example.neoblueprints.registry.ModDataComponents;
import com.example.neoblueprints.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(NeoBlueprintsMod.MODID)
public class NeoBlueprintsMod {

    public static final String MODID = "neoblueprints";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeoBlueprintsMod(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModConditions.register(modEventBus);
        NetworkHandler.register(modEventBus);

        NeoForge.EVENT_BUS.register(CraftingLockHandler.class);
        NeoForge.EVENT_BUS.register(BlueprintCommand.class);
        NeoForge.EVENT_BUS.register(ServerSyncHandler.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.register();
        }

        LOGGER.info("NeoBlueprints initialized");
    }
}
