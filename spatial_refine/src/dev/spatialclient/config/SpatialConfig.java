package dev.spatialclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpatialConfig {
    public enum AuraStyle { ORBIT, HELIX, RIBBON }
    public enum HaloStyle { CLEAN_RING, DOUBLE_RING, ORBITAL }
    public enum CrosshairStyle { CROSS, DOT, BRACKETS }
    public enum HudCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public enum ArmorAnchor { ABOVE_HOTBAR, TOP_CENTER, BOTTOM_RIGHT }
    public enum EffectSort { DURATION, NAME }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("spatialclient.json");
    private static SpatialConfig instance;

    public boolean customCrosshair = true;
    public CrosshairStyle crosshairStyle = CrosshairStyle.CROSS;
    public int crosshairSize = 7;
    public int crosshairGap = 1;
    public int crosshairThickness = 1;
    public int crosshairOpacity = 100;
    public boolean crosshairOutline = true;
    public boolean crosshairUseAccent = true;
    public int crosshairColorRgb = 0xFFFFFF;

    public boolean fpsHud = true;
    public float fpsScale = 1.0F;
    public float fpsOpacity = 0.72F;
    public HudCorner fpsCorner = HudCorner.TOP_LEFT;
    public boolean fpsAccentBar = true;

    public boolean pingHud = true;
    public float pingScale = 1.0F;
    public float pingOpacity = 0.72F;
    public HudCorner pingCorner = HudCorner.TOP_LEFT;
    public boolean pingAccentBar = true;

    public boolean coordinatesHud = true;
    public float coordinatesScale = 1.0F;
    public float coordinatesOpacity = 0.72F;
    public HudCorner coordinatesCorner = HudCorner.TOP_LEFT;
    public boolean decimalCoordinates = false;
    public boolean coordinatesAccentBar = true;

    public boolean keystrokesHud = true;
    public float keystrokesScale = 1.0F;
    public float keystrokesOpacity = 0.72F;
    public HudCorner keystrokesCorner = HudCorner.BOTTOM_LEFT;
    public boolean keystrokesMouseButtons = true;

    public boolean armorHud = true;
    public float armorScale = 1.0F;
    public ArmorAnchor armorAnchor = ArmorAnchor.ABOVE_HOTBAR;
    public boolean armorDurability = true;

    public boolean effectsHud = true;
    public float effectsScale = 1.0F;
    public float effectsOpacity = 0.72F;
    public HudCorner effectsCorner = HudCorner.TOP_RIGHT;
    public boolean effectsDuration = true;
    public EffectSort effectsSort = EffectSort.DURATION;

    public boolean aura = true;
    public AuraStyle auraStyle = AuraStyle.ORBIT;
    public int auraIntensity = 2;
    public float auraSpeed = 1.0F;
    public float auraRadius = 0.52F;
    public float auraHeight = 1.75F;
    public float auraWidth = 0.055F;
    public float auraWave = 0.12F;
    public float auraOpacity = 0.72F;
    public boolean auraUseAccent = true;
    public int auraColorRgb = 0x7700FF;

    public boolean halo = true;
    public HaloStyle haloStyle = HaloStyle.CLEAN_RING;
    public float haloSpeed = 0.8F;
    public float haloRadius = 0.43F;
    public float haloHeight = 2.02F;
    public float haloWidth = 0.040F;
    public float haloTilt = 11.0F;
    public float haloOpacity = 0.72F;
    public boolean haloUseAccent = true;
    public int haloColorRgb = 0x7700FF;

    public boolean headFollow = true;
    public float headFollowYaw = 28.0F;
    public float headFollowPitch = 16.0F;
    public float headFollowSpeed = 0.18F;
    public boolean bodyFacesCamera = true;

    public boolean menuParallax = true;
    public float parallaxStrength = 0.09F;
    public boolean cameraCollision = true;
    public float collisionPadding = 0.20F;
    public float cameraSmoothness = 0.18F;
    public float tabOrbitStrength = 1.0F;
    public float tabVerticalStrength = 1.0F;
    public float tabTransitionSpeed = 1.0F;

    public int accentRgb = 0x7700FF;
    public int menuOpacity = 95;
    public float menuScale = 1.0F;
    public int menuRadius = 15;

    private SpatialConfig() {}

    public static SpatialConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                SpatialConfig read = GSON.fromJson(reader, SpatialConfig.class);
                instance = read == null ? new SpatialConfig() : read;
            } catch (Exception ignored) {
                instance = new SpatialConfig();
            }
        } else {
            instance = new SpatialConfig();
        }
        instance.sanitize();
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private void sanitize() {
        if (crosshairStyle == null) crosshairStyle = CrosshairStyle.CROSS;
        crosshairSize = clamp(crosshairSize, 3, 14);
        crosshairGap = clamp(crosshairGap, 0, 8);
        crosshairThickness = clamp(crosshairThickness, 1, 4);
        crosshairOpacity = clamp(crosshairOpacity, 20, 100);
        crosshairColorRgb &= 0xFFFFFF;

        fpsScale = clamp(fpsScale, 0.70F, 1.55F);
        pingScale = clamp(pingScale, 0.70F, 1.55F);
        coordinatesScale = clamp(coordinatesScale, 0.70F, 1.55F);
        keystrokesScale = clamp(keystrokesScale, 0.70F, 1.55F);
        armorScale = clamp(armorScale, 0.70F, 1.55F);
        effectsScale = clamp(effectsScale, 0.70F, 1.55F);
        fpsOpacity = clamp(fpsOpacity, 0.20F, 1.0F);
        pingOpacity = clamp(pingOpacity, 0.20F, 1.0F);
        coordinatesOpacity = clamp(coordinatesOpacity, 0.20F, 1.0F);
        keystrokesOpacity = clamp(keystrokesOpacity, 0.20F, 1.0F);
        effectsOpacity = clamp(effectsOpacity, 0.20F, 1.0F);
        if (fpsCorner == null) fpsCorner = HudCorner.TOP_LEFT;
        if (pingCorner == null) pingCorner = HudCorner.TOP_LEFT;
        if (coordinatesCorner == null) coordinatesCorner = HudCorner.TOP_LEFT;
        if (keystrokesCorner == null) keystrokesCorner = HudCorner.BOTTOM_LEFT;
        if (effectsCorner == null) effectsCorner = HudCorner.TOP_RIGHT;
        if (armorAnchor == null) armorAnchor = ArmorAnchor.ABOVE_HOTBAR;
        if (effectsSort == null) effectsSort = EffectSort.DURATION;

        if (auraStyle == null) auraStyle = AuraStyle.ORBIT;
        auraIntensity = clamp(auraIntensity, 1, 4);
        auraSpeed = clamp(auraSpeed, 0.25F, 2.50F);
        auraRadius = clamp(auraRadius, 0.30F, 0.90F);
        auraHeight = clamp(auraHeight, 0.70F, 2.35F);
        auraWidth = clamp(auraWidth, 0.015F, 0.12F);
        auraWave = clamp(auraWave, 0.0F, 0.30F);
        auraOpacity = clamp(auraOpacity, 0.15F, 1.0F);
        auraColorRgb &= 0xFFFFFF;

        if (haloStyle == null) haloStyle = HaloStyle.CLEAN_RING;
        haloSpeed = clamp(haloSpeed, 0.0F, 2.50F);
        haloRadius = clamp(haloRadius, 0.24F, 0.75F);
        haloHeight = clamp(haloHeight, 1.55F, 2.55F);
        haloWidth = clamp(haloWidth, 0.012F, 0.10F);
        haloTilt = clamp(haloTilt, -45.0F, 45.0F);
        haloOpacity = clamp(haloOpacity, 0.15F, 1.0F);
        haloColorRgb &= 0xFFFFFF;

        headFollowYaw = clamp(headFollowYaw, 5.0F, 60.0F);
        headFollowPitch = clamp(headFollowPitch, 4.0F, 32.0F);
        headFollowSpeed = clamp(headFollowSpeed, 0.05F, 0.50F);
        parallaxStrength = clamp(parallaxStrength, 0.0F, 0.18F);
        collisionPadding = clamp(collisionPadding, 0.05F, 0.45F);
        cameraSmoothness = clamp(cameraSmoothness, 0.08F, 0.40F);
        tabOrbitStrength = clamp(tabOrbitStrength, 0.25F, 1.35F);
        tabVerticalStrength = clamp(tabVerticalStrength, 0.0F, 1.50F);
        tabTransitionSpeed = clamp(tabTransitionSpeed, 0.55F, 1.80F);

        accentRgb &= 0xFFFFFF;
        menuOpacity = clamp(menuOpacity, 70, 100);
        menuScale = clamp(menuScale, 0.85F, 1.15F);
        menuRadius = clamp(menuRadius, 8, 20);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
