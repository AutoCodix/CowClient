package dev.spatial.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("spatialclient.json");
    private static Data data = new Data();

    private ClientConfig() { }

    public static Data get() { return data; }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) data = loaded;
        } catch (Exception ignored) {
            data = new Data();
        }
        data.sanitize();
    }

    public static void save() {
        data.sanitize();
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) { }
    }

    public static final class Data {
        public boolean auraEnabled = true;
        public AuraStyle auraStyle = AuraStyle.MIXED;
        public float auraRadius = 0.88f;
        public float auraOpacity = 0.74f;
        public float auraSpeed = 0.85f;
        public int auraCount = 8;
        public int accentColor = 0x5AA8FF;

        public boolean hudEnabled = true;
        public boolean showFps = true;
        public boolean showCoordinates = true;
        public boolean showPing = true;
        public boolean showKeystrokes = true;

        public boolean crosshairEnabled = true;
        public float crosshairGap = 3.0f;
        public float crosshairLength = 5.0f;
        public float crosshairThickness = 1.0f;

        public float cameraSmoothness = 0.14f;
        public float menuScale = 1.0f;

        private void sanitize() {
            auraRadius = clamp(auraRadius, 0.45f, 1.8f);
            auraOpacity = clamp(auraOpacity, 0.1f, 1f);
            auraSpeed = clamp(auraSpeed, 0.1f, 2.5f);
            auraCount = Math.max(3, Math.min(12, auraCount));
            crosshairGap = clamp(crosshairGap, 0f, 12f);
            crosshairLength = clamp(crosshairLength, 2f, 14f);
            crosshairThickness = clamp(crosshairThickness, 1f, 4f);
            cameraSmoothness = clamp(cameraSmoothness, 0.06f, 0.35f);
            menuScale = clamp(menuScale, 0.8f, 1.25f);
            if (auraStyle == null) auraStyle = AuraStyle.MIXED;
        }

        private static float clamp(float v, float min, float max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    public enum AuraStyle {
        ORBIT("Orbit"),
        HALO("Halo"),
        SPIRAL("Spiral"),
        CARDS("Cards"),
        MICE("Mice"),
        SHARDS("Shards"),
        STARS("Stars"),
        RUNES("Runes"),
        MIXED("Mixed");

        private final String displayName;

        AuraStyle(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
