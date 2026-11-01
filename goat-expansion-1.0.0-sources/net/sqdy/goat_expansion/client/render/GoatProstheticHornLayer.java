package net.sqdy.goat_expansion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.resources.Identifier;
import net.sqdy.goat_expansion.GoatExpansion;
import net.sqdy.goat_expansion.GoatExpansionClient;
import net.sqdy.goat_expansion.client.model.GoatProstheticHornsModel;

public class GoatProstheticHornLayer extends RenderLayer<GoatRenderState, GoatModel> {

    public static final ModelLayerLocation PROSTHETIC_HORN_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(GoatExpansion.MOD_ID, "prosthetic_horns"),
            "main"
    );

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            GoatExpansion.MOD_ID,
            "textures/entity/goat/goat_prosthetic_horns.png"
    );

    private final GoatProstheticHornsModel model;

    public GoatProstheticHornLayer(RenderLayerParent<GoatRenderState, GoatModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new GoatProstheticHornsModel(modelSet.bakeLayer(PROSTHETIC_HORN_LAYER));
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
        boolean hasLeft = GoatExpansionClient.hasLeftProsthetic(renderState);
        boolean hasRight = GoatExpansionClient.hasRightProsthetic(renderState);

        if ((hasLeft || hasRight) && !renderState.isInvisible) {
            poseStack.pushPose();

            // Переход в систему координат головы козы
            ModelPart parentHead = this.getParentModel().root().getChild("head");
            parentHead.translateAndRotate(poseStack);

            this.model.setHornVisibility(hasLeft, hasRight);
            renderColoredCutoutModel(
                    this.model,
                    TEXTURE,
                    poseStack,
                    submitNodeCollector,
                    packedLight,
                    renderState,
                    -1,
                    1
            );

            poseStack.popPose();
        }
    }
}