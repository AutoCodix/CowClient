package dev.spatialclient.client;

import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public final class SpatialCamera {
    private static RemotePlayer cameraEntity;
    private static Entity previousCameraEntity;
    private static CameraType previousCameraType;
    private static Vec3 currentEye;
    private static float currentYaw;
    private static float currentPitch;
    private static boolean active;
    private static boolean closing;
    private static long lastNanos;
    private static SpatialSection section = SpatialSection.VISUALS;
    private static float mouseXNorm;
    private static float mouseYNorm;

    private SpatialCamera() {}

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || active) return;

        previousCameraEntity = mc.getCameraEntity();
        previousCameraType = mc.options.getCameraType();
        cameraEntity = new RemotePlayer(mc.level, player.getGameProfile());

        float partial = mc.getFrameTime();
        Vec3 playerEye = interpolatedEye(player, partial);
        float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        Target target = menuTarget(player, partial, bodyYaw, SpatialSection.VISUALS);
        currentEye = playerEye.lerp(target.eye, 0.72);
        float[] initialLook = lookAt(currentEye, target.lookAt);
        currentYaw = initialLook[0];
        currentPitch = initialLook[1];
        syncCameraEntity();

        active = true;
        closing = false;
        section = SpatialSection.VISUALS;
        lastNanos = System.nanoTime();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(cameraEntity);
    }

    public static void beginClose() {
        if (!active) return;
        closing = true;
        lastNanos = System.nanoTime();
    }

    public static void forceRestore() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.setCameraEntity(previousCameraEntity != null ? previousCameraEntity : mc.player);
        if (previousCameraType != null) mc.options.setCameraType(previousCameraType);
        active = false;
        closing = false;
        cameraEntity = null;
        previousCameraEntity = null;
        previousCameraType = null;
        currentEye = null;
        lastNanos = 0L;
    }

    public static boolean isMenuCameraActive() {
        return active && !closing;
    }

    public static boolean isAnySpatialCameraActive() {
        return active;
    }

    public static void setSection(SpatialSection newSection) {
        if (newSection != null && newSection != SpatialSection.HOME) section = newSection;
    }

    public static void setMouse(double mouseX, double mouseY, int width, int height) {
        if (width <= 0 || height <= 0) return;
        mouseXNorm = Mth.clamp((float)((mouseX / width) * 2.0 - 1.0), -1.0F, 1.0F);
        mouseYNorm = Mth.clamp((float)((mouseY / height) * 2.0 - 1.0), -1.0F, 1.0F);
    }

    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || cameraEntity == null) {
            forceRestore();
            return;
        }

        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 1.0F / 60.0F : Mth.clamp((now - lastNanos) / 1_000_000_000.0F, 0.001F, 0.033F);
        lastNanos = now;
        float partial = event.renderTickTime;

        Vec3 targetEye;
        Vec3 targetLook;
        float targetYaw;
        float targetPitch;

        if (closing) {
            targetEye = interpolatedEye(player, partial);
            targetYaw = Mth.rotLerp(partial, player.yRotO, player.getYRot());
            targetPitch = Mth.lerp(partial, player.xRotO, player.getXRot());
            targetLook = null;
        } else {
            float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
            Target target = menuTarget(player, partial, bodyYaw, section);
            targetEye = target.eye;
            targetLook = target.lookAt;

            if (SpatialConfig.get().cameraCollision) {
                Vec3 focus = interpolatedBody(player, partial).add(0.0, 1.28, 0.0);
                BlockHitResult hit = mc.level.clip(new ClipContext(focus, targetEye, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
                if (hit.getType() != HitResult.Type.MISS) {
                    Vec3 hitPos = hit.getLocation();
                    Vec3 towardFocus = focus.subtract(hitPos);
                    if (towardFocus.lengthSqr() > 1.0E-6) hitPos = hitPos.add(towardFocus.normalize().scale(0.20));
                    targetEye = hitPos;
                }
            }

            float[] look = lookAt(targetEye, targetLook);
            targetYaw = look[0];
            targetPitch = look[1];
        }

        float baseRate = closing ? 22.0F : 10.5F + SpatialConfig.get().cameraSmoothness * 18.0F;
        float posAlpha = 1.0F - (float)Math.exp(-baseRate * dt);
        float rotAlpha = 1.0F - (float)Math.exp(-(baseRate + 4.0F) * dt);

        if (currentEye == null) currentEye = targetEye;
        currentEye = currentEye.lerp(targetEye, posAlpha);
        currentYaw = rotLerp(currentYaw, targetYaw, rotAlpha);
        currentPitch = Mth.lerp(rotAlpha, currentPitch, targetPitch);
        syncCameraEntity();

        if (closing) {
            double posError = currentEye.distanceTo(targetEye);
            float yawError = Math.abs(Mth.wrapDegrees(currentYaw - targetYaw));
            float pitchError = Math.abs(currentPitch - targetPitch);
            if (posError < 0.025 && yawError < 1.2F && pitchError < 1.2F) forceRestore();
        }
    }

    private static Target menuTarget(LocalPlayer player, float partial, float bodyYaw, SpatialSection section) {
        Vec3 base = interpolatedBody(player, partial);
        Vec3 focus = base.add(0.0, 1.28, 0.0);
        Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        double distance = section.distance;
        double side = section.sideOffset;
        double up = section.heightOffset;
        if (SpatialConfig.get().menuParallax) {
            side += mouseXNorm * 0.09;
            up += -mouseYNorm * 0.055;
        }

        Vec3 eye = focus.add(forward.scale(distance)).add(right.scale(side)).add(0.0, up, 0.0);
        Vec3 lookAt = focus.add(right.scale(section.lookOffset));
        return new Target(eye, lookAt);
    }

    private static Vec3 interpolatedBody(LocalPlayer player, float partial) {
        return new Vec3(
                Mth.lerp(partial, player.xOld, player.getX()),
                Mth.lerp(partial, player.yOld, player.getY()),
                Mth.lerp(partial, player.zOld, player.getZ())
        );
    }

    private static Vec3 interpolatedEye(LocalPlayer player, float partial) {
        Vec3 p = interpolatedBody(player, partial);
        return p.add(0.0, player.getEyeHeight(), 0.0);
    }

    private static void syncCameraEntity() {
        if (cameraEntity == null || currentEye == null) return;
        double feetY = currentEye.y - cameraEntity.getEyeHeight();
        cameraEntity.setPos(currentEye.x, feetY, currentEye.z);
        cameraEntity.xo = currentEye.x;
        cameraEntity.yo = feetY;
        cameraEntity.zo = currentEye.z;
        cameraEntity.xOld = currentEye.x;
        cameraEntity.yOld = feetY;
        cameraEntity.zOld = currentEye.z;
        cameraEntity.setYRot(currentYaw);
        cameraEntity.setXRot(currentPitch);
        cameraEntity.yRotO = currentYaw;
        cameraEntity.xRotO = currentPitch;
        cameraEntity.yHeadRot = currentYaw;
        cameraEntity.yHeadRotO = currentYaw;
        cameraEntity.yBodyRot = currentYaw;
        cameraEntity.yBodyRotO = currentYaw;
    }

    static float[] lookAt(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float)-Math.toDegrees(Math.atan2(d.y, horizontal));
        return new float[]{yaw, pitch};
    }

    static float rotLerp(float from, float to, float alpha) {
        return from + Mth.wrapDegrees(to - from) * alpha;
    }

    private record Target(Vec3 eye, Vec3 lookAt) {}
}
