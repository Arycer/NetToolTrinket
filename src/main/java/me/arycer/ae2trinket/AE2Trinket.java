package me.arycer.ae2trinket;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.GridHelper;
import appeng.api.parts.IPartHost;
import appeng.core.definitions.AEItems;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.WirelessCraftingTerminalMenuHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.contents.PortableCellMenuHost;
import appeng.items.parts.PartItem;
import appeng.items.tools.powered.PortableCellItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.WirelessCraftingTermMenu;
import appeng.menu.me.networktool.NetworkStatusMenu;
import appeng.parts.automation.UpgradeablePart;
import appeng.parts.networking.CablePart;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import me.arycer.ae2trinket.net.c2s.UseActionC2S;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class AE2Trinket implements ModInitializer {

    public static final String MOD_ID = "ae2trinket";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
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

    private static boolean checkNoParts(IPartHost host, Direction direction) {
        if (host.getPart(direction) instanceof UpgradeablePart) {
            return false;
        }

        return host.getPart(direction) == null;
    }

    @Override
    public void onInitialize() {
        MenuLocators.register(NetToolTrinketHostLocator.class, NetToolTrinketHostLocator::write, NetToolTrinketHostLocator::read);
        MenuLocators.register(TerminalTrinketHostLocator.class, TerminalTrinketHostLocator::write, TerminalTrinketHostLocator::read);
        MenuLocators.register(CellHostLocator.class, CellHostLocator::write, CellHostLocator::read);

        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
            if (playerEntity.isHolding(AEItems.COLOR_APPLICATOR.asItem())
                    || playerEntity.isHolding(AEItems.CERTUS_QUARTZ_WRENCH.asItem())
                    || playerEntity.isHolding(AEItems.NETHER_QUARTZ_WRENCH.asItem())
                    || playerEntity.isHolding(AEItems.MEMORY_CARD.asItem())) {
                return ActionResult.PASS;
            }

            Predicate<ItemStack> predicate = stack -> {
                if (!(stack.getItem() instanceof PartItem<?> partItem)) {
                    return false;
                }

                Class<?> partClass = partItem.getPartClass();
                return CablePart.class.isAssignableFrom(partClass) || UpgradeablePart.class.isAssignableFrom(partClass);
            };

            if (playerEntity.isHolding(predicate)) {
                return ActionResult.PASS;
            }

            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(playerEntity);

            Vec3d pos = blockHitResult.getPos();
            Vec3d center = Vec3d.ofCenter(blockPos);
            Direction hitDirection = Direction.getFacing(pos.x - center.x, pos.y - center.y, pos.z - center.z);

            if (blockEntity instanceof IPartHost host
                    && checkNoParts(host, hitDirection)
                    && trinketComponent.isPresent()
                    && trinketComponent.get().isEquipped(AEItems.NETWORK_TOOL.asItem())
                    && !playerEntity.isSneaking()) {
                List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.get().getEquipped(AEItems.NETWORK_TOOL.asItem());
                ItemStack networkToolStack = equipped.get(0).getRight();

                var nodeHost = GridHelper.getNodeHost(world, blockPos);
                var locator = new NetToolTrinketHostLocator(networkToolStack, blockPos);

                if (nodeHost != null) {
                    if (!world.isClient) {
                        MenuOpener.open(NetworkStatusMenu.NETWORK_TOOL_TYPE, playerEntity, locator);
                    }

                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });

        ServerPlayNetworking.registerGlobalReceiver(UseActionC2S.TYPE, (payload, player, packetSender) -> {
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(player);
            if (trinketComponent.isPresent()) {
                if (payload.action() == UseActionC2S.UseAction.OPEN_TERMINAL) {
                    handleOpenTerminal(player, trinketComponent.get());
                } else if (payload.action() == UseActionC2S.UseAction.OPEN_PORTABLE_CELL) {
                    handleOpenPortableCell(player, trinketComponent.get());
                }
            }
        });
    }

    private static void handleOpenPortableCell(ServerPlayerEntity player, TrinketComponent trinketComponent) {
        List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.getEquipped(stack -> stack.getItem() instanceof PortableCellItem);
        if (equipped.isEmpty()) {
            return;
        }

        if (!checkCellPreconditions(equipped.get(0).getRight(), player)) {
            return;
        }

        MenuOpener.open(MEStorageMenu.PORTABLE_ITEM_CELL_TYPE, player, new CellHostLocator());
    }

    private static boolean checkCellPreconditions(ItemStack stack, ServerPlayerEntity player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PortableCellItem item)) {
            return false;
        }

        if (item.getAECurrentPower(stack) <= 0) {
            player.sendMessage(PlayerMessages.DeviceNotPowered.text(), true);
            return false;
        } else {
            return true;
        }
    }

    private static void handleOpenTerminal(ServerPlayerEntity player, TrinketComponent trinketComponent) {
        WirelessTerminalItem item;

        if (trinketComponent.isEquipped(AEItems.WIRELESS_TERMINAL.asItem())) {
            item = AEItems.WIRELESS_TERMINAL.asItem();
        } else if (trinketComponent.isEquipped(AEItems.WIRELESS_CRAFTING_TERMINAL.asItem())) {
            item = AEItems.WIRELESS_CRAFTING_TERMINAL.asItem();
        } else {
            return;
        }

        List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.getEquipped(item);
        ItemStack terminalStack = equipped.get(0).getRight();

        if (!checkTerminalPreconditions(item, terminalStack, player)) {
            return;
        }

        var locator = new TerminalTrinketHostLocator();
        MenuOpener.open(item == AEItems.WIRELESS_TERMINAL.asItem() ? MEStorageMenu.WIRELESS_TYPE : WirelessCraftingTermMenu.TYPE, player, locator);
    }

    public record CellHostLocator() implements MenuLocator {

        @Override
        public @Nullable <T> T locate(PlayerEntity playerEntity, Class<T> aClass) {
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(playerEntity);
            if (trinketComponent.isEmpty()) {
                return null;
            }

            PortableCellItem item;

            List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.get().getEquipped(stack -> stack.getItem() instanceof PortableCellItem);
            if (equipped.isEmpty()) {
                return null;
            }

            ItemStack cellStack = equipped.get(0).getRight();
            item = (PortableCellItem) cellStack.getItem();

            ItemMenuHost menuHost = new PortableCellMenuHost(playerEntity, null, item, cellStack, (player, iSubMenu) -> MenuOpener.open(MEStorageMenu.PORTABLE_ITEM_CELL_TYPE, player, CellHostLocator.this));
            if (aClass.isInstance(menuHost)) {
                return aClass.cast(menuHost);
            }

            return null;
        }

        public void write(PacketByteBuf byteBuf) {

        }

        public static CellHostLocator read(PacketByteBuf byteBuf) {
            return new CellHostLocator();
        }
    }

    public record TerminalTrinketHostLocator() implements MenuLocator {

        public static TerminalTrinketHostLocator read(PacketByteBuf byteBuf) {
            return new TerminalTrinketHostLocator();
        }

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
    }

    public record NetToolTrinketHostLocator(ItemStack stack, BlockPos pos) implements MenuLocator {
        public static NetToolTrinketHostLocator read(PacketByteBuf packetByteBuf) {
            ItemStack stack = packetByteBuf.readItemStack();
            BlockPos pos = packetByteBuf.readBlockPos();
            return new NetToolTrinketHostLocator(stack, pos);
        }

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
    }
}
