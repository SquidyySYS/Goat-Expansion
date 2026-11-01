package net.sqdy.goat_expansion.mixin.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.sqdy.goat_expansion.GoatExpansionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoatModel.class)
public abstract class GoatModelMixin extends QuadrupedModel<GoatRenderState> {

    protected GoatModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/GoatRenderState;)V", at = @At("TAIL"))
    private void onSetupAnim(GoatRenderState state, CallbackInfo ci) {
        if (GoatExpansionClient.isChargingStance) {
            this.head.xRot += 0.85F; // Наклон рогов вперед в режиме тарана
        }
    }
}