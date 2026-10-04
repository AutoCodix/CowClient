package dev.spatial.client.render;

import dev.spatial.client.camera.SpatialCameraController;
import dev.spatial.client.ui.SpatialScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PlayerLookController {
    private static float oldHeadYaw;
    private static float oldHeadYawO;
    private static float oldPitch;
    private static float oldPitchO;
    private static AbstractClientPlayer changedPlayer;
    private static int smokeRenderCount;

    private PlayerLookController() { }

    @SubscribeEvent
    public static void onPre(RenderPlayerEvent.Pre event) {
        restoreIfNeeded();
        Minecraft mc = Minecraft.getInstance();
        if (!SpatialCameraController.isActive() || !(mc.screen instanceof SpatialScreen screen)) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || player != mc.player) return;

        oldHeadYaw = player.yHeadRot;
        oldHeadYawO = player.yHeadRotO;
        oldPitch = player.getXRot();
        oldPitchO = player.xRotO;

        float yawOffset = screen.normalizedMouseX() * 24f;
        float pitch = screen.normalizedMouseY() * 12f;
        player.yHeadRot = player.yBodyRot + yawOffset;
        player.yHeadRotO = player.yHeadRot;
        player.setXRot(pitch);
        player.xRotO = pitch;
        changedPlayer = player;
        if (Boolean.getBoolean("spatialclient.smokeTest")) {
            smokeRenderCount++;
        }
    }

    @SubscribeEvent
    public static void onPost(RenderPlayerEvent.Post event) {
        if (changedPlayer == null || event.getEntity() != changedPlayer) return;
        restoreIfNeeded();
    }

    public static int smokeRenderCount() {
        return smokeRenderCount;
    }

    public static void restoreIfNeeded() {
        if (changedPlayer == null) return;
        changedPlayer.yHeadRot = oldHeadYaw;
        changedPlayer.yHeadRotO = oldHeadYawO;
        changedPlayer.setXRot(oldPitch);
        changedPlayer.xRotO = oldPitchO;
        changedPlayer = null;
    }
}
