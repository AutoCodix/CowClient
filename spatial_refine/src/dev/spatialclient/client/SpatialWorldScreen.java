package dev.spatialclient.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

public final class SpatialWorldScreen extends SelectWorldScreen {
    public SpatialWorldScreen(Screen parent) {
        super(parent);
    }

    public void renderBackground(GuiGraphics g) {
        SpatialMenuTheme.drawSpace(g, width, height, width / 2, height / 2);
    }

    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SpatialMenuTheme.drawSpace(g, width, height, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        UiRenderer.roundedOutline(g, 12, 10, 148, 27, 9, 1, 0xFF30436F, 0xBB071022);
        UiFont.draw(g, "SPATIAL  /  WORLDS", 24, 18, 0xFFDDE7FA, 0.25F, UiFont.Weight.SEMIBOLD);
    }
}
