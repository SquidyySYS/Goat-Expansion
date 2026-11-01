package net.sqdy.goat_expansion.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.GoatRenderState;

public class GoatProstheticHornsModel extends EntityModel<GoatRenderState> {

    private final ModelPart straps;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;

    public GoatProstheticHornsModel(ModelPart root) {
        super(root);
        this.straps = root.getChild("straps");
        this.leftHorn = root.getChild("left_horn");
        this.rightHorn = root.getChild("right_horn");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // КОЖАНЫЕ РЕМНИ И НАМОРДНИК (облегают морду поверх козы на 0.05 пикселя)
        partdefinition.addOrReplaceChild(
                "straps",
                CubeListBuilder.create()
                        .texOffs(34, 46)
                        .addBox(-3.0F, -5.0F, -8.0F, 5.0F, 7.0F, 10.0F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, -8.0F, 0.9599F, 0.0F, 0.0F)
        );

        // ЛЕВЫЙ РОГ
        PartDefinition left_horn = partdefinition.addOrReplaceChild(
                "left_horn",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-0.01F, -15.0F, -10.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        PartDefinition bone = left_horn.addOrReplaceChild(
                "bone",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-1.51F, -20.0F, -12.1F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.5F, 5.0F, -12.0F, -0.7418F, 0.0F, 0.0F)
        );

        PartDefinition bone3 = bone.addOrReplaceChild(
                "bone3",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-1.51F, -11.9F, -17.39F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0F, -4.5F, -0.6981F, 0.0F, 0.0F)
        );

        bone3.addOrReplaceChild(
                "bone5",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-1.51F, -24.1F, -7.71F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.1F, -29.39F, -0.9163F, 0.0F, 0.0F)
        );

        // ПРАВЫЙ РОГ
        PartDefinition right_horn = partdefinition.addOrReplaceChild(
                "right_horn",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-2.99F, -15.0F, -10.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        PartDefinition rbone = right_horn.addOrReplaceChild(
                "rbone",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-4.51F, -20.0F, -12.1F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.5F, 5.0F, -12.0F, -0.7418F, 0.0F, 0.0F)
        );

        PartDefinition rbone3 = rbone.addOrReplaceChild(
                "rbone3",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-4.51F, -11.9F, -17.39F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0F, -4.5F, -0.6981F, 0.0F, 0.0F)
        );

        rbone3.addOrReplaceChild(
                "rbone5",
                CubeListBuilder.create()
                        .texOffs(12, 55).addBox(-4.51F, -24.1F, -7.71F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.1F, -29.39F, -0.9163F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setHornVisibility(boolean left, boolean right) {
        this.straps.visible = left || right;
        this.leftHorn.visible = left;
        this.rightHorn.visible = right;
    }
}