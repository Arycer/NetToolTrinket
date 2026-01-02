package me.arycer.ae2trinket.client;

import appeng.core.definitions.AEItems;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import me.arycer.ae2trinket.client.render.NetworkToolTrinketRenderer;
import me.arycer.ae2trinket.client.render.TerminallTrinketRenderer;
import me.arycer.ae2trinket.net.c2s.UseTerminalC2S;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class AE2TrinketClient implements ClientModInitializer {

    private static boolean wasPressed = false;

    public static final KeyBinding TERMINAL_TRINKET_KEYBINDING = new KeyBinding(
            "key.ae2trinket.terminal_trinket",
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.inventory"
    );

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerRenderers());

        KeyBindingHelper.registerKeyBinding(TERMINAL_TRINKET_KEYBINDING);

        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            boolean isPressed = TERMINAL_TRINKET_KEYBINDING.isPressed();

            if (isPressed && !wasPressed && minecraftClient.currentScreen == null) {
                ClientPlayNetworking.send(new UseTerminalC2S());
            }

            wasPressed = isPressed;
        });
    }

    private void registerRenderers() {
        TrinketRendererRegistry.registerRenderer(AEItems.NETWORK_TOOL.asItem(), NetworkToolTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.WIRELESS_TERMINAL.asItem(), TerminallTrinketRenderer.INSTANCE);
        TrinketRendererRegistry.registerRenderer(AEItems.WIRELESS_CRAFTING_TERMINAL.asItem(), TerminallTrinketRenderer.INSTANCE);
    }
}