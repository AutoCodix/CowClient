package dev.spatialclient.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class UiFont {
    public enum Weight { REGULAR, SEMIBOLD }

    private static final ResourceLocation REGULAR = new ResourceLocation("spatialclient", "textures/gui/ui_font_regular.png");
    private static final ResourceLocation SEMIBOLD = new ResourceLocation("spatialclient", "textures/gui/ui_font_semibold.png");
    private static final int CELL_W = 44;
    private static final int CELL_H = 48;
    private static final int ATLAS_W = 704;
    private static final int ATLAS_H = 288;
    private static final int FIRST = 32;
    private static final int LAST = 126;
    private static boolean filterSet;

    private static final int[] REGULAR_WIDTHS = new int[]{8,7,12,18,18,25,18,8,9,9,14,19,7,13,7,10,18,11,17,18,19,17,18,15,17,18,7,7,19,19,19,16,29,20,19,22,21,17,17,22,21,7,16,19,16,26,21,22,18,22,19,18,18,21,20,28,19,19,18,9,10,9,13,14,7,16,17,16,17,16,9,17,16,6,6,15,6,25,16,16,17,17,10,14,9,16,15,23,15,15,14,12,9,12,19};
    private static final int[] SEMIBOLD_WIDTHS = new int[]{7,7,13,18,19,27,19,8,10,10,15,19,7,13,7,11,19,11,18,18,19,18,18,16,18,18,7,7,19,19,19,17,30,21,19,22,21,18,17,22,21,8,17,20,16,26,22,22,19,22,19,19,19,21,21,29,20,20,19,10,11,10,14,14,8,16,18,16,18,17,10,18,17,7,7,16,7,26,17,17,18,18,11,15,10,17,16,24,16,16,15,13,10,13,19};

    private UiFont() {}

    public static float width(String text, float scale, Weight weight) {
        if (text == null || text.isEmpty()) return 0.0F;
        int[] widths = widths(weight);
        float line = 0.0F;
        float max = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                max = Math.max(max, line);
                line = 0.0F;
                continue;
            }
            int idx = normalize(ch) - FIRST;
            line += (widths[idx] + 1) * scale;
        }
        return Math.max(max, line);
    }

    public static void draw(GuiGraphics g, String text, float x, float y, int argb, float scale, Weight weight) {
        if (text == null || text.isEmpty() || scale <= 0.0F) return;
        ensureFilter();
        ResourceLocation texture = weight == Weight.SEMIBOLD ? SEMIBOLD : REGULAR;
        int[] widths = widths(weight);
        float a = ((argb >>> 24) & 255) / 255.0F;
        float r = ((argb >>> 16) & 255) / 255.0F;
        float gr = ((argb >>> 8) & 255) / 255.0F;
        float b = (argb & 255) / 255.0F;

        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        RenderSystem.setShaderColor(r, gr, b, a);
        int cursorX = 0;
        int cursorY = 0;
        for (int i = 0; i < text.length(); i++) {
            char raw = text.charAt(i);
            if (raw == '\n') {
                cursorX = 0;
                cursorY += 38;
                continue;
            }
            char ch = normalize(raw);
            int idx = ch - FIRST;
            int col = idx % 16;
            int row = idx / 16;
            int sw = Math.min(CELL_W, widths[idx] + 8);
            if (ch != ' ') {
                g.blit(texture, cursorX, cursorY, sw, CELL_H, col * CELL_W, row * CELL_H, sw, CELL_H, ATLAS_W, ATLAS_H);
            }
            cursorX += widths[idx] + 1;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }

    private static char normalize(char ch) {
        return ch < FIRST || ch > LAST ? '?' : ch;
    }

    private static int[] widths(Weight weight) {
        return weight == Weight.SEMIBOLD ? SEMIBOLD_WIDTHS : REGULAR_WIDTHS;
    }

    private static void ensureFilter() {
        if (filterSet) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.getTextureManager().getTexture(REGULAR).setFilter(true, false);
            mc.getTextureManager().getTexture(SEMIBOLD).setFilter(true, false);
            filterSet = true;
        } catch (Exception ignored) {
        }
    }
}
