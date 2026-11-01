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
import net.sqdy.goat_expansion.item.ModItems;

public class GoatPouchLayer extends RenderLayer<GoatRenderState, GoatModel> {

    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState pouchRenderState = new ItemStackRenderState();
    private ItemStack cachedPouchStack;

    public GoatPouchLayer(RenderLayerParent<GoatRenderState, GoatModel> renderer, ItemModelResolver itemModelResolver) {
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
        int pouchMask = GoatExpansionClient.getPouchMask(renderState);
        if (pouchMask == 0) {
            return;
        }

        if (this.cachedPouchStack == null) {
            this.cachedPouchStack = new ItemStack(ModItems.GOAT_POUCH);
        }

        Minecraft mc = Minecraft.getInstance();
        this.itemModelResolver.updateForTopItem(
                this.pouchRenderState,
                this.cachedPouchStack,
                ItemDisplayContext.FIXED,
                mc.level,
                null,
                0
        );

        if (this.pouchRenderState.isEmpty()) {
            return;
        }

        ModelPart bodyPart = this.getParentModel().root().getChild("body");

        float[] zPositions = new float[]{-0.18F, 0.04F, 0.26F};

        for (int slot = 1; slot <= 6; slot++) {
            if ((pouchMask & (1 << (slot - 1))) != 0) {
                boolean isLeft = slot <= 3;
                int posIdx = (slot - 1) % 3;
                renderSingle3DPouch(
                        poseStack,
                        submitNodeCollector,
                        packedLight,
                        renderState,
                        bodyPart,
                        isLeft,
                        zPositions[posIdx]
                );
            }
        }
    }

    private void renderSingle3DPouch(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            GoatRenderState goatState,
            ModelPart bodyPart,
            boolean isLeft,
            float zOffset
    ) {
        poseStack.pushPose();
        bodyPart.translateAndRotate(poseStack);

        float xOffset = isLeft ? 0.31F : -0.25F;
        float yOffset = -0.78F;

        poseStack.translate(xOffset, yOffset, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(isLeft ? -90.0F : 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(8.0F));

        poseStack.scale(0.38F, 0.38F, 0.85F);

        this.pouchRenderState.submit(
                poseStack,
                submitNodeCollector,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                goatState.outlineColor
        );

        poseStack.popPose();
    }
}