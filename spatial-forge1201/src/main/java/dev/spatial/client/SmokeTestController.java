package dev.spatial.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.spatial.client.ui.SpatialScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class SmokeTestController {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int ticks;
    private static boolean opened;
    private static boolean screenshotRequested;
    private static boolean finished;

    private SmokeTestController() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("spatialclient.smokeTest") || event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        if (!opened) {
            if (!(mc.screen instanceof TitleScreen)) return;
            opened = true;
            ticks = 0;
            mc.setScreen(new SpatialScreen(true));
            LOGGER.info("SPATIALCLIENT_SMOKE_UI_OPEN");
            return;
        }

        ticks++;
        if (!screenshotRequested && ticks >= 30) {
            screenshotRequested = true;
            try {
                NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                image.writeToFile(mc.gameDirectory.toPath().resolve("spatialclient-smoke.png"));
                image.close();
                LOGGER.info("SPATIALCLIENT_SMOKE_SCREENSHOT_OK");
            } catch (Exception ex) {
                LOGGER.error("SPATIALCLIENT_SMOKE_SCREENSHOT_FAIL", ex);
                finish(mc, false);
                return;
            }
        }

        if (screenshotRequested && ticks >= 35) {
            finish(mc, true);
        } else if (ticks > 200) {
            finish(mc, false);
        }
    }

    private static void finish(Minecraft mc, boolean ok) {
        if (finished) return;
        finished = true;
        if (ok) LOGGER.info("SPATIALCLIENT_SMOKE_OK");
        else LOGGER.error("SPATIALCLIENT_SMOKE_FAILED");
        mc.stop();
    }
}
