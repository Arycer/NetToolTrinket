package me.arycer.ae2trinket.mixin;

import appeng.core.definitions.AEItems;
import appeng.menu.ToolboxMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

@Mixin(ToolboxMenu.class)
public class MixinToolboxMenu {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;getStack(I)Lnet/minecraft/item/ItemStack;"
            )
    )
    private ItemStack onTickOperation(PlayerInventory instance, int slot, Operation<ItemStack> original) {
        PlayerEntity player = instance.player;
        Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(player);

        if (trinketComponent.isPresent() && trinketComponent.get().isEquipped(AEItems.NETWORK_TOOL.asItem())) {
            List<Pair<SlotReference, ItemStack>> equipped = trinketComponent.get().getEquipped(AEItems.NETWORK_TOOL.asItem());
            return equipped.get(0).getRight();
        }

        return original.call(instance, slot);
    }
}
