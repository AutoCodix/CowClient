package dev.spatialclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class UiFont {
    public enum Weight { REGULAR, SEMIBOLD }

    private static final ResourceLocation SPATIAL_UI = new ResourceLocation("spatialclient", "ui");

    private UiFont() {}

    private static Component styled(String text) {
        return Component.literal(text == null ? "" : text).withStyle(style -> style.withFont(SPATIAL_UI));
    }

    public static float width(String text, float scale, Weight weight) {
        if (text == null || text.isEmpty()) return 0.0F;
        return Minecraft.getInstance().font.width(styled(text)) * pixelScale(scale);
    }

    public static void draw(GuiGraphics g, String text, float x, float y, int argb, float scale, Weight weight) {
        if (text == null || text.isEmpty() || scale <= 0.0F) return;
        float s = pixelScale(scale);
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(s, s, 1.0F);
        g.drawString(Minecraft.getInstance().font, styled(text), 0, 0, argb, false);
        g.pose().popPose();
    }

    private static float pixelScale(float legacyScale) {
        return Math.max(0.62F, legacyScale * 2.72F);
    }
}
