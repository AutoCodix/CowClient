package dev.spatial.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.spatial.client.camera.SpatialCameraController;
import dev.spatial.client.config.ClientConfig;
import dev.spatial.client.render.AuraRenderer;
import dev.spatial.client.render.HudRenderer;
import dev.spatial.client.render.PlayerLookController;
import dev.spatial.client.ui.SpatialScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod(SpatialClient.MOD_ID)
public final class SpatialClient {
    public static final String MOD_ID = "spatialclient";
    public static KeyMapping OPEN_MENU;

    public SpatialClient() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeys);
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        MinecraftForge.EVENT_BUS.register(AuraRenderer.class);
        MinecraftForge.EVENT_BUS.register(HudRenderer.class);
        MinecraftForge.EVENT_BUS.register(PlayerLookController.class);
        MinecraftForge.EVENT_BUS.register(SmokeTestController.class);
        ClientConfig.load();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(SpatialCameraController::reset);
    }

    private void registerKeys(final RegisterKeyMappingsEvent event) {
        OPEN_MENU = new KeyMapping(
                "key.spatialclient.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.spatialclient"
        );
        event.register(OPEN_MENU);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() { }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            PlayerLookController.restoreIfNeeded();

            if (SpatialCameraController.isActive()) {
                if (mc.player == null || mc.level == null) {
                    SpatialCameraController.forceClose();
                } else if (!(mc.screen instanceof SpatialScreen)) {
                    SpatialCameraController.forceClose();
                }
            }

            if (OPEN_MENU == null || Boolean.getBoolean("spatialclient.smokeTest")) return;
            while (OPEN_MENU.consumeClick()) {
                if (mc.player == null || mc.level == null) return;
                if (mc.screen instanceof SpatialScreen screen) {
                    screen.beginClose();
                } else if (mc.screen == null) {
                    SpatialCameraController.beginOpen();
                    mc.setScreen(new SpatialScreen());
                }
            }
        }
    }
}
