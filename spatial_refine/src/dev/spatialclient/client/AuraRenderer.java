package dev.spatialclient.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class AuraRenderer {
    private AuraRenderer() {}

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        SpatialConfig cfg = SpatialConfig.get();
        if (player == null || mc.options.hideGui || (!cfg.aura && !cfg.halo)) return;

        boolean thirdPerson = !mc.options.getCameraType().isFirstPerson();
        if (!thirdPerson && !SpatialCamera.isAnySpatialCameraActive()) return;

        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        double px = Mth.lerp(partial, player.xOld, player.getX());
        double py = Mth.lerp(partial, player.yOld, player.getY());
        double pz = Mth.lerp(partial, player.zOld, player.getZ());

        event.getPoseStack().pushPose();
        event.getPoseStack().translate(px - cam.x, py - cam.y, pz - cam.z);
        Matrix4f matrix = event.getPoseStack().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        if (cfg.aura) drawAura(matrix, cfg, player.tickCount + partial);
        if (cfg.halo) drawHalo(matrix, cfg, player.tickCount + partial);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        event.getPoseStack().popPose();
    }

    private static void drawAura(Matrix4f matrix, SpatialConfig cfg, float ticks) {
        int rgb = cfg.auraUseAccent ? cfg.accentRgb : cfg.auraColorRgb;
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        float time = ticks * 0.105F * cfg.auraSpeed;
        int alpha = Mth.clamp((int)(255.0F * cfg.auraOpacity), 20, 255);
        float radius = cfg.auraRadius;
        float width = cfg.auraWidth;
        float height = cfg.auraHeight;
        float wave = cfg.auraWave;

        switch (cfg.auraStyle) {
            case ORBIT -> {
                int layers = cfg.auraIntensity;
                for (int i = 0; i < layers; i++) {
                    float f = layers == 1 ? 0.5F : i / (float)(layers - 1);
                    float y = 0.28F + f * Math.max(0.3F, height - 0.55F);
                    float localRadius = radius * (0.96F + (float)Math.sin(i * 1.8) * 0.07F);
                    float direction = (i & 1) == 0 ? 1.0F : -0.78F;
                    drawWavyOrbit(matrix, r, g, b, time * direction + i * 1.45F, y,
                            localRadius, width, wave, alpha - i * 20, 96);
                }
            }
            case HELIX -> {
                int strands = Math.min(4, cfg.auraIntensity + 1);
                for (int i = 0; i < strands; i++) {
                    float phase = (float)(Math.PI * 2.0 * i / strands);
                    float dir = (i & 1) == 0 ? 1.0F : -1.0F;
                    drawHelixRibbon(matrix, r, g, b, time * dir, phase, alpha - i * 16,
                            120, 2.0F + cfg.auraIntensity * 0.28F, radius, height, width, wave);
                }
            }
            case RIBBON -> {
                drawDiagonalRibbon(matrix, r, g, b, time, 0.0F, alpha, 126, radius, height, width, wave);
                if (cfg.auraIntensity >= 2) drawDiagonalRibbon(matrix, r, g, b, -time * 0.82F,
                        (float)Math.PI, alpha - 35, 126, radius * 1.05F, height, width * 0.85F, wave * 0.85F);
                if (cfg.auraIntensity >= 3) drawWavyOrbit(matrix, r, g, b, time * 1.35F,
                        height * 0.56F, radius * 1.08F, width * 0.75F, wave * 0.55F, alpha - 60, 96);
                if (cfg.auraIntensity >= 4) drawWavyOrbit(matrix, r, g, b, -time * 1.1F,
                        0.22F, radius * 1.15F, width * 0.60F, wave * 0.45F, alpha - 80, 96);
            }
        }
    }

    private static void drawHalo(Matrix4f matrix, SpatialConfig cfg, float ticks) {
        int rgb = cfg.haloUseAccent ? cfg.accentRgb : cfg.haloColorRgb;
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        float time = ticks * 0.08F * cfg.haloSpeed;
        int alpha = Mth.clamp((int)(255.0F * cfg.haloOpacity), 20, 255);

        switch (cfg.haloStyle) {
            case CLEAN_RING -> drawTiltedRing(matrix, r, g, b, time, cfg.haloHeight,
                    cfg.haloRadius, cfg.haloWidth, cfg.haloTilt, alpha, 108);
            case DOUBLE_RING -> {
                drawTiltedRing(matrix, r, g, b, time, cfg.haloHeight,
                        cfg.haloRadius, cfg.haloWidth, cfg.haloTilt, alpha, 108);
                drawTiltedRing(matrix, r, g, b, -time * 0.74F + 1.2F, cfg.haloHeight + 0.07F,
                        cfg.haloRadius * 0.82F, cfg.haloWidth * 0.70F, -cfg.haloTilt * 0.70F,
                        Math.max(25, alpha - 55), 96);
            }
            case ORBITAL -> {
                drawTiltedRing(matrix, r, g, b, time, cfg.haloHeight,
                        cfg.haloRadius, cfg.haloWidth, cfg.haloTilt, alpha, 108);
                drawTiltedRing(matrix, r, g, b, -time * 1.15F + 0.8F, cfg.haloHeight,
                        cfg.haloRadius * 0.93F, cfg.haloWidth * 0.62F, cfg.haloTilt + 62.0F,
                        Math.max(25, alpha - 45), 108);
            }
        }
    }

    private static void drawWavyOrbit(Matrix4f matrix, int r, int g, int b, float time,
                                      float centerY, float radius, float halfWidth, float wave,
                                      int alpha, int segments) {
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            double angle = t * Math.PI * 2.0 + time * 1.75;
            double y = centerY + Math.sin(angle * 2.0 + time * 1.4) * wave
                    + Math.sin(angle * 3.0 - time * 0.7) * wave * 0.28;
            double rad = radius + Math.sin(angle * 1.5 + time) * wave * 0.20;
            int localAlpha = Mth.clamp((int)(alpha * (0.72 + 0.28 * Math.sin(t * Math.PI))), 18, 255);
            vertex(buf, matrix, Math.cos(angle) * (rad + halfWidth), y, Math.sin(angle) * (rad + halfWidth), r, g, b, localAlpha);
            vertex(buf, matrix, Math.cos(angle) * (rad - halfWidth), y, Math.sin(angle) * (rad - halfWidth), r, g, b, Math.max(12, localAlpha - 60));
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void drawHelixRibbon(Matrix4f matrix, int r, int g, int b, float time, float phase,
                                        int alpha, int segments, float turns, float radius, float height,
                                        float width, float wave) {
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            double angle = t * Math.PI * 2.0 * turns + time * 2.0 + phase;
            double rad = radius + Math.sin(t * Math.PI * 6.0 + time) * wave * 0.18;
            double y = 0.08 + t * height + Math.sin(angle * 0.50) * wave * 0.35;
            double localWidth = width * (0.82 + 0.35 * Math.sin(t * Math.PI));
            int localAlpha = Mth.clamp((int)(alpha * (0.46 + 0.54 * Math.sin(t * Math.PI))), 14, 255);
            vertex(buf, matrix, Math.cos(angle) * (rad + localWidth), y, Math.sin(angle) * (rad + localWidth), r, g, b, localAlpha);
            vertex(buf, matrix, Math.cos(angle) * (rad - localWidth), y, Math.sin(angle) * (rad - localWidth), r, g, b, Math.max(10, localAlpha - 65));
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void drawDiagonalRibbon(Matrix4f matrix, int r, int g, int b, float time, float phase,
                                           int alpha, int segments, float radius, float height,
                                           float width, float wave) {
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            double angle = t * Math.PI * 4.0 + time * 1.65 + phase;
            double y = 0.12 + t * height + Math.sin(angle * 1.25 + time) * wave * 0.60;
            double rad = radius + Math.sin(angle * 0.75 - time) * wave * 0.14;
            double localWidth = width * (0.72 + 0.38 * Math.sin(t * Math.PI));
            int localAlpha = Mth.clamp((int)(alpha * (0.42 + 0.58 * Math.sin(t * Math.PI))), 14, 255);
            vertex(buf, matrix, Math.cos(angle) * (rad + localWidth), y, Math.sin(angle) * (rad + localWidth), r, g, b, localAlpha);
            vertex(buf, matrix, Math.cos(angle) * (rad - localWidth), y, Math.sin(angle) * (rad - localWidth), r, g, b, Math.max(10, localAlpha - 60));
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void drawTiltedRing(Matrix4f matrix, int r, int g, int b, float time,
                                       float centerY, float radius, float halfWidth, float tiltDeg,
                                       int alpha, int segments) {
        double tilt = Math.toRadians(tiltDeg);
        double sinTilt = Math.sin(tilt);
        double cosTilt = Math.cos(tilt);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            double angle = t * Math.PI * 2.0 + time * 1.8;
            int localAlpha = Mth.clamp((int)(alpha * (0.78 + 0.22 * Math.sin(t * Math.PI))), 18, 255);
            ringVertex(buf, matrix, angle, radius + halfWidth, centerY, sinTilt, cosTilt, r, g, b, localAlpha);
            ringVertex(buf, matrix, angle, radius - halfWidth, centerY, sinTilt, cosTilt, r, g, b, Math.max(12, localAlpha - 48));
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void ringVertex(BufferBuilder buf, Matrix4f matrix, double angle, double radius,
                                   double centerY, double sinTilt, double cosTilt,
                                   int r, int g, int b, int a) {
        double x = Math.cos(angle) * radius;
        double plane = Math.sin(angle) * radius;
        double y = centerY + plane * sinTilt;
        double z = plane * cosTilt;
        vertex(buf, matrix, x, y, z, r, g, b, a);
    }

    private static void vertex(BufferBuilder buf, Matrix4f matrix, double x, double y, double z,
                               int r, int g, int b, int a) {
        buf.vertex(matrix, (float)x, (float)y, (float)z).color(r, g, b, a).endVertex();
    }
}
