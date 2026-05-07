package com.example.neoblueprints.registry;

import com.example.neoblueprints.NeoBlueprintsMod;
import com.example.neoblueprints.config.BlueprintConfig;
import com.example.neoblueprints.config.BlueprintDefinition;
import com.example.neoblueprints.item.BlueprintItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;
import java.util.List;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NeoBlueprintsMod.MODID);

    /** Dedicated tab containing the empty blueprint plus every configured blueprint, sorted by rarity then name. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLUEPRINT_TAB =
            CREATIVE_MODE_TABS.register("tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.neoblueprints.tab"))
                    .icon(() -> new ItemStack(ModItems.BLUEPRINT.get()))
                    .displayItems((parameters, output) -> {
                        // Empty blueprint first.
                        output.accept(new ItemStack(ModItems.BLUEPRINT.get()));

                        List<BlueprintDefinition> defs = BlueprintConfig.all();
                        defs.sort(Comparator
                                .comparingInt((BlueprintDefinition d) -> d.rarity().tier())
                                .thenComparing(BlueprintDefinition::name, String.CASE_INSENSITIVE_ORDER));

                        for (BlueprintDefinition def : defs) {
                            output.accept(BlueprintItem.create(def.id()));
                        }
                    })
                    .build());

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
