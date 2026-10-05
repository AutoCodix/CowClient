package dev.spatialclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.spatialclient.SpatialClient;
import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    private ClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = SpatialClient.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        public static final KeyMapping OPEN_MENU = new KeyMapping(
                "key.spatialclient.menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.spatialclient"
        );

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_MENU);
        }
    }

    @Mod.EventBusSubscriber(modid = SpatialClient.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void screenOpening(ScreenEvent.Opening event) {
            if (!(event.getNewScreen() instanceof TitleScreen)) return;
            SpatialLocalAccount.init();
            if (SpatialLocalAccount.isUnlocked()) {
                event.setNewScreen(new SpatialTitleScreen());
            } else {
                event.setNewScreen(new SpatialAccountScreen(new SpatialTitleScreen(), false));
            }
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            InputStats.tick(mc);
            SpatialLocalAccount.init();
            if (mc.player == null || mc.level == null) {
                if (SpatialCamera.isAnySpatialCameraActive()) SpatialCamera.forceRestore();
                return;
            }

            if (SpatialCamera.isAnySpatialCameraActive()) {
                while (ModBus.OPEN_MENU.consumeClick()) {
                }
                return;
            }

            if (mc.screen == null && ModBus.OPEN_MENU.consumeClick()) {
                SpatialConfig.get();
                if (!SpatialLocalAccount.isUnlocked()) {
                    mc.setScreen(new SpatialAccountScreen());
                } else {
                    SpatialCamera.open();
                    if (SpatialCamera.isMenuCameraActive()) mc.setScreen(new SpatialMenuScreen());
                }
            }
        }

        @SubscribeEvent
        public static void renderTick(TickEvent.RenderTickEvent event) {
            SpatialCamera.onRenderTick(event);
            HeadTracking.onRenderEnd(event);
        }

        @SubscribeEvent
        public static void renderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
            HeadTracking.onPlayerPre(event);
        }

        @SubscribeEvent
        public static void renderPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {
            HeadTracking.onPlayerPost(event);
        }

        @SubscribeEvent
        public static void renderLevel(net.minecraftforge.client.event.RenderLevelStageEvent event) {
            AuraRenderer.onRenderLevel(event);
        }

        @SubscribeEvent
        public static void renderHand(net.minecraftforge.client.event.RenderHandEvent event) {
            if (SpatialCamera.isAnySpatialCameraActive()) event.setCanceled(true);
        }

        @SubscribeEvent
        public static void overlayPost(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event) {
            HudRenderer.onOverlayPost(event);
        }

        @SubscribeEvent
        public static void overlayPre(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
            HudRenderer.onOverlayPre(event);
        }
    }
}
