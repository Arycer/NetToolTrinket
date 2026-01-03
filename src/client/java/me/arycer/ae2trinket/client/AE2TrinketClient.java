package me.arycer.ae2trinket.client;

import appeng.core.definitions.AEItems;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import me.arycer.ae2trinket.client.render.NetworkToolTrinketRenderer;
import me.arycer.ae2trinket.client.render.PortableCellTrinketRenderer;
import me.arycer.ae2trinket.client.render.TerminallTrinketRenderer;
import me.arycer.ae2trinket.net.c2s.UseActionC2S;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class AE2TrinketClient implements ClientModInitializer {

    private static boolean terminalPressed = false;
    private static boolean cellPressed = false;

    public static final KeyBinding TERMINAL_TRINKET_KEYBINDING = new KeyBinding(
            "key.ae2trinket.terminal_trinket",
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.inventory"
    );

    public static final KeyBinding CELL_TRINKET_KEYBINDING = new KeyBinding(
            "key.ae2trinket.cell_trinket",
            GLFW.GLFW_KEY_COMMA,
            "key.categories.inventory"
    );

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerRenderers());

        KeyBindingHelper.registerKeyBinding(TERMINAL_TRINKET_KEYBINDING);
        KeyBindingHelper.registerKeyBinding(CELL_TRINKET_KEYBINDING);

        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            // Terminal keybind
            boolean isPressed = TERMINAL_TRINKET_KEYBINDING.isPressed();

            if (isPressed && !terminalPressed && minecraftClient.currentScreen == null) {
                ClientPlayNetworking.send(new UseActionC2S(UseActionC2S.UseAction.OPEN_TERMINAL));
            }

            terminalPressed = isPressed;

            // Portable cell keybind
            boolean cellIsPressed = CELL_TRINKET_KEYBINDING.isPressed();

            if (cellIsPressed && !cellPressed && minecraftClient.currentScreen == null) {
                ClientPlayNetworking.send(new UseActionC2S(UseActionC2S.UseAction.OPEN_PORTABLE_CELL));
            }

            cellPressed = cellIsPressed;
        });
    }

    private void registerRenderers() {
        TrinketRendererRegistry.registerRenderer(AEItems.NETWORK_TOOL.asItem(), NetworkToolTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.WIRELESS_TERMINAL.asItem(), TerminallTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.WIRELESS_CRAFTING_TERMINAL.asItem(), TerminallTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.PORTABLE_ITEM_CELL1K.asItem(), PortableCellTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.PORTABLE_ITEM_CELL4K.asItem(), PortableCellTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.PORTABLE_ITEM_CELL16K.asItem(), PortableCellTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.PORTABLE_ITEM_CELL64K.asItem(), PortableCellTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.PORTABLE_ITEM_CELL256K.asItem(), PortableCellTrinketRenderer.INSTANCE);
    }
}