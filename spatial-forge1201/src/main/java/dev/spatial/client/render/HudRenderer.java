package dev.spatial.client.render;

import dev.spatial.client.config.ClientConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class HudRenderer {
    private HudRenderer() { }

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.get().crosshairEnabled || mc.options.getCameraType() != CameraType.FIRST_PERSON) return;
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        GuiGraphics g = event.getGuiGraphics();
        ClientConfig.Data cfg = ClientConfig.get();

        if (cfg.crosshairEnabled && mc.options.getCameraType() == CameraType.FIRST_PERSON) renderCrosshair(g, cfg);
        if (cfg.hudEnabled) {
            renderStats(g, cfg);
            if (cfg.showKeystrokes) renderKeystrokes(g, cfg);
        }
    }

    private static void renderCrosshair(GuiGraphics g, ClientConfig.Data cfg) {
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        int gap = Math.round(cfg.crosshairGap);
        int len = Math.round(cfg.crosshairLength);
        int t = Math.max(1, Math.round(cfg.crosshairThickness));
        int c = 0xFFF7FAFF;
        g.fill(cx - gap - len, cy - t / 2, cx - gap, cy + (t + 1) / 2, c);
        g.fill(cx + gap, cy - t / 2, cx + gap + len, cy + (t + 1) / 2, c);
        g.fill(cx - t / 2, cy - gap - len, cx + (t + 1) / 2, cy - gap, c);
        g.fill(cx - t / 2, cy + gap, cx + (t + 1) / 2, cy + gap + len, c);
    }

    private static void renderStats(GuiGraphics g, ClientConfig.Data cfg) {
        Minecraft mc = Minecraft.getInstance();
        int x = 8;
        int y = 8;
        int accent = 0xFF000000 | cfg.accentColor;
        if (cfg.showFps) {
            g.drawString(mc.font, "FPS " + Minecraft.getFps(), x, y, accent, true);
            y += 12;
        }
        if (cfg.showPing && mc.getConnection() != null && mc.player != null) {
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) {
                g.drawString(mc.font, "PING " + info.getLatency() + "ms", x, y, 0xFFD7E2EF, true);
                y += 12;
            }
        }
        if (cfg.showCoordinates && mc.player != null) {
            String xyz = String.format("XYZ %.1f  %.1f  %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            g.drawString(mc.font, xyz, x, y, 0xFFE8EDF5, true);
        }
    }

    private static void renderKeystrokes(GuiGraphics g, ClientConfig.Data cfg) {
        Minecraft mc = Minecraft.getInstance();
        int s = 20;
        int gap = 3;
        int x = g.guiWidth() - (s * 3 + gap * 2) - 10;
        int y = g.guiHeight() - (s * 3 + gap * 2) - 10;
        key(g, "W", x + s + gap, y, s, mc.options.keyUp.isDown(), cfg);
        key(g, "A", x, y + s + gap, s, mc.options.keyLeft.isDown(), cfg);
        key(g, "S", x + s + gap, y + s + gap, s, mc.options.keyDown.isDown(), cfg);
        key(g, "D", x + (s + gap) * 2, y + s + gap, s, mc.options.keyRight.isDown(), cfg);
        wideKey(g, "LMB", x, y + (s + gap) * 2, s + (s + gap) / 2, mc.options.keyAttack.isDown(), cfg);
        wideKey(g, "RMB", x + s + (s + gap) / 2 + gap, y + (s + gap) * 2,
                s + (s + gap) / 2, mc.options.keyUse.isDown(), cfg);
    }

    private static void key(GuiGraphics g, String label, int x, int y, int size, boolean down, ClientConfig.Data cfg) {
        int bg = down ? (0xD0000000 | cfg.accentColor) : 0x8A111924;
        int border = down ? (0xFF000000 | cfg.accentColor) : 0x90455566;
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, border);
        g.fill(x, y, x + size, y + size, bg);
        int tw = Minecraft.getInstance().font.width(label);
        g.drawString(Minecraft.getInstance().font, label, x + (size - tw) / 2, y + 6, 0xFFF5F8FC, false);
    }

    private static void wideKey(GuiGraphics g, String label, int x, int y, int width, boolean down, ClientConfig.Data cfg) {
        int bg = down ? (0xD0000000 | cfg.accentColor) : 0x8A111924;
        int border = down ? (0xFF000000 | cfg.accentColor) : 0x90455566;
        g.fill(x - 1, y - 1, x + width + 1, y + 21, border);
        g.fill(x, y, x + width, y + 20, bg);
        int tw = Minecraft.getInstance().font.width(label);
        g.drawString(Minecraft.getInstance().font, label, x + (width - tw) / 2, y + 6, 0xFFF5F8FC, false);
    }
}
