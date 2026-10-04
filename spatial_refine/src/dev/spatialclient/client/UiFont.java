package dev.spatialclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class UiFont {
    public enum Weight { REGULAR, SEMIBOLD }

    private UiFont() {}

    public static float width(String text, float scale, Weight weight) {
        if (text == null || text.isEmpty()) return 0.0F;
        return Minecraft.getInstance().font.width(text) * pixelScale(scale);
    }

    public static void draw(GuiGraphics g, String text, float x, float y, int argb, float scale, Weight weight) {
        if (text == null || text.isEmpty() || scale <= 0.0F) return;
        float s = pixelScale(scale);
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(s, s, 1.0F);
        g.drawString(Minecraft.getInstance().font, text, 0, 0, argb, false);
        g.pose().popPose();
    }

    private static float pixelScale(float legacyScale) {
        return Math.max(0.72F, legacyScale * 3.05F);
    }
}
