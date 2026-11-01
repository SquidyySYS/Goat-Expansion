package net.sqdy.goat_expansion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.sqdy.goat_expansion.GoatExpansionClient;

public class GoatShieldLayer extends RenderLayer<GoatRenderState, GoatModel> {

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState leftShieldRenderState = new ItemStackRenderState();
    private final ItemStackRenderState rightShieldRenderState = new ItemStackRenderState();

    public GoatShieldLayer(RenderLayerParent<GoatRenderState, GoatModel> renderer, ItemModelResolver itemModelResolver) {
        super(renderer);
        this.itemModelResolver = itemModelResolver;
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            GoatRenderState renderState,
            float yRot,
            float xRot
    ) {
        ItemStack leftShield = GoatExpansionClient.getLeftShield(renderState);
        ItemStack rightShield = GoatExpansionClient.getRightShield(renderState);

        if (leftShield.isEmpty() && rightShield.isEmpty()) {
            return;
        }

        ModelPart bodyPart = this.getParentModel().root().getChild("body");

        if (!leftShield.isEmpty()) {
            submitFlankShield(
                    poseStack,
                    submitNodeCollector,
                    packedLight,
                    renderState,
                    bodyPart,
                    leftShield,
                    this.leftShieldRenderState,
                    true
            );
        }

        if (!rightShield.isEmpty()) {
            submitFlankShield(
                    poseStack,
                    submitNodeCollector,
                    packedLight,
                    renderState,
                    bodyPart,
                    rightShield,
                    this.rightShieldRenderState,
                    false
            );
        }
    }

    private void submitFlankShield(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            GoatRenderState goatState,
            ModelPart bodyPart,
            ItemStack shieldStack,
            ItemStackRenderState shieldRenderState,
            boolean isLeftFlank
    ) {
        Minecraft mc = Minecraft.getInstance();
        this.itemModelResolver.updateForTopItem(
                shieldRenderState,
                shieldStack,
                ItemDisplayContext.FIXED,
                mc.level,
                null,
                0
        );

        if (shieldRenderState.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        bodyPart.translateAndRotate(poseStack);

        // Прижаты вплотную к телу козы
        float xOffset = isLeftFlank ? 0.315F : -0.255F;
        float yOffset = -0.78F;
        float zOffset = 0.05F;
        poseStack.translate(xOffset, yOffset, zOffset);

        poseStack.mulPose(Axis.YP.rotationDegrees(isLeftFlank ? -90.0F : 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

        // Размер щита закрывает весь бок козы
        poseStack.scale(1.45F, 1.45F, 1.45F);

        shieldRenderState.submit(
                poseStack,
                submitNodeCollector,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                goatState.outlineColor
        );

        poseStack.popPose();
    }
}