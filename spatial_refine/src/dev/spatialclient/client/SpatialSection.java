package dev.spatialclient.client;

public enum SpatialSection {
    HOME("Overview", "Client overview", -11.0, 0.27, -0.36),
    VISUALS("Visual", "Screen and crosshair polish", -11.0, 0.27, -0.36),
    HUD("HUD", "Useful client overlays", 78.0, 0.12, -0.10),
    COSMETICS("Cosmetics", "Aura, halo and player presentation", 158.0, 0.38, 0.12),
    CAMERA("Camera", "Spatial camera behavior", -76.0, 0.84, -0.08),
    SETTINGS("Settings", "Client preferences", 20.0, -0.28, -0.18);

    public final String title;
    public final String subtitle;
    public final double orbitDegrees;
    public final double heightOffset;
    public final double lookOffset;

    SpatialSection(String title, String subtitle, double orbitDegrees, double heightOffset, double lookOffset) {
        this.title = title;
        this.subtitle = subtitle;
        this.orbitDegrees = orbitDegrees;
        this.heightOffset = heightOffset;
        this.lookOffset = lookOffset;
    }
}
