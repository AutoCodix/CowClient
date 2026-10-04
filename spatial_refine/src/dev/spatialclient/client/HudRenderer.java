package dev.spatialclient.client;

import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public final class HudRenderer {
    private HudRenderer() {}

    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        SpatialConfig cfg = SpatialConfig.get();
        if (player == null || mc.options.hideGui || SpatialCamera.isMenuCameraActive()) return;
        GuiGraphics g = event.getGuiGraphics();

        EnumMap<SpatialConfig.HudCorner, Integer> offsets = new EnumMap<>(SpatialConfig.HudCorner.class);
        for (SpatialConfig.HudCorner c : SpatialConfig.HudCorner.values()) offsets.put(c, 8);

        if (cfg.fpsHud) {
            drawCornerPill(g, "FPS  " + mc.getFps(), cfg.fpsCorner, cfg.fpsScale, cfg.fpsOpacity,
                    cfg.fpsAccentBar, cfg.accentRgb, offsets);
        }
        if (cfg.pingHud) {
            int ping = 0;
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) ping = info.getLatency();
            }
            drawCornerPill(g, "PING  " + ping + "ms", cfg.pingCorner, cfg.pingScale, cfg.pingOpacity,
                    cfg.pingAccentBar, cfg.accentRgb, offsets);
        }
        if (cfg.coordinatesHud) {
            String xyz = cfg.decimalCoordinates
                    ? String.format("XYZ  %.1f  %.1f  %.1f", player.getX(), player.getY(), player.getZ())
                    : "XYZ  " + player.getBlockX() + "  " + player.getBlockY() + "  " + player.getBlockZ();
            drawCornerPill(g, xyz, cfg.coordinatesCorner, cfg.coordinatesScale, cfg.coordinatesOpacity,
                    cfg.coordinatesAccentBar, cfg.accentRgb, offsets);
        }

        if (cfg.keystrokesHud) drawKeystrokes(g, cfg);
        if (cfg.armorHud) drawArmor(g, player, cfg);
        if (cfg.effectsHud) drawEffects(g, player, cfg);
    }

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
        int rgb = cfg.crosshairUseAccent ? cfg.accentRgb : cfg.crosshairColorRgb;
        int alpha = Math.round(255.0F * (cfg.crosshairOpacity / 100.0F));
        int c = (alpha << 24) | rgb;
        int shadow = (Math.min(210, alpha) << 24);
        int size = cfg.crosshairSize;
        int thick = cfg.crosshairThickness;
        int gap = cfg.crosshairGap;

        switch (cfg.crosshairStyle) {
            case DOT -> {
                int rad = Math.max(1, size / 3);
                if (cfg.crosshairOutline) g.fill(cx - rad - 1, cy - rad - 1, cx + rad + 2, cy + rad + 2, shadow);
                g.fill(cx - rad, cy - rad, cx + rad + 1, cy + rad + 1, c);
            }
            case BRACKETS -> {
                int halfH = size;
                int offset = gap + Math.max(3, size / 2);
                if (cfg.crosshairOutline) {
                    g.fill(cx - offset - thick - 1, cy - halfH - 1, cx - offset + 1, cy + halfH + 2, shadow);
                    g.fill(cx + offset - 1, cy - halfH - 1, cx + offset + thick + 1, cy + halfH + 2, shadow);
                }
                g.fill(cx - offset - thick, cy - halfH, cx - offset, cy + halfH + 1, c);
                g.fill(cx + offset, cy - halfH, cx + offset + thick, cy + halfH + 1, c);
            }
            case CROSS -> {
                int len = size;
                if (cfg.crosshairOutline) {
                    g.fill(cx - thick, cy - gap - len - 1, cx + thick + 1, cy - gap + 1, shadow);
                    g.fill(cx - thick, cy + gap - 1, cx + thick + 1, cy + gap + len + 2, shadow);
                    g.fill(cx - gap - len - 1, cy - thick, cx - gap + 1, cy + thick + 1, shadow);
                    g.fill(cx + gap - 1, cy - thick, cx + gap + len + 2, cy + thick + 1, shadow);
                }
                g.fill(cx - thick + 1, cy - gap - len, cx + thick, cy - gap, c);
                g.fill(cx - thick + 1, cy + gap, cx + thick, cy + gap + len + 1, c);
                g.fill(cx - gap - len, cy - thick + 1, cx - gap, cy + thick, c);
                g.fill(cx + gap, cy - thick + 1, cx + gap + len + 1, cy + thick, c);
            }
        }
    }

    private static void drawCornerPill(GuiGraphics g, String text, SpatialConfig.HudCorner corner,
                                       float scale, float opacity, boolean accentBar, int accent,
                                       EnumMap<SpatialConfig.HudCorner, Integer> offsets) {
        float textScale = 0.255F * scale;
        int h = Math.max(14, Math.round(16 * scale));
        int w = Math.round(UiFont.width(text, textScale, UiFont.Weight.SEMIBOLD)) + Math.round(14 * scale);
        int offset = offsets.get(corner);
        int x = switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> 8;
            case TOP_RIGHT, BOTTOM_RIGHT -> g.guiWidth() - w - 8;
        };
        int y = switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> offset;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> g.guiHeight() - offset - h;
        };
        int alpha = Mth.clamp(Math.round(opacity * 255.0F), 25, 255);
        UiRenderer.roundedRect(g, x, y, w, h, Math.max(4, Math.round(6 * scale)), (alpha << 24) | 0x0E1020);
        if (accentBar) UiRenderer.roundedRect(g, x + Math.round(4 * scale), y + Math.round(4 * scale),
                Math.max(2, Math.round(2 * scale)), Math.max(6, h - Math.round(8 * scale)), 1, 0xFF000000 | accent);
        float textX = x + Math.round((accentBar ? 9 : 6) * scale);
        float textY = y + Math.max(2, Math.round(4 * scale));
        UiFont.draw(g, text, textX, textY, 0xFFEDEAF7, textScale, UiFont.Weight.SEMIBOLD);
        offsets.put(corner, offset + h + Math.max(2, Math.round(3 * scale)));
    }

    private static void drawKeystrokes(GuiGraphics g, SpatialConfig cfg) {
        float s = cfg.keystrokesScale;
        int key = Math.round(20 * s);
        int gap = Math.max(2, Math.round(2 * s));
        int mouseH = Math.round(18 * s);
        int blockW = key * 3 + gap * 2;
        int blockH = key * 2 + gap + (cfg.keystrokesMouseButtons ? mouseH + gap : 0);
        int[] pos = anchored(g, cfg.keystrokesCorner, blockW, blockH, 8);
        int x = pos[0], y = pos[1];
        Minecraft mc = Minecraft.getInstance();

        key(g, x + key + gap, y, key, key, "W", mc.options.keyUp.isDown(), cfg);
        key(g, x, y + key + gap, key, key, "A", mc.options.keyLeft.isDown(), cfg);
        key(g, x + key + gap, y + key + gap, key, key, "S", mc.options.keyDown.isDown(), cfg);
        key(g, x + (key + gap) * 2, y + key + gap, key, key, "D", mc.options.keyRight.isDown(), cfg);
        if (cfg.keystrokesMouseButtons) {
            long window = mc.getWindow().getWindow();
            boolean l = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            int half = (blockW - gap) / 2;
            key(g, x, y + key * 2 + gap * 2, half, mouseH, "LMB", l, cfg);
            key(g, x + half + gap, y + key * 2 + gap * 2, half, mouseH, "RMB", r, cfg);
        }
    }

    private static void key(GuiGraphics g, int x, int y, int w, int h, String text, boolean down, SpatialConfig cfg) {
        int alpha = Mth.clamp(Math.round(cfg.keystrokesOpacity * 255.0F), 25, 255);
        int bg = down ? ((Math.min(235, alpha) << 24) | cfg.accentRgb) : ((alpha << 24) | 0x111428);
        UiRenderer.roundedRect(g, x, y, w, h, Math.max(4, Math.round(5 * cfg.keystrokesScale)), bg);
        float textScale = 0.25F * cfg.keystrokesScale;
        float tw = UiFont.width(text, textScale, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, text, x + (w - tw) * 0.5F, y + (h - 12 * cfg.keystrokesScale) * 0.5F,
                0xFFF1EFF8, textScale, UiFont.Weight.SEMIBOLD);
    }

    private static void drawArmor(GuiGraphics g, LocalPlayer player, SpatialConfig cfg) {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> armor = new ArrayList<>();
        player.getArmorSlots().forEach(armor::add);
        java.util.Collections.reverse(armor);
        armor.removeIf(ItemStack::isEmpty);
        if (armor.isEmpty()) return;

        float scale = cfg.armorScale;
        int itemStep = 20;
        int totalW = armor.size() * itemStep;
        int x;
        int y;
        switch (cfg.armorAnchor) {
            case ABOVE_HOTBAR -> {
                x = g.guiWidth() / 2 - Math.round(totalW * scale / 2.0F);
                y = g.guiHeight() - Math.round(48 * scale);
            }
            case TOP_CENTER -> {
                x = g.guiWidth() / 2 - Math.round(totalW * scale / 2.0F);
                y = 8;
            }
            case BOTTOM_RIGHT -> {
                x = g.guiWidth() - Math.round(totalW * scale) - 8;
                y = g.guiHeight() - Math.round(24 * scale) - 8;
            }
            default -> { return; }
        }

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        int drawX = 0;
        for (ItemStack stack : armor) {
            UiRenderer.roundedRect(g, drawX - 1, -1, 19, 19, 4, 0x86121419);
            g.renderItem(stack, drawX, 0);
            if (cfg.armorDurability) g.renderItemDecorations(mc.font, stack, drawX, 0);
            drawX += itemStep;
        }
        g.pose().popPose();
    }

    private static void drawEffects(GuiGraphics g, LocalPlayer player, SpatialConfig cfg) {
        List<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
        if (cfg.effectsSort == SpatialConfig.EffectSort.NAME) {
            effects.sort(Comparator.comparing(e -> e.getEffect().getDisplayName().getString()));
        } else {
            effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        }
        float scale = cfg.effectsScale;
        float textScale = 0.245F * scale;
        int h = Math.max(14, Math.round(16 * scale));
        int gap = Math.max(2, Math.round(3 * scale));
        int offset = 8;
        int alpha = Mth.clamp(Math.round(cfg.effectsOpacity * 255.0F), 25, 255);

        for (MobEffectInstance effect : effects) {
            String name = effect.getEffect().getDisplayName().getString();
            if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
            String text = name;
            if (cfg.effectsDuration) {
                int seconds = Math.max(0, effect.getDuration() / 20);
                text += "  " + String.format("%d:%02d", seconds / 60, seconds % 60);
            }
            int w = Math.round(UiFont.width(text, textScale, UiFont.Weight.REGULAR)) + Math.round(14 * scale);
            int x = switch (cfg.effectsCorner) {
                case TOP_LEFT, BOTTOM_LEFT -> 8;
                case TOP_RIGHT, BOTTOM_RIGHT -> g.guiWidth() - w - 8;
            };
            int y = switch (cfg.effectsCorner) {
                case TOP_LEFT, TOP_RIGHT -> offset;
                case BOTTOM_LEFT, BOTTOM_RIGHT -> g.guiHeight() - offset - h;
            };
            UiRenderer.roundedRect(g, x, y, w, h, Math.max(4, Math.round(6 * scale)), (alpha << 24) | 0x0E1020);
            UiRenderer.roundedRect(g, x + w - Math.round(6 * scale), y + Math.round(4 * scale),
                    Math.max(2, Math.round(2 * scale)), Math.max(6, h - Math.round(8 * scale)), 1, 0xFF000000 | cfg.accentRgb);
            UiFont.draw(g, text, x + Math.round(7 * scale), y + Math.max(2, Math.round(4 * scale)),
                    0xFFE9E7F2, textScale, UiFont.Weight.REGULAR);
            offset += h + gap;
            if (offset > g.guiHeight() / 2) break;
        }
    }

    private static int[] anchored(GuiGraphics g, SpatialConfig.HudCorner corner, int w, int h, int margin) {
        return switch (corner) {
            case TOP_LEFT -> new int[]{margin, margin};
            case TOP_RIGHT -> new int[]{g.guiWidth() - w - margin, margin};
            case BOTTOM_LEFT -> new int[]{margin, g.guiHeight() - h - margin};
            case BOTTOM_RIGHT -> new int[]{g.guiWidth() - w - margin, g.guiHeight() - h - margin};
        };
    }
}
