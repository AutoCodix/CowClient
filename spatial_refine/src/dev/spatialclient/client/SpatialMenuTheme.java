package dev.spatialclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public final class SpatialMenuTheme {
    public static final ResourceLocation LOGO = new ResourceLocation("spatialclient", "textures/gui/spatial_logo.png");

    private SpatialMenuTheme() {}

    public static void drawSpace(GuiGraphics g, int width, int height, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, 0xFF02050E);
        long now = System.nanoTime();
        float t = (now % 40_000_000_000L) / 1_000_000_000.0F;
        float nx = width <= 0 ? 0.0F : mouseX / (float) width - 0.5F;
        float ny = height <= 0 ? 0.0F : mouseY / (float) height - 0.5F;

        // restrained moving blue-violet depth bands
        int bandA = Math.round((float)Math.sin(t * 0.18F) * 16.0F);
        int bandB = Math.round((float)Math.cos(t * 0.13F) * 20.0F);
        g.fill(0, Math.max(0, height / 5 + bandA), width, Math.min(height, height / 5 + bandA + 26), 0x1014447A);
        g.fill(0, Math.max(0, height * 3 / 4 + bandB), width, Math.min(height, height * 3 / 4 + bandB + 36), 0x101F1256);

        int driftX = Math.round(nx * 10.0F + t * 0.7F);
        int driftY = Math.round(ny * 7.0F + t * 0.24F);
        for (int i = 0; i < 125; i++) {
            int depth = 1 + (i % 3);
            int x = Math.floorMod(i * 173 + 47 + driftX * depth, Math.max(1, width));
            int y = Math.floorMod(i * 97 + 31 + driftY * depth, Math.max(1, height));
            int size = i % 23 == 0 ? 2 : 1;
            int color = i % 17 == 0 ? 0xFFD8EAFF : (i % 11 == 0 ? 0xFF9C86FF : 0xFF40577F);
            g.fill(x, y, x + size, y + size, color);
        }

        // soft vignette made from layered translucent edges
        int edgeX = Math.max(16, width / 12);
        int edgeY = Math.max(12, height / 12);
        g.fill(0, 0, edgeX, height, 0x24000000);
        g.fill(width - edgeX, 0, width, height, 0x24000000);
        g.fill(0, 0, width, edgeY, 0x1B000000);
        g.fill(0, height - edgeY, width, height, 0x26000000);
    }

    public static void drawPanelSpace(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        g.enableScissor(x, y, x + w, y + h);
        g.fill(x, y, x + w, y + h, 0xEE070B18);
        long now = System.nanoTime();
        float t = (now % 32_000_000_000L) / 1_000_000_000.0F;
        float nx = w <= 0 ? 0.0F : (mouseX - x) / (float)w - 0.5F;
        float ny = h <= 0 ? 0.0F : (mouseY - y) / (float)h - 0.5F;
        int dx = Math.round(nx * 6.0F + t * 0.35F);
        int dy = Math.round(ny * 4.0F + t * 0.16F);
        for (int i = 0; i < 58; i++) {
            int sx = x + Math.floorMod(i * 89 + 17 + dx * (1 + i % 2), Math.max(1, w));
            int sy = y + Math.floorMod(i * 53 + 11 + dy * (1 + i % 3), Math.max(1, h));
            int color = i % 12 == 0 ? 0xFF9AC7FF : (i % 9 == 0 ? 0xFF836EFF : 0xFF334463);
            int sz = i % 19 == 0 ? 2 : 1;
            g.fill(sx, sy, sx + sz, sy + sz, color);
        }
        g.disableScissor();
    }

    public static void drawLogo(GuiGraphics g, int x, int y, int size) {
        int r = Math.max(10, size / 3);
        UiRenderer.shadow(g, x, y, size, size, r);
        UiRenderer.roundedOutline(g, x, y, size, size, r, 1, 0xFF354A7D, 0xFF071022);
        int inset = Math.max(2, size / 18);
        g.blit(LOGO, x + inset, y + inset, 0.0F, 0.0F, size - inset * 2, size - inset * 2, 48, 48);
    }

    public static void click(float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, Math.max(0.65F, Math.min(1.35F, pitch))));
    }

    public static void rainbowText(GuiGraphics g, String text, float x, float y, float scale) {
        if (text == null || text.isEmpty()) return;
        float cursor = x;
        float base = (System.currentTimeMillis() % 6000L) / 6000.0F;
        for (int i = 0; i < text.length(); i++) {
            String s = String.valueOf(text.charAt(i));
            int rgb = java.awt.Color.HSBtoRGB((base + i * 0.055F) % 1.0F, 0.66F, 1.0F) & 0xFFFFFF;
            UiFont.draw(g, s, cursor, y, 0xFF000000 | rgb, scale, UiFont.Weight.SEMIBOLD);
            cursor += UiFont.width(s, scale, UiFont.Weight.SEMIBOLD);
        }
    }
}
