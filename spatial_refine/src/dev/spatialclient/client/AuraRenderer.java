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
        if (player == null || !cfg.aura || mc.options.hideGui) return;

        boolean thirdPerson = !mc.options.getCameraType().isFirstPerson();
        if (!thirdPerson && !SpatialCamera.isAnySpatialCameraActive()) return;

        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        double px = Mth.lerp(partial, player.xOld, player.getX());
        double py = Mth.lerp(partial, player.yOld, player.getY());
        double pz = Mth.lerp(partial, player.zOld, player.getZ());
        float time = (player.tickCount + partial) * 0.105F;

        event.getPoseStack().pushPose();
        event.getPoseStack().translate(px - cam.x, py - cam.y, pz - cam.z);
        Matrix4f matrix = event.getPoseStack().last().pose();

        int rgb = cfg.accentRgb;
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        switch (cfg.auraStyle) {
            case ORBIT -> drawFlowingOrbit(matrix, r, g, b, time, cfg.auraIntensity);
            case HELIX -> drawHelix(matrix, r, g, b, time, cfg.auraIntensity);
            case HALO -> drawHaloFlow(matrix, r, g, b, time, cfg.auraIntensity);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        event.getPoseStack().popPose();
    }

    private static void drawFlowingOrbit(Matrix4f matrix, int r, int g, int b, float time, int intensity) {
        drawWavyLoop(matrix, r, g, b, time, 0.74F, 0.49F, 0.060F, 0.00F, 170, 84);
        drawWavyLoop(matrix, r, g, b, -time * 0.82F, 1.22F, 0.54F, 0.050F, (float)Math.PI, 130, 76);
        if (intensity >= 2) {
            drawWavyLoop(matrix, r, g, b, time * 1.13F, 1.63F, 0.46F, 0.040F, 1.35F, 105, 70);
        }
        if (intensity >= 3) {
            drawVerticalSparkRibbon(matrix, r, g, b, time * 0.78F, 0.0F, 105, 96, 2.0F, 0.47F);
        }
    }

    private static void drawHelix(Matrix4f matrix, int r, int g, int b, float time, int intensity) {
        drawVerticalSparkRibbon(matrix, r, g, b, time, 0.0F, 180, 112, 2.15F, 0.50F);
        drawVerticalSparkRibbon(matrix, r, g, b, -time * 0.88F, (float)Math.PI, 120, 108, 1.90F, 0.55F);
        if (intensity >= 2) {
            drawVerticalSparkRibbon(matrix, r, g, b, time * 1.28F, (float)Math.PI * 0.5F, 85, 92, 1.45F, 0.43F);
        }
    }

    private static void drawHaloFlow(Matrix4f matrix, int r, int g, int b, float time, int intensity) {
        drawWavyLoop(matrix, r, g, b, time * 1.25F, 1.92F, 0.43F, 0.052F, 0.0F, 180, 88);
        drawWavyLoop(matrix, r, g, b, -time, 0.18F, 0.60F, 0.045F, 0.7F, 95, 80);
        if (intensity >= 2) drawVerticalSparkRibbon(matrix, r, g, b, time * 0.64F, 1.0F, 90, 92, 1.35F, 0.48F);
        if (intensity >= 3) drawWavyLoop(matrix, r, g, b, time * 0.72F, 1.08F, 0.52F, 0.038F, 2.1F, 78, 72);
    }

    private static void drawWavyLoop(Matrix4f matrix, int r, int g, int b, float time,
                                     float centerY, float radius, float halfWidth, float phase,
                                     int alpha, int segments) {
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            double angle = t * Math.PI * 2.0 + time * 1.8 + phase;
            double wave = Math.sin(angle * 2.0 + time * 1.7) * 0.12 + Math.sin(angle * 3.0 - time) * 0.035;
            double rad = radius + Math.sin(angle * 1.5 + time) * 0.025;
            double y = centerY + wave;
            int localAlpha = Mth.clamp((int)(alpha * (0.70 + 0.30 * Math.sin(t * Math.PI))), 35, 220);
            double outer = rad + halfWidth;
            double inner = rad - halfWidth;
            vertex(buf, matrix, Math.cos(angle) * outer, y, Math.sin(angle) * outer, r, g, b, localAlpha);
            vertex(buf, matrix, Math.cos(angle) * inner, y, Math.sin(angle) * inner, r, g, b, Math.max(20, localAlpha - 45));
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void drawVerticalSparkRibbon(Matrix4f matrix, int r, int g, int b, float time, float phase,
                                                int alpha, int segments, float turns, float radius) {
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            double angle = t * Math.PI * 2.0 * turns + time * 2.15 + phase;
            double waveRadius = radius + Math.sin(t * Math.PI * 5.0 + time * 1.8) * 0.035;
            double y = 0.08 + t * 1.78 + Math.sin(angle * 0.55) * 0.055;
            double width = 0.045 + 0.018 * Math.sin(t * Math.PI);
            int localAlpha = Mth.clamp((int)(alpha * (0.50 + 0.50 * Math.sin(t * Math.PI))), 22, 230);
            double outer = waveRadius + width;
            double inner = waveRadius - width;
            vertex(buf, matrix, Math.cos(angle) * outer, y, Math.sin(angle) * outer, r, g, b, localAlpha);
            vertex(buf, matrix, Math.cos(angle) * inner, y, Math.sin(angle) * inner, r, g, b, Math.max(18, localAlpha - 60));
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void vertex(BufferBuilder buf, Matrix4f matrix, double x, double y, double z,
                               int r, int g, int b, int a) {
        buf.vertex(matrix, (float)x, (float)y, (float)z).color(r, g, b, a).endVertex();
    }
}
