package com.example.neoblueprints.registry;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.example.neoblueprints.item.BlueprintItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NeoBlueprintsMod.MODID);

    public static final DeferredItem<Item> BLUEPRINT = ITEMS.register(
            "blueprint",
            () -> new BlueprintItem(new Item.Properties().stacksTo(16))
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
