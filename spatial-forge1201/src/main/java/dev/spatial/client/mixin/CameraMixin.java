package dev.spatial.client.mixin;

import dev.spatial.client.camera.SpatialCameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void spatialclient$applyCamera(BlockGetter level, Entity entity, boolean detached,
                                           boolean mirrored, float partialTick, CallbackInfo ci) {
        SpatialCameraController.CameraTransform transform = SpatialCameraController.sample(partialTick);
        if (transform == null) return;
        setPosition(transform.position().x, transform.position().y, transform.position().z);
        setRotation(transform.yaw(), transform.pitch());
    }
}
