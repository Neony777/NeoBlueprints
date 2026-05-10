package com.example.neoblueprints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/** Registers client-only event handlers for the locked-recipe UI overlay. */
public final class ClientEvents {

    private ClientEvents() {}

    /** Guards against the recursive tooltip loop: our redraw fires another Pre event. */
    private static boolean inTooltipRedraw = false;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onContainerRenderForeground);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onTooltipPre);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientLoggingOut);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientUnlockCache.clear();
    }

    private static void onContainerRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        LockedRecipeOverlay.renderForScreen(event.getGuiGraphics(), screen);
    }

    /**
     * When hovering the result slot of a locked recipe, cancel the normal
     * tooltip and render our own "Locked Recipe — requires blueprint" message.
     *
     * We cancel-and-redraw rather than mutating the existing component list
     * because {@code RenderTooltipEvent.Pre#getComponents()} returns an
     * unmodifiable list; calling clear() on it crashes the game.
     */
    private static void onTooltipPre(RenderTooltipEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> cs)) return;

        var hovered = cs.hoveredSlot;
        if (hovered == null) return;

        List<Component> replacement = LockedRecipeOverlay.tooltipForLockedSlot(cs, hovered);
        if (replacement == null) return;

        // Cancel the original tooltip and draw ours at the same screen position.
        // Guard against recursion: renderComponentTooltip fires another RenderTooltipEvent.Pre.
        if (inTooltipRedraw) return;
        event.setCanceled(true);
        inTooltipRedraw = true;
        try {
            event.getGraphics().renderComponentTooltip(mc.font, replacement, event.getX(), event.getY());
        } finally {
            inTooltipRedraw = false;
        }
    }
}
