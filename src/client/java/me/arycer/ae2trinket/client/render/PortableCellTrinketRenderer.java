package me.arycer.ae2trinket.client.render;

import appeng.items.tools.powered.WirelessTerminalItem;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

import java.util.Optional;

public class PortableCellTrinketRenderer implements TrinketRenderer {

    public static final PortableCellTrinketRenderer INSTANCE = new PortableCellTrinketRenderer();

    @Override
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {

        if (!(contextModel instanceof BipedEntityModel<?> model)) {
            return;
        }

        matrices.push();

        model.body.rotate(matrices);

        double z = -0.15;
        double y = 0.6;

        // if player has chestplate equipped, move the trinket slightly forward
        if (!entity.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
            z = -0.2;
        }

        Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(entity);
        if (trinketComponent.isPresent() && trinketComponent.get().isEquipped(item -> item.getItem() instanceof WirelessTerminalItem)) {
            y += 0.2;
        }

        matrices.translate(-0.15, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));

        matrices.scale(0.2f, 0.2f, 0.2f);

        MinecraftClient.getInstance().getItemRenderer().renderItem(
                entity,
                stack,
                ModelTransformationMode.FIXED,
                false,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                light,
                0,
                entity.getId()
        );

        matrices.pop();
    }
}