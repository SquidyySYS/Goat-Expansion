package net.sqdy.goat_expansion.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Inventory;
import net.sqdy.goat_expansion.GoatExpansion;

public class GoatScreen extends AbstractContainerScreen<GoatScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            GoatExpansion.MOD_ID,
            "textures/gui/container/goat_inventory.png"
    );

    public GoatScreen(GoatScreenHandler menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // Не считать правую панель мешочков кликом "за пределами инвентаря" (предотвращает выброс предмета)
    // Не считать правую панель мешочков кликом "за пределами инвентаря" (предотвращает выброс предмета)
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int leftPos, int topPos) {
        int pouchCount = this.menu.getEquippedPouchCount();
        if (pouchCount > 0) {
            int pouchBottom = topPos + 11 + pouchCount * 36;
            if (mouseX >= leftPos + 176 && mouseX < leftPos + 230 && mouseY >= topPos + 11 && mouseY < pouchBottom) {
                return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, leftPos, topPos);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        // 1. Основное окно инвентаря козы (176x166)
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256
        );

        // 2. Отрисовка ячеек мешочков справа сверху вниз (u=176, v=11, размер 54x36 на каждый мешочек)
        int pouchCount = this.menu.getEquippedPouchCount();
        for (int i = 0; i < pouchCount; i++) {
            int drawY = this.topPos + 11 + i * 36;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    this.leftPos + 176,
                    drawY,
                    176.0F,
                    11.0F,
                    54,
                    36,
                    256,
                    256
            );
        }

        // 3. Отрисовка 3D-модели козы в окне слева
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof Goat goat) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    this.leftPos + 26,
                    this.topPos + 17,
                    this.leftPos + 77,
                    this.topPos + 69,
                    17,
                    0.0625F,
                    mouseX,
                    mouseY,
                    goat
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}