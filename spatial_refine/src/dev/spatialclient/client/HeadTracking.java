package dev.spatialclient.client;

import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;

public final class HeadTracking {
    private static float mouseXNorm;
    private static float mouseYNorm;
    private static float smoothYawOffset;
    private static float smoothPitch;
    private static boolean saved;
    private static float oldHead;
    private static float oldHeadO;
    private static float oldPitch;
    private static float oldPitchO;
    private static float oldBody;
    private static float oldBodyO;

    private HeadTracking() {}

    public static void updateMouse(double mouseX, double mouseY, int width, int height) {
        if (width <= 0 || height <= 0) return;
        mouseXNorm = Mth.clamp((float)((mouseX / width) * 2.0 - 1.0), -1.0F, 1.0F);
        mouseYNorm = Mth.clamp((float)((mouseY / height) * 2.0 - 1.0), -1.0F, 1.0F);
        SpatialConfig cfg = SpatialConfig.get();
        smoothYawOffset = Mth.lerp(cfg.headFollowSpeed, smoothYawOffset, -mouseXNorm * cfg.headFollowYaw);
        smoothPitch = Mth.lerp(cfg.headFollowSpeed, smoothPitch,
                Mth.clamp(mouseYNorm * cfg.headFollowPitch, -cfg.headFollowPitch, cfg.headFollowPitch));
    }

    public static void onPlayerPre(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || event.getEntity() != player || !SpatialCamera.isMenuCameraActive()) return;

        SpatialConfig cfg = SpatialConfig.get();
        if (!cfg.headFollow && !cfg.bodyFacesCamera) return;

        restore();
        saved = true;
        oldHead = player.yHeadRot;
        oldHeadO = player.yHeadRotO;
        oldPitch = player.getXRot();
        oldPitchO = player.xRotO;
        oldBody = player.yBodyRot;
        oldBodyO = player.yBodyRotO;

        float body = cfg.bodyFacesCamera ? SpatialCamera.presentationBodyYaw() : player.yBodyRot;
        if (cfg.bodyFacesCamera) {
            player.yBodyRot = body;
            player.yBodyRotO = body;
        }

        if (cfg.headFollow) {
            float head = body + smoothYawOffset;
            player.yHeadRot = head;
            player.yHeadRotO = head;
            player.setXRot(smoothPitch);
            player.xRotO = smoothPitch;
        }
    }

    public static void onPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity() == Minecraft.getInstance().player) restore();
    }

    public static void onRenderEnd(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) restore();
    }

    private static void restore() {
        if (!saved) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.yHeadRot = oldHead;
            player.yHeadRotO = oldHeadO;
            player.setXRot(oldPitch);
            player.xRotO = oldPitchO;
            player.yBodyRot = oldBody;
            player.yBodyRotO = oldBodyO;
        }
        saved = false;
    }
}
