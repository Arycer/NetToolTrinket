package me.arycer.ae2trinket;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.GridHelper;
import appeng.block.networking.CableBusBlock;
import appeng.core.definitions.AEItems;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.WirelessCraftingTerminalMenuHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.WirelessCraftingTermMenu;
import appeng.menu.me.networktool.NetworkStatusMenu;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import me.arycer.ae2trinket.net.c2s.UseTerminalC2S;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class AE2Trinket implements ModInitializer {

    public static final String MOD_ID = "ae2trinket";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        MenuLocators.register(NetToolTrinketHostLocator.class, NetToolTrinketHostLocator::write, NetToolTrinketHostLocator::read);
        MenuLocators.register(TerminalTrinketHostLocator.class, TerminalTrinketHostLocator::write, TerminalTrinketHostLocator::read);

        ServerPlayNetworking.registerGlobalReceiver(UseTerminalC2S.TYPE, (payload, player, packetSender) -> {
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(player);
            if (trinketComponent.isPresent()) {
                WirelessTerminalItem item;

                if (trinketComponent.get().isEquipped(AEItems.WIRELESS_TERMINAL.asItem())) {
                    item = AEItems.WIRELESS_TERMINAL.asItem();
                } else if (trinketComponent.get().isEquipped(AEItems.WIRELESS_CRAFTING_TERMINAL.asItem())) {
                    item = AEItems.WIRELESS_CRAFTING_TERMINAL.asItem();
                } else {
                    return;
                }

                List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.get().getEquipped(item);
                ItemStack terminalStack = equipped.get(0).getRight();

                if (!checkTerminalPreconditions(item, terminalStack, player)) {
                    return;
                }

                var locator = new TerminalTrinketHostLocator();
                MenuOpener.open(item == AEItems.WIRELESS_TERMINAL.asItem() ? MEStorageMenu.WIRELESS_TYPE : WirelessCraftingTermMenu.TYPE, player, locator);
            }
        });

        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = world.getBlockState(blockPos);
            Block block = blockState.getBlock();
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(playerEntity);

            if (block instanceof CableBusBlock
                    && validateNetworkToolUse(world, blockPos)
                    && trinketComponent.isPresent()
                    && trinketComponent.get().isEquipped(AEItems.NETWORK_TOOL.asItem())
                    && !playerEntity.isSneaking()) {
                List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.get().getEquipped(AEItems.NETWORK_TOOL.asItem());
                ItemStack networkToolStack = equipped.get(0).getRight();

                var nodeHost = GridHelper.getNodeHost(world, blockPos);
                var locator = new NetToolTrinketHostLocator(networkToolStack, blockPos);

                if (nodeHost != null) {
                    MenuOpener.open(NetworkStatusMenu.NETWORK_TOOL_TYPE, playerEntity, locator);
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;

        });
    }

    public record TerminalTrinketHostLocator() implements MenuLocator {

        @Override
        public @Nullable <T> T locate(PlayerEntity playerEntity, Class<T> hostInterface) {
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(playerEntity);
            if (trinketComponent.isEmpty()) {
                return null;
            }

            WirelessTerminalItem item;
            ScreenHandlerType<?> screenType;

            if (trinketComponent.get().isEquipped(AEItems.WIRELESS_TERMINAL.asItem())) {
                item = AEItems.WIRELESS_TERMINAL.asItem();
                screenType = MEStorageMenu.WIRELESS_TYPE;
            } else if (trinketComponent.get().isEquipped(AEItems.WIRELESS_CRAFTING_TERMINAL.asItem())) {
                item = AEItems.WIRELESS_CRAFTING_TERMINAL.asItem();
                screenType = WirelessCraftingTermMenu.TYPE;
            } else {
                return null;
            }

            ItemStack it = trinketComponent.get().getEquipped(item).get(0).getRight();
            ItemMenuHost menuHost;

            if (item == AEItems.WIRELESS_TERMINAL.asItem()) {
                menuHost = new WirelessTerminalMenuHost(playerEntity, null, it, (player, iSubMenu) -> MenuOpener.open(screenType, player, TerminalTrinketHostLocator.this));
            } else {
                menuHost = new WirelessCraftingTerminalMenuHost(playerEntity, null, it, (player, iSubMenu) -> MenuOpener.open(screenType, player, TerminalTrinketHostLocator.this));
            }

            if (hostInterface.isInstance(menuHost)) {
                return hostInterface.cast(menuHost);
            }

            return null;
        }

        public void write(PacketByteBuf byteBuf) {

        }

        public static TerminalTrinketHostLocator read(PacketByteBuf byteBuf) {
            return new TerminalTrinketHostLocator();
        }
    }

    public record NetToolTrinketHostLocator(ItemStack stack, BlockPos pos) implements MenuLocator {
        @Override
        public @Nullable <T> T locate(PlayerEntity playerEntity, Class<T> hostInterface) {
            var nodeHost = GridHelper.getNodeHost(playerEntity.getWorld(), this.pos);

            if (nodeHost == null) {
                return null;
            }

            ItemMenuHost itemMenuHost = new NetworkToolMenuHost(playerEntity, null, stack, nodeHost);
            if (hostInterface.isInstance(itemMenuHost)) {
                return hostInterface.cast(itemMenuHost);
            }

            return null;
        }

        public void write(PacketByteBuf packetByteBuf) {
            packetByteBuf.writeItemStack(this.stack);
            packetByteBuf.writeBlockPos(this.pos);
        }

        public static NetToolTrinketHostLocator read(PacketByteBuf packetByteBuf) {
            ItemStack stack = packetByteBuf.readItemStack();
            BlockPos pos = packetByteBuf.readBlockPos();
            return new NetToolTrinketHostLocator(stack, pos);
        }
    }

    private static boolean checkTerminalPreconditions(WirelessTerminalItem item, ItemStack stack, PlayerEntity player) {
        if (!stack.isEmpty() && stack.getItem() == item) {
            if (item.getLinkedGrid(stack, player.getWorld(), player) == null) {
                return false;
            } else if (!item.hasPower(player, 0.5F, stack)) {
                player.sendMessage(PlayerMessages.DeviceNotPowered.text(), true);
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean validateNetworkToolUse(World world, BlockPos pos) {
        // get cable bus at position and check that it not interacting with something like a storage bus or similar
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        if (!(block instanceof CableBusBlock cableBusBlock)) {
            return false;
        }

        System.out.printf("Cable bus class: %s%n", cableBusBlock.getClass().getName());

        return true;
    }

}
