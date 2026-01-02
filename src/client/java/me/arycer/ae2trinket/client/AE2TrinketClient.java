package me.arycer.ae2trinket.client;

import appeng.core.definitions.AEItems;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import me.arycer.ae2trinket.client.render.NetworkToolTrinketRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class AE2TrinketClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerRenderers());
    }

    private void registerRenderers() {
        TrinketRendererRegistry.registerRenderer(AEItems.NETWORK_TOOL.asItem(), NetworkToolTrinketRenderer.INSTANCE);
    }
}
