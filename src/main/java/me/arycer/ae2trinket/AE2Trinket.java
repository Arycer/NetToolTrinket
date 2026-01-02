package me.arycer.ae2trinket;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.GridHelper;
import appeng.block.networking.CableBusBlock;
import appeng.core.definitions.AEItems;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.networktool.NetworkStatusMenu;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class AE2Trinket implements ModInitializer {

    @Override
    public void onInitialize() {
        MenuLocators.register(AE2Trinket.TrinketHostLocator.class, TrinketHostLocator::write, TrinketHostLocator::read);

        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = world.getBlockState(blockPos);
            Block block = blockState.getBlock();
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(playerEntity);

            if (block instanceof CableBusBlock
                    && trinketComponent.isPresent()
                    && trinketComponent.get().isEquipped(AEItems.NETWORK_TOOL.asItem())
                    && !playerEntity.isSneaking()) {
                List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.get().getEquipped(AEItems.NETWORK_TOOL.asItem());
                ItemStack networkToolStack = equipped.get(0).getRight();

                var nodeHost = GridHelper.getNodeHost(world, blockPos);
                var locator = new TrinketHostLocator(networkToolStack, blockPos);

                if (nodeHost != null) {
                    MenuOpener.open(NetworkStatusMenu.NETWORK_TOOL_TYPE, playerEntity, locator);
                }
            }

            return ActionResult.PASS;

        });
    }

    public record TrinketHostLocator(ItemStack stack, BlockPos pos) implements MenuLocator {
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

        public static TrinketHostLocator read(PacketByteBuf packetByteBuf) {
            ItemStack stack = packetByteBuf.readItemStack();
            BlockPos pos = packetByteBuf.readBlockPos();
            return new TrinketHostLocator(stack, pos);
        }
    }
}
