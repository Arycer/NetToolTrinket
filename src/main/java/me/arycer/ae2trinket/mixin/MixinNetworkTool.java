package me.arycer.ae2trinket.mixin;

import appeng.core.definitions.AEItems;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.Optional;

@Mixin(NetworkToolItem.class)
public class MixinNetworkTool {

    @WrapMethod(method = "findNetworkToolInv")
    private static NetworkToolMenuHost customTrinketMenuHost(PlayerEntity player, Operation<NetworkToolMenuHost> original) {
        Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(player);
        if (trinketComponent.isPresent() && trinketComponent.get().isEquipped(AEItems.NETWORK_TOOL.asItem())) {
            List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.get().getEquipped(AEItems.NETWORK_TOOL.asItem());
            ItemStack networkToolStack = equipped.get(0).getRight();
            return new NetworkToolMenuHost(player, null, networkToolStack, null);
        }

        return original.call(player);
    }
}
