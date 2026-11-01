package net.sqdy.goat_expansion.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.sqdy.goat_expansion.GoatExpansion;

public class GoatPouchScreen extends AbstractContainerScreen<GoatPouchScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            GoatExpansion.MOD_ID,
            "textures/gui/container/goat_inventory.png"
    );

    public GoatPouchScreen(GoatPouchScreenHandler menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        // 1. Базовая рамка окна (176x166)
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

        // 2. Чистая заливка верхней части стандартным серым цветом контейнера Minecraft (#C6C6C6) без черных полос
        graphics.fill(
                this.leftPos + 6,
                this.topPos + 15,
                this.leftPos + 170,
                this.topPos + 74,
                0xFFC6C6C6
        );

        // 3. Вырезаем ровно 6 слотов (3x2, размер 54x36) из правой сетки текстуры (u=176, v=11) и рисуем по центру
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                this.leftPos + 61,
                this.topPos + 25,
                176.0F,
                11.0F,
                54,
                36,
                256,
                256
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}