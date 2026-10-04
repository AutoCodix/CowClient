package dev.spatial.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.spatial.client.camera.SpatialCameraController;
import dev.spatial.client.config.ClientConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

public final class AuraRenderer {
    private AuraRenderer() { }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientConfig.get().auraEnabled) return;

        boolean thirdPerson = mc.options.getCameraType() != CameraType.FIRST_PERSON;
        boolean detached = mc.gameRenderer.getMainCamera().isDetached();
        boolean visibleContext = thirdPerson || detached || SpatialCameraController.isActive();
        if (!visibleContext) return;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        double px = lerp(mc.player.xOld, mc.player.getX(), event.getPartialTick()) - camera.x;
        double py = lerp(mc.player.yOld, mc.player.getY(), event.getPartialTick()) - camera.y;
        double pz = lerp(mc.player.zOld, mc.player.getZ(), event.getPartialTick()) - camera.z;

        pose.pushPose();
        pose.translate(px, py, pz);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        renderAura(pose, event.getPartialTick());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void renderAura(PoseStack pose, float partialTick) {
        ClientConfig.Data cfg = ClientConfig.get();
        int color = cfg.accentColor;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = cfg.auraOpacity;
        float time = ((System.currentTimeMillis() % 100_000L) / 1000f + partialTick / 20f) * cfg.auraSpeed;
        Matrix4f matrix = pose.last().pose();

        switch (cfg.auraStyle) {
            case HALO -> {
                circle(matrix, 1.92f, cfg.auraRadius * 0.74f, r, g, b, a, time * 0.35f);
                circle(matrix, 0.08f, cfg.auraRadius, r, g, b, a * 0.68f, -time * 0.2f);
            }
            case SPIRAL -> spiral(matrix, cfg.auraRadius, r, g, b, a, time);
            case CARDS -> cards(matrix, cfg.auraRadius, cfg.auraCount, r, g, b, a, time);
            case MICE -> mice(matrix, cfg.auraRadius, cfg.auraCount, r, g, b, a, time);
            case SHARDS -> shards(matrix, cfg.auraRadius, cfg.auraCount, r, g, b, a, time);
            case STARS -> stars(matrix, cfg.auraRadius, cfg.auraCount, r, g, b, a, time);
            case RUNES -> runes(matrix, cfg.auraRadius, cfg.auraCount, r, g, b, a, time);
            case MIXED -> mixed(matrix, cfg.auraRadius, Math.max(8, cfg.auraCount), r, g, b, a, time);
            case ORBIT -> {
                circle(matrix, 0.08f, cfg.auraRadius, r, g, b, a, time);
                circle(matrix, 0.94f, cfg.auraRadius * 0.78f, r, g, b, a * 0.72f, -time * 0.7f);
                orbitPoints(matrix, cfg.auraRadius, cfg.auraCount, r, g, b, a, time);
            }
        }
    }

    private static void mixed(Matrix4f matrix, float radius, int count, float r, float g, float b, float a, float time) {
        circle(matrix, 0.08f, radius * 0.94f, r, g, b, a * 0.45f, time * 0.35f);
        int each = Math.max(2, count / 4);
        cards(matrix, radius, each, r, g, b, a * 0.9f, time);
        mice(matrix, radius * 0.91f, each, r, g, b, a * 0.92f, -time * 0.82f + 1.2f);
        stars(matrix, radius * 0.82f, each, r, g, b, a * 0.82f, time * 1.15f + 2.3f);
        shards(matrix, radius * 0.72f, each, r, g, b, a * 0.72f, -time * 1.06f + 0.6f);
    }

    private static void circle(Matrix4f matrix, float y, float radius, float r, float g, float b, float a, float phase) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int segments = 72;
        for (int i = 0; i <= segments; i++) {
            double ang = Math.PI * 2.0 * i / segments + phase;
            bb.vertex(matrix, (float) Math.cos(ang) * radius, y, (float) Math.sin(ang) * radius)
                    .color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void spiral(Matrix4f matrix, float radius, float r, float g, float b, float a, float time) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int segments = 120;
        for (int i = 0; i <= segments; i++) {
            float p = i / (float) segments;
            double ang = p * Math.PI * 5.0 + time * 2.0;
            float rad = radius * (0.72f + 0.18f * (float) Math.sin(p * Math.PI));
            float y = 0.10f + p * 1.85f;
            float fade = (float) Math.sin(p * Math.PI);
            bb.vertex(matrix, (float) Math.cos(ang) * rad, y, (float) Math.sin(ang) * rad)
                    .color(r, g, b, a * fade).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void orbitPoints(Matrix4f matrix, float radius, int count, float r, float g, float b, float a, float time) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            double ang = time * 1.6 + i * (Math.PI * 2 / count);
            float x = (float) Math.cos(ang) * radius;
            float z = (float) Math.sin(ang) * radius;
            float y = 0.18f + (i % 5) * 0.35f;
            billboardQuad(bb, matrix, ang, x, y, z, 0.045f, 0.045f, r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void cards(Matrix4f matrix, float radius, int count, float r, float g, float b, float a, float time) {
        count = Math.max(3, count);
        for (int i = 0; i < count; i++) {
            double ang = time * 0.95 + i * (Math.PI * 2 / count);
            float x = (float) Math.cos(ang) * radius;
            float z = (float) Math.sin(ang) * radius;
            float y = 0.24f + (i % 4) * 0.43f + 0.05f * (float) Math.sin(time * 2.0 + i);
            card(matrix, ang, x, y, z, r, g, b, a, i);
        }
    }

    private static void card(Matrix4f matrix, double ang, float x, float y, float z,
                             float r, float g, float b, float a, int index) {
        BufferBuilder fill = Tesselator.getInstance().getBuilder();
        fill.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        billboardQuad(fill, matrix, ang, x, y, z, 0.105f, 0.155f, 0.035f, 0.045f, 0.065f, a * 0.78f);
        BufferUploader.drawWithShader(fill.end());

        BufferBuilder outline = Tesselator.getInstance().getBuilder();
        outline.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        localPoint(outline, matrix, ang, x, y, z, -0.105f, -0.155f, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z,  0.105f, -0.155f, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z,  0.105f,  0.155f, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z, -0.105f,  0.155f, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z, -0.105f, -0.155f, r, g, b, a);
        float motif = index % 2 == 0 ? 0.048f : 0.038f;
        localPoint(outline, matrix, ang, x, y, z, 0, motif, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z, motif, 0, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z, 0, -motif, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z, -motif, 0, r, g, b, a);
        localPoint(outline, matrix, ang, x, y, z, 0, motif, r, g, b, a);
        BufferUploader.drawWithShader(outline.end());
    }

    private static void mice(Matrix4f matrix, float radius, int count, float r, float g, float b, float a, float time) {
        count = Math.max(3, count);
        for (int i = 0; i < count; i++) {
            double ang = -time * 0.90 + i * (Math.PI * 2 / count);
            float x = (float) Math.cos(ang) * radius;
            float z = (float) Math.sin(ang) * radius;
            float y = 0.30f + (i % 5) * 0.34f;
            mouseGlyph(matrix, ang, x, y, z, r, g, b, a);
        }
    }

    private static void mouseGlyph(Matrix4f matrix, double ang, float x, float y, float z,
                                   float r, float g, float b, float a) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int seg = 18;
        for (int i = 0; i <= seg; i++) {
            double t = Math.PI * 2.0 * i / seg;
            float u = (float) Math.cos(t) * 0.10f;
            float v = (float) Math.sin(t) * 0.145f;
            localPoint(bb, matrix, ang, x, y, z, u, v, r, g, b, a);
        }
        BufferUploader.drawWithShader(bb.end());

        BufferBuilder detail = Tesselator.getInstance().getBuilder();
        detail.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        localPoint(detail, matrix, ang, x, y, z, 0f, 0.142f, r, g, b, a);
        localPoint(detail, matrix, ang, x, y, z, 0f, 0.02f, r, g, b, a);
        localPoint(detail, matrix, ang, x, y, z, -0.075f, 0.015f, r, g, b, a * 0.8f);
        localPoint(detail, matrix, ang, x, y, z,  0.075f, 0.015f, r, g, b, a * 0.8f);
        localPoint(detail, matrix, ang, x, y, z, 0f, 0.145f, r, g, b, a * 0.7f);
        localPoint(detail, matrix, ang, x, y, z, 0f, 0.225f, r, g, b, a * 0.55f);
        BufferUploader.drawWithShader(detail.end());
    }

    private static void shards(Matrix4f matrix, float radius, int count, float r, float g, float b, float a, float time) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        count = Math.max(3, count);
        for (int i = 0; i < count; i++) {
            double ang = time * 1.18 + i * (Math.PI * 2 / count);
            float x = (float) Math.cos(ang) * radius;
            float z = (float) Math.sin(ang) * radius;
            float y = 0.22f + (i % 6) * 0.30f;
            localPoint(bb, matrix, ang, x, y, z, 0f, 0.13f, r, g, b, a);
            localPoint(bb, matrix, ang, x, y, z, -0.065f, -0.10f, r * 0.62f, g * 0.62f, b * 0.62f, a * 0.70f);
            localPoint(bb, matrix, ang, x, y, z, 0.070f, -0.055f, r, g, b, a * 0.92f);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void stars(Matrix4f matrix, float radius, int count, float r, float g, float b, float a, float time) {
        count = Math.max(3, count);
        for (int i = 0; i < count; i++) {
            double ang = time * 0.72 + i * (Math.PI * 2 / count);
            float x = (float) Math.cos(ang) * radius;
            float z = (float) Math.sin(ang) * radius;
            float y = 0.28f + (i % 5) * 0.36f;
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int p = 0; p <= 10; p++) {
                double sa = -Math.PI / 2 + p * Math.PI / 5.0;
                float rr = p % 2 == 0 ? 0.11f : 0.045f;
                localPoint(bb, matrix, ang, x, y, z,
                        (float) Math.cos(sa) * rr, (float) Math.sin(sa) * rr,
                        r, g, b, a);
            }
            BufferUploader.drawWithShader(bb.end());
        }
    }

    private static void runes(Matrix4f matrix, float radius, int count, float r, float g, float b, float a, float time) {
        count = Math.max(3, count);
        for (int i = 0; i < count; i++) {
            double ang = -time * 0.64 + i * (Math.PI * 2 / count);
            float x = (float) Math.cos(ang) * radius;
            float z = (float) Math.sin(ang) * radius;
            float y = 0.23f + (i % 4) * 0.46f;
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            float s = 0.105f;
            localPoint(bb, matrix, ang, x, y, z, 0, s, r, g, b, a);
            localPoint(bb, matrix, ang, x, y, z, s, 0, r, g, b, a);
            localPoint(bb, matrix, ang, x, y, z, 0, -s, r, g, b, a);
            localPoint(bb, matrix, ang, x, y, z, -s, 0, r, g, b, a);
            localPoint(bb, matrix, ang, x, y, z, 0, s, r, g, b, a);
            localPoint(bb, matrix, ang, x, y, z, 0, s * 0.48f, r, g, b, a * 0.72f);
            localPoint(bb, matrix, ang, x, y, z, s * 0.48f, 0, r, g, b, a * 0.72f);
            localPoint(bb, matrix, ang, x, y, z, 0, -s * 0.48f, r, g, b, a * 0.72f);
            localPoint(bb, matrix, ang, x, y, z, -s * 0.48f, 0, r, g, b, a * 0.72f);
            localPoint(bb, matrix, ang, x, y, z, 0, s * 0.48f, r, g, b, a * 0.72f);
            BufferUploader.drawWithShader(bb.end());
        }
    }

    private static void billboardQuad(BufferBuilder bb, Matrix4f matrix, double ang,
                                      float x, float y, float z, float halfW, float halfH,
                                      float r, float g, float b, float a) {
        localPoint(bb, matrix, ang, x, y, z, -halfW, -halfH, r, g, b, a);
        localPoint(bb, matrix, ang, x, y, z,  halfW, -halfH, r, g, b, a);
        localPoint(bb, matrix, ang, x, y, z,  halfW,  halfH, r, g, b, a);
        localPoint(bb, matrix, ang, x, y, z, -halfW,  halfH, r, g, b, a);
    }

    private static void localPoint(BufferBuilder bb, Matrix4f matrix, double ang,
                                   float x, float y, float z, float u, float v,
                                   float r, float g, float b, float a) {
        float tx = (float) -Math.sin(ang);
        float tz = (float) Math.cos(ang);
        bb.vertex(matrix, x + tx * u, y + v, z + tz * u).color(r, g, b, a).endVertex();
    }

    private static double lerp(double a, double b, float t) { return a + (b - a) * t; }
}
