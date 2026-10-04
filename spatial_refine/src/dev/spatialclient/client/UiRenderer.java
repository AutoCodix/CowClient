package dev.spatialclient.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class UiRenderer {
    private UiRenderer() {}

    public static void roundedRect(GuiGraphics g, float x, float y, float w, float h, float radius, int argb) {
        if (w <= 0 || h <= 0) return;
        radius = Math.max(0.0F, Math.min(radius, Math.min(w, h) * 0.5F));
        if (radius < 1.0F) {
            g.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), argb);
            return;
        }

        int a = (argb >>> 24) & 255;
        int r = (argb >>> 16) & 255;
        int gr = (argb >>> 8) & 255;
        int b = argb & 255;
        Matrix4f matrix = g.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(matrix, x + w * 0.5F, y + h * 0.5F, 0.0F).color(r, gr, b, a).endVertex();

        final int steps = 8;
        arc(buf, matrix, x + radius, y + radius, radius, 180.0, 270.0, steps, r, gr, b, a);
        arc(buf, matrix, x + w - radius, y + radius, radius, 270.0, 360.0, steps, r, gr, b, a);
        arc(buf, matrix, x + w - radius, y + h - radius, radius, 0.0, 90.0, steps, r, gr, b, a);
        arc(buf, matrix, x + radius, y + h - radius, radius, 90.0, 180.0, steps, r, gr, b, a);
        double rad = Math.toRadians(180.0);
        buf.vertex(matrix, x + radius + (float)Math.cos(rad) * radius, y + radius + (float)Math.sin(rad) * radius, 0.0F)
                .color(r, gr, b, a).endVertex();
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.enableCull();
    }

    private static void arc(BufferBuilder buf, Matrix4f matrix, float cx, float cy, float radius,
                            double startDeg, double endDeg, int steps, int r, int g, int b, int a) {
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double angle = Math.toRadians(startDeg + (endDeg - startDeg) * t);
            float px = cx + (float) Math.cos(angle) * radius;
            float py = cy + (float) Math.sin(angle) * radius;
            buf.vertex(matrix, px, py, 0.0F).color(r, g, b, a).endVertex();
        }
    }

    public static void shadow(GuiGraphics g, float x, float y, float w, float h, float radius) {
        roundedRect(g, x + 1.5F, y + 3.5F, w, h, radius + 1, 0x4A000000);
        roundedRect(g, x + 0.5F, y + 1.5F, w, h, radius, 0x25000000);
    }

    public static void roundedOutline(GuiGraphics g, float x, float y, float w, float h, float radius, float thickness, int outer, int inner) {
        roundedRect(g, x, y, w, h, radius, outer);
        roundedRect(g, x + thickness, y + thickness, w - thickness * 2.0F, h - thickness * 2.0F,
                Math.max(0.0F, radius - thickness), inner);
    }

    public static void circle(GuiGraphics g, float cx, float cy, float radius, int argb) {
        roundedRect(g, cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, radius, argb);
    }
}
