package dev.cowclient.mixin;

import dev.cowclient.CowClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Inject(method="onButton",at=@At("HEAD"))
    private void cowclient$count(long window,MouseButtonInfo input,int action,CallbackInfo ci) {
        var mc=Minecraft.getInstance();
        if(action==GLFW.GLFW_PRESS && window==mc.getWindow().handle() && mc.screen==null && mc.player!=null)
            CowClient.clicks.click(input.button(),System.currentTimeMillis());
    }
}
