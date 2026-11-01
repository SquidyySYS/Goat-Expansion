package net.sqdy.goat_expansion.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.animal.goat.Goat;
import net.sqdy.goat_expansion.entity.GoatSaddleAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.options.hideGui && mc.player.getVehicle() instanceof Goat goat) {
            if (goat instanceof GoatSaddleAccessor accessor && accessor.goat_expansion$isSaddled()) {
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                int screenHeight = mc.getWindow().getGuiScaledHeight();

                // Компактная полоска (80px) слева от сердечек козы — не наезжает на второй ряд сердец
                int width = 80;
                int height = 5;
                int x = screenWidth / 2 - 91;
                int y = screenHeight - 39;

                int baseLimitX = x + (int) (width * 0.80F);
                int stunTicks = accessor.goat_expansion$getStunTicks();
                float effectiveCharge = accessor.goat_expansion$getEffectiveCharge();

                graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
                graphics.fill(x, y, baseLimitX, y + height, 0xFF2B1B17);
                graphics.fill(baseLimitX, y, x + width, y + height, 0xFF3D271D);

                if (stunTicks > 0) {
                    float stunProgress = Math.min(1.0F, (float) stunTicks / 30.0F);
                    int fillWidth = (int) (stunProgress * width);
                    graphics.fill(x, y, x + fillWidth, y + height, 0xFFAA0044);
                    graphics.fill(x, y, x + fillWidth, y + 1, 0xFFFF5588);
                } else if (effectiveCharge > 0.0F) {
                    int fillWidth = (int) (effectiveCharge * width);

                    int mainColor = (effectiveCharge > 0.805F) ? 0xFF00E5FF : 0xFFFF8800;
                    int highlightColor = (effectiveCharge > 0.805F) ? 0xFFAAFFFF : 0xFFFFCC44;

                    graphics.fill(x, y, x + fillWidth, y + height, mainColor);
                    graphics.fill(x, y, x + fillWidth, y + 1, highlightColor);
                }

                // Белая отсечка 80% (8 урона) и деления пиковой зоны (8..10 урона)
                graphics.fill(baseLimitX, y - 1, baseLimitX + 1, y + height + 1, 0xFFFFFFFF);
                int peakZoneWidth = width - (int) (width * 0.80F);
                for (int i = 1; i <= 3; i++) {
                    int divX = baseLimitX + (i * peakZoneWidth / 4);
                    graphics.fill(divX, y, divX + 1, y + height, 0xAA000000);
                }
            }
        }
    }
}