package dev.spatialclient.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import dev.spatialclient.config.SpatialConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    private HudRenderer() {}

    @SubscribeEvent
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        SpatialConfig cfg = SpatialConfig.get();
        if (player == null || mc.options.hideGui || SpatialCamera.isMenuCameraActive()) return;
        GuiGraphics g = event.getGuiGraphics();

        int leftY = 8;
        if (cfg.fpsHud) {
            drawPill(g, 8, leftY, "FPS  " + mc.getFps(), cfg.accentRgb);
            leftY += 18;
        }
        if (cfg.pingHud) {
            int ping = 0;
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) ping = info.getLatency();
            }
            drawPill(g, 8, leftY, "PING  " + ping + "ms", cfg.accentRgb);
            leftY += 18;
        }
        if (cfg.coordinatesHud) {
            String xyz = "XYZ  " + player.getBlockX() + "  " + player.getBlockY() + "  " + player.getBlockZ();
            drawPill(g, 8, leftY, xyz, cfg.accentRgb);
        }

        if (cfg.keystrokesHud) drawKeystrokes(g, cfg.accentRgb);
        if (cfg.armorHud) drawArmor(g, player);
        if (cfg.effectsHud) drawEffects(g, player, cfg.accentRgb);
    }

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        SpatialConfig cfg = SpatialConfig.get();
        if (!cfg.customCrosshair || mc.options.hideGui || SpatialCamera.isMenuCameraActive()) return;
        event.setCanceled(true);
        drawCrosshair(event.getGuiGraphics(), cfg);
    }

    private static void drawCrosshair(GuiGraphics g, SpatialConfig cfg) {
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        int c = 0xFF000000 | cfg.accentRgb;
        int shadow = 0xAA000000;
        switch (cfg.crosshairStyle) {
            case DOT -> {
                g.fill(cx - 2, cy - 2, cx + 3, cy + 3, shadow);
                g.fill(cx - 1, cy - 1, cx + 2, cy + 2, c);
            }
            case BRACKETS -> {
                g.fill(cx - 8, cy - 5, cx - 6, cy + 6, shadow);
                g.fill(cx + 6, cy - 5, cx + 8, cy + 6, shadow);
                g.fill(cx - 7, cy - 4, cx - 6, cy + 5, c);
                g.fill(cx + 6, cy - 4, cx + 7, cy + 5, c);
            }
            case CROSS -> {
                g.fill(cx - 1, cy - 7, cx + 2, cy + 8, shadow);
                g.fill(cx - 7, cy - 1, cx + 8, cy + 2, shadow);
                g.fill(cx, cy - 6, cx + 1, cy + 7, c);
                g.fill(cx - 6, cy, cx + 7, cy + 1, c);
            }
        }
    }

    private static void drawPill(GuiGraphics g, int x, int y, String text, int accent) {
        float scale = 0.255F;
        int w = Math.round(UiFont.width(text, scale, UiFont.Weight.SEMIBOLD)) + 14;
        UiRenderer.roundedRect(g, x, y, w, 16, 6, 0xB60E1020);
        UiRenderer.roundedRect(g, x + 4, y + 4, 2, 8, 1, 0xFF000000 | accent);
        UiFont.draw(g, text, x + 9, y + 4, 0xFFEDEAF7, scale, UiFont.Weight.SEMIBOLD);
    }

    private static void drawKeystrokes(GuiGraphics g, int accent) {
        Minecraft mc = Minecraft.getInstance();
        int x = 8;
        int y = g.guiHeight() - 68;
        key(g, x + 22, y, 20, 20, "W", mc.options.keyUp.isDown(), accent);
        key(g, x, y + 22, 20, 20, "A", mc.options.keyLeft.isDown(), accent);
        key(g, x + 22, y + 22, 20, 20, "S", mc.options.keyDown.isDown(), accent);
        key(g, x + 44, y + 22, 20, 20, "D", mc.options.keyRight.isDown(), accent);
        long window = mc.getWindow().getWindow();
        boolean l = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        key(g, x, y + 44, 31, 18, "LMB", l, accent);
        key(g, x + 33, y + 44, 31, 18, "RMB", r, accent);
    }

    private static void key(GuiGraphics g, int x, int y, int w, int h, String s, boolean down, int accent) {
        int bg = down ? (0xD0000000 | accent) : 0xB6111428;
        UiRenderer.roundedRect(g, x, y, w, h, 5, bg);
        float scale = 0.25F;
        float tw = UiFont.width(s, scale, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, s, x + (w - tw) * 0.5F, y + (h - 12) * 0.5F, 0xFFF1EFF8, scale, UiFont.Weight.SEMIBOLD);
    }

    private static void drawArmor(GuiGraphics g, LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> armor = new ArrayList<>();
        player.getArmorSlots().forEach(armor::add);
        Collections.reverse(armor);
        int visible = 0;
        for (ItemStack stack : armor) if (!stack.isEmpty()) visible++;
        if (visible == 0) return;
        int x = g.guiWidth() / 2 - (visible * 20) / 2;
        int y = g.guiHeight() - 48;
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) continue;
            g.fill(x - 1, y - 1, x + 18, y + 18, 0x84121419);
            g.renderItem(stack, x, y);
            g.renderItemDecorations(mc.font, stack, x, y);
            x += 20;
        }
    }

    private static void drawEffects(GuiGraphics g, LocalPlayer player, int accent) {
        List<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
        effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        int y = 8;
        for (MobEffectInstance effect : effects) {
            String name = effect.getEffect().getDisplayName().getString();
            if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
            int seconds = Math.max(0, effect.getDuration() / 20);
            String time = String.format("%d:%02d", seconds / 60, seconds % 60);
            String text = name + "  " + time;
            float scale = 0.245F;
            int w = Math.round(UiFont.width(text, scale, UiFont.Weight.REGULAR)) + 14;
            int x = g.guiWidth() - w - 8;
            UiRenderer.roundedRect(g, x, y, w, 16, 6, 0xB60E1020);
            UiRenderer.roundedRect(g, x + w - 6, y + 4, 2, 8, 1, 0xFF000000 | accent);
            UiFont.draw(g, text, x + 7, y + 4, 0xFFE9E7F2, scale, UiFont.Weight.REGULAR);
            y += 19;
            if (y > g.guiHeight() / 2) break;
        }
    }
}
