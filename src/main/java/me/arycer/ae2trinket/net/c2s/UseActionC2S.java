package me.arycer.ae2trinket.net.c2s;

import me.arycer.ae2trinket.AE2Trinket;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

public record UseActionC2S(UseAction action) implements FabricPacket {

    public static final PacketType<UseActionC2S> TYPE = PacketType.create(AE2Trinket.id("use_terminal_c2s"), UseActionC2S::new);

    public UseActionC2S(PacketByteBuf buf) {
        this(buf.readEnumConstant(UseAction.class)); // No data to read
    }

    @Override
    public void write(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeEnumConstant(action);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public enum UseAction {
        OPEN_TERMINAL,
        OPEN_PORTABLE_CELL
    }
}
