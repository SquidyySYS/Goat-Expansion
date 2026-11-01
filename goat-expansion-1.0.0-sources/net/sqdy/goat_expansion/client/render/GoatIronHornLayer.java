package net.sqdy.goat_expansion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.resources.Identifier;
import net.sqdy.goat_expansion.GoatExpansion;
import net.sqdy.goat_expansion.GoatExpansionClient;

public class GoatIronHornLayer extends RenderLayer<GoatRenderState, GoatModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            GoatExpansion.MOD_ID,
            "textures/entity/goat/goat_iron_horns.png"
    );

    public GoatIronHornLayer(RenderLayerParent<GoatRenderState, GoatModel> renderer) {
        super(renderer);
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
        if (GoatExpansionClient.hasIronCaps(renderState) && !renderState.isInvisible) {
            renderColoredCutoutModel(
                    this.getParentModel(),
                    TEXTURE,
                    poseStack,
                    submitNodeCollector,
                    packedLight,
                    renderState,
                    -1, // Белый цвет (0xFFFFFFFF) без тонирования
                    1   // Порядок отрисовки поверх базового скина (order = 1)
            );
        }
    }
}