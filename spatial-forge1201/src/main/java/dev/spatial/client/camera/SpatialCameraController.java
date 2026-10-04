package dev.spatial.client.camera;

import dev.spatial.client.config.ClientConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class SpatialCameraController {
    private static boolean active;
    private static boolean closing;
    private static boolean closeFinished;
    private static float transition;
    private static long lastNanos;
    private static float cachedDt = 1f / 60f;

    private static PoseSpec fromPose = CameraPreset.HOME.pose;
    private static PoseSpec toPose = CameraPreset.HOME.pose;
    private static float presetBlend = 1f;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static boolean cameraTypeCaptured;

    private SpatialCameraController() { }

    public static void reset() {
        restoreCameraType();
        active = false;
        closing = false;
        closeFinished = false;
        transition = 0f;
        fromPose = CameraPreset.HOME.pose;
        toPose = CameraPreset.HOME.pose;
        presetBlend = 1f;
        lastNanos = System.nanoTime();
    }

    public static void beginOpen() {
        Minecraft mc = Minecraft.getInstance();
        if (!cameraTypeCaptured) {
            previousCameraType = mc.options.getCameraType();
            cameraTypeCaptured = true;
        }
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);

        active = true;
        closing = false;
        closeFinished = false;
        transition = 0f;
        fromPose = CameraPreset.HOME.pose;
        toPose = CameraPreset.HOME.pose;
        presetBlend = 1f;
        lastNanos = System.nanoTime();
    }

    public static void beginClose() {
        if (!active) {
            restoreCameraType();
            closeFinished = true;
            return;
        }
        closing = true;
    }

    public static void forceClose() {
        active = false;
        closing = false;
        transition = 0f;
        closeFinished = true;
        restoreCameraType();
    }

    public static boolean isActive() { return active; }
    public static boolean isClosing() { return closing; }

    public static boolean consumeCloseFinished() {
        if (!closeFinished) return false;
        closeFinished = false;
        return true;
    }

    public static void setPreset(CameraPreset preset) {
        if (preset == null) return;
        PoseSpec desired = preset.pose;
        if (desired.equals(toPose) && presetBlend >= 0.999f) return;
        fromPose = currentPose();
        toPose = desired;
        presetBlend = 0f;
    }

    public static CameraTransform sample(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!active || player == null || mc.level == null) return null;

        updateTime();
        float desiredTransition = closing ? 0f : 1f;
        float smooth = ClientConfig.get().cameraSmoothness;
        float frameStep = Math.min(1f, frameSeconds() / Math.max(0.03f, smooth));
        transition = lerp(transition, desiredTransition, easeOut(frameStep));
        presetBlend = Math.min(1f, presetBlend + frameSeconds() * 4.8f);

        if (closing && transition <= 0.015f) {
            active = false;
            closing = false;
            transition = 0f;
            closeFinished = true;
            restoreCameraType();
            return null;
        }

        PoseSpec pose = currentPose();
        float bodyYaw = lerpDegrees(player.yBodyRotO, player.yBodyRot, partialTick);
        double yawRad = Math.toRadians(bodyYaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(forward.z, 0, -forward.x);

        Vec3 base = player.getPosition(partialTick).add(0, 0.1, 0);
        Vec3 desiredPos = base
                .add(forward.scale(pose.front))
                .add(right.scale(pose.side))
                .add(0, pose.up, 0);
        Vec3 desiredTarget = base.add(0, pose.targetUp, 0);

        Vec3 vanillaPos = player.getEyePosition(partialTick);
        Vec3 vanillaTarget = vanillaPos.add(player.getViewVector(partialTick).scale(4.0));
        Vec3 position = vanillaPos.lerp(desiredPos, transition);
        Vec3 target = vanillaTarget.lerp(desiredTarget, transition);

        return CameraTransform.lookAt(position, target);
    }

    private static PoseSpec currentPose() {
        float t = easeInOut(presetBlend);
        return PoseSpec.lerp(fromPose, toPose, t);
    }

    private static void restoreCameraType() {
        if (!cameraTypeCaptured) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.setCameraType(previousCameraType);
        cameraTypeCaptured = false;
    }

    private static void updateTime() {
        long now = System.nanoTime();
        if (lastNanos == 0L) lastNanos = now;
        cachedDt = (float) Math.min(0.05, Math.max(0.001, (now - lastNanos) / 1_000_000_000.0));
        lastNanos = now;
    }

    private static float frameSeconds() { return cachedDt; }
    private static float easeOut(float t) { return 1f - (1f - t) * (1f - t); }
    private static float easeInOut(float t) { return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f; }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static double lerp(double a, double b, float t) { return a + (b - a) * t; }

    private static float lerpDegrees(float a, float b, float t) {
        float delta = ((b - a + 540f) % 360f) - 180f;
        return a + delta * t;
    }

    public enum CameraPreset {
        HOME(new PoseSpec(-1.15, 3.35, 1.72, 1.52)),
        VISUALS(new PoseSpec(-1.45, 4.15, 1.52, 1.28)),
        HUD(new PoseSpec(1.65, 3.65, 1.92, 1.55)),
        COSMETICS(new PoseSpec(0.85, 3.15, 1.55, 1.38)),
        CAMERA(new PoseSpec(-0.40, 2.55, 2.05, 1.63)),
        SETTINGS(new PoseSpec(1.05, 2.80, 1.78, 1.56));

        final PoseSpec pose;
        CameraPreset(PoseSpec pose) { this.pose = pose; }
    }

    private record PoseSpec(double side, double front, double up, double targetUp) {
        static PoseSpec lerp(PoseSpec a, PoseSpec b, float t) {
            return new PoseSpec(
                    SpatialCameraController.lerp(a.side, b.side, t),
                    SpatialCameraController.lerp(a.front, b.front, t),
                    SpatialCameraController.lerp(a.up, b.up, t),
                    SpatialCameraController.lerp(a.targetUp, b.targetUp, t)
            );
        }
    }

    public record CameraTransform(Vec3 position, float yaw, float pitch) {
        static CameraTransform lookAt(Vec3 position, Vec3 target) {
            Vec3 d = target.subtract(position);
            double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
            float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, horizontal)));
            return new CameraTransform(position, yaw, pitch);
        }
    }
}
