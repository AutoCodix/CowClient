package dev.spatial.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.spatial.client.camera.SpatialCameraController;
import dev.spatial.client.config.ClientConfig;
import dev.spatial.client.render.AuraRenderer;
import dev.spatial.client.render.HudRenderer;
import dev.spatial.client.render.PlayerLookController;
import dev.spatial.client.ui.SpatialScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;

public final class SmokeTestController {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int WATCHDOG_TICKS = 20 * 150;

    private enum Phase {
        WAIT_TITLE,
        WAIT_WORLD,
        SETTLE,
        PAGE_SWEEP,
        AURA_SWEEP,
        CLOSE_MENU,
        HUD,
        DONE
    }

    private static Phase phase = Phase.WAIT_TITLE;
    private static int phaseTicks;
    private static int totalTicks;
    private static int pageIndex;
    private static int auraIndex;
    private static Vec3 previousCameraPosition;
    private static boolean finished;

    private SmokeTestController() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("spatialclient.smokeTest")
                || event.phase != TickEvent.Phase.END
                || finished) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        totalTicks++;
        phaseTicks++;

        try {
            if (totalTicks > WATCHDOG_TICKS) {
                throw new IllegalStateException("Smoke test watchdog expired in phase " + phase);
            }
            step(mc);
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void step(Minecraft mc) throws Exception {
        switch (phase) {
            case WAIT_TITLE -> {
                if (mc.getWindow() == null) return;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(2);
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);

                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                    createWorld(mc);
                    setPhase(Phase.WAIT_WORLD);
                } else if (phaseTicks > 200) {
                    mc.setScreen(new TitleScreen());
                    phaseTicks = 0;
                }
            }
            case WAIT_WORLD -> {
                if (mc.player != null && mc.level != null && mc.screen == null) {
                    LOGGER.info("SPATIALCLIENT_SMOKE_WORLD_READY");
                    setPhase(Phase.SETTLE);
                }
            }
            case SETTLE -> {
                if (phaseTicks < 50) return;

                ClientConfig.Data cfg = ClientConfig.get();
                cfg.auraEnabled = true;
                cfg.auraStyle = ClientConfig.AuraStyle.MIXED;
                cfg.auraOpacity = 0.82f;
                cfg.auraRadius = 0.88f;
                cfg.auraCount = 8;
                cfg.hudEnabled = true;
                cfg.crosshairEnabled = true;

                SpatialCameraController.beginOpen();
                SpatialScreen screen = new SpatialScreen();
                mc.setScreen(screen);
                screen.selectPageForSmoke(0);
                pageIndex = 0;
                previousCameraPosition = null;
                LOGGER.info("SPATIALCLIENT_SMOKE_UI_OPEN");
                setPhase(Phase.PAGE_SWEEP);
            }
            case PAGE_SWEEP -> {
                if (!(mc.screen instanceof SpatialScreen screen)) {
                    throw new IllegalStateException("Spatial screen disappeared during page sweep");
                }
                if (phaseTicks < 22) return;

                if (!SpatialCameraController.isActive()) {
                    throw new IllegalStateException("Spatial camera is not active");
                }
                if (mc.options.getCameraType() != CameraType.THIRD_PERSON_FRONT) {
                    throw new IllegalStateException("Spatial camera did not force front third person");
                }

                Vec3 cameraPosition = mc.gameRenderer.getMainCamera().getPosition();
                Vec3 eye = mc.player.getEyePosition();
                if (cameraPosition.distanceTo(eye) < 1.15) {
                    throw new IllegalStateException("Camera never moved far enough away from the player");
                }
                if (previousCameraPosition != null && cameraPosition.distanceTo(previousCameraPosition) < 0.06) {
                    throw new IllegalStateException("Camera did not move between spatial pages " + (pageIndex - 1) + " and " + pageIndex);
                }
                previousCameraPosition = cameraPosition;

                String page = screen.currentPageForSmoke();
                capture(mc, "spatialclient-page-" + page.toLowerCase(Locale.ROOT) + ".png");
                LOGGER.info("SPATIALCLIENT_SMOKE_PAGE_OK {}", page);

                pageIndex++;
                if (pageIndex < screen.pageCountForSmoke()) {
                    screen.selectPageForSmoke(pageIndex);
                    phaseTicks = 0;
                } else {
                    screen.selectPageForSmoke(3);
                    auraIndex = 0;
                    ClientConfig.get().auraStyle = ClientConfig.AuraStyle.values()[auraIndex];
                    setPhase(Phase.AURA_SWEEP);
                }
            }
            case AURA_SWEEP -> {
                if (!(mc.screen instanceof SpatialScreen)) {
                    throw new IllegalStateException("Spatial screen disappeared during aura sweep");
                }
                if (phaseTicks < 14) return;

                ClientConfig.AuraStyle style = ClientConfig.AuraStyle.values()[auraIndex];
                if (!AuraRenderer.smokeRendered(style)) {
                    throw new IllegalStateException("Aura renderer did not render style " + style);
                }

                capture(mc, "spatialclient-aura-" + style.name().toLowerCase(Locale.ROOT) + ".png");
                LOGGER.info("SPATIALCLIENT_SMOKE_AURA_OK {}", style);

                auraIndex++;
                if (auraIndex < ClientConfig.AuraStyle.values().length) {
                    ClientConfig.get().auraStyle = ClientConfig.AuraStyle.values()[auraIndex];
                    phaseTicks = 0;
                } else {
                    if (PlayerLookController.smokeRenderCount() < 1) {
                        throw new IllegalStateException("Player head-follow render hook was never exercised");
                    }
                    ((SpatialScreen) mc.screen).beginClose();
                    setPhase(Phase.CLOSE_MENU);
                }
            }
            case CLOSE_MENU -> {
                if (mc.screen != null || SpatialCameraController.isActive()) return;
                if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                    throw new IllegalStateException("Previous camera mode was not restored");
                }
                LOGGER.info("SPATIALCLIENT_SMOKE_CAMERA_RESTORE_OK");
                setPhase(Phase.HUD);
            }
            case HUD -> {
                if (phaseTicks < 30) return;
                if (HudRenderer.smokeHudFrames() < 1) {
                    throw new IllegalStateException("HUD renderer was never exercised");
                }
                if (HudRenderer.smokeCrosshairFrames() < 1) {
                    throw new IllegalStateException("Custom crosshair renderer was never exercised");
                }
                capture(mc, "spatialclient-hud.png");

                int savedAccent = ClientConfig.get().accentColor;
                ClientConfig.save();
                ClientConfig.get().accentColor = 0;
                ClientConfig.load();
                if (ClientConfig.get().accentColor != savedAccent) {
                    throw new IllegalStateException("Config save/reload did not persist settings");
                }

                finished = true;
                phase = Phase.DONE;
                LOGGER.info("SPATIALCLIENT_SMOKE_OK");
                mc.stop();
            }
            case DONE -> { }
        }
    }

    private static void createWorld(Minecraft mc) {
        String name = "spatial-smoke-" + (System.currentTimeMillis() / 1000L);
        LevelSettings settings = new LevelSettings(
                "Spatial Client Smoke",
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(),
                WorldDataConfiguration.DEFAULT
        );

        LOGGER.info("SPATIALCLIENT_SMOKE_WORLD_CREATE {}", name);
        mc.createWorldOpenFlows().createFreshLevel(
                name,
                settings,
                new WorldOptions(42L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT)
                        .value()
                        .createWorldDimensions()
        );
    }

    private static void capture(Minecraft mc, String fileName) throws Exception {
        Path path = mc.gameDirectory.toPath().resolve(fileName);
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            if (image.getWidth() < 320 || image.getHeight() < 200) {
                throw new IllegalStateException("Unexpected framebuffer size " + image.getWidth() + "x" + image.getHeight());
            }

            int unique = 0;
            int[] seen = new int[24];
            int seenCount = 0;
            int stepX = Math.max(1, image.getWidth() / 40);
            int stepY = Math.max(1, image.getHeight() / 30);
            outer:
            for (int y = 0; y < image.getHeight(); y += stepY) {
                for (int x = 0; x < image.getWidth(); x += stepX) {
                    int pixel = image.getPixelRGBA(x, y);
                    boolean exists = false;
                    for (int i = 0; i < seenCount; i++) {
                        if (seen[i] == pixel) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        if (seenCount < seen.length) seen[seenCount++] = pixel;
                        unique++;
                        if (unique >= 12) break outer;
                    }
                }
            }
            if (unique < 12) {
                throw new IllegalStateException("Framebuffer appears blank/flat for " + fileName + " (unique samples=" + unique + ")");
            }

            image.writeToFile(path);
        }
    }

    private static void setPhase(Phase next) {
        LOGGER.info("SPATIALCLIENT_SMOKE_PHASE {} -> {}", phase, next);
        phase = next;
        phaseTicks = 0;
    }

    private static void fail(Throwable t) {
        if (finished) return;
        finished = true;
        phase = Phase.DONE;
        LOGGER.error("SPATIALCLIENT_SMOKE_FAILED", t);
        Runtime.getRuntime().halt(1);
    }
}
