package dev.cowclient;

import dev.cowclient.core.*;
import dev.cowclient.ui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;

public final class CowClient implements ClientModInitializer {
    public static Config config;
    public static final ClickCounter clicks=new ClickCounter();
    public static KeyMapping menu,zoom,sprint;
    public static boolean sprintLatched;
    public static long worldStarted;
    private static Object lastWorld;
    private static boolean heldSprint;
    private static float zoomScale=1;
    private static long lastZoomTime=System.nanoTime();
    @Override public void onInitializeClient() {
        config=new Config(FabricLoader.getInstance().getConfigDir().resolve("cowclient"));
        String profile="Default";
        try {
            var p=FabricLoader.getInstance().getGameDir().resolve("cowclient-profile.txt");
            if(Files.exists(p)) { var s=Files.readString(p).strip(); if(Config.PROFILES.contains(s)) profile=s; }
        } catch(Exception ignored) {}
        config.load(profile);
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("cowclient","main"));
        menu=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cowclient.menu",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,category));
        zoom=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cowclient.zoom",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_C,category));
        sprint=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cowclient.sprint",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,category));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(lastWorld!=client.level) {lastWorld=client.level;worldStarted=System.currentTimeMillis();sprintLatched=false;}
            while(menu.consumeClick()) if(client.screen==null) client.setScreen(new CowScreen());
            while(sprint.consumeClick()) if(client.screen==null && active("sprint")) {
                sprintLatched=!sprintLatched;
                if(client.player!=null) client.player.displayClientMessage(Component.literal("CowClient · Sprint "+(sprintLatched?"enabled":"disabled")),true);
            }
            boolean force=active("sprint") && sprintLatched && client.screen==null && client.player!=null;
            if(force) client.options.keySprint.setDown(true);
            else if(heldSprint) client.options.keySprint.setDown(false);
            heldSprint=force;
        });
        SmokeTest.install();
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("cowclient","hud"),(g,d)->Hud.render(g));
    }
    public static boolean active(String id) { return config!=null && config.enabled(id); }
    public static float zoomMultiplier() {
        long now=System.nanoTime();double dt=Math.min(.05,(now-lastZoomTime)/1e9);lastZoomTime=now;
        float target=active("zoom") && zoom!=null && zoom.isDown() && Minecraft.getInstance().screen==null ? (float)(1/config.data.zoomFactor):1;
        zoomScale=active("motion")?target:(float)(zoomScale+(target-zoomScale)*(1-Math.exp(-18*dt)));
        return zoomScale;
    }
}
