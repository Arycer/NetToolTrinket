package me.arycer.ae2trinket.net.c2s;

import me.arycer.ae2trinket.AE2Trinket;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

public record UseTerminalC2S() implements FabricPacket {

    public static final PacketType<UseTerminalC2S> TYPE = PacketType.create(AE2Trinket.id("use_terminal_c2s"), UseTerminalC2S::new);

    public UseTerminalC2S(PacketByteBuf buf) {
        this(); // No data to read
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        // No data to write
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
