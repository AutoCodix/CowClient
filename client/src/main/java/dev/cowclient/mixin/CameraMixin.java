package dev.cowclient.mixin;

import dev.cowclient.CowClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameRenderer.class)
public abstract class CameraMixin {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void cowclient$zoom(Camera camera,float delta,boolean changingFov,CallbackInfoReturnable<Float> ci) {
        if(changingFov) ci.setReturnValue(ci.getReturnValue()*CowClient.zoomMultiplier());
    }
    @Inject(method="bobView",at=@At("HEAD"),cancellable=true)
    private void cowclient$walking(PoseStack pose,float delta,CallbackInfo ci) {
        if(CowClient.active("steady")) ci.cancel();
    }
    @Inject(method="bobHurt",at=@At("HEAD"),cancellable=true)
    private void cowclient$hurt(PoseStack pose,float delta,CallbackInfo ci) {
        if(CowClient.active("hurt")) ci.cancel();
    }
}
