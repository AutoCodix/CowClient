package dev.spatialclient.client;

public enum SpatialSection {
    HOME("Overview", "Client overview", 3.20, -0.56, 0.26, -0.34),
    VISUALS("Visual", "Screen and crosshair polish", 3.18, -0.62, 0.27, -0.36),
    HUD("HUD", "Useful client overlays", 3.25, -0.49, 0.31, -0.33),
    COSMETICS("Cosmetics", "Aura and player presentation", 3.35, -0.72, 0.33, -0.40),
    CAMERA("Camera", "Spatial camera behavior", 3.05, -0.42, 0.38, -0.28),
    SETTINGS("Settings", "Client preferences", 3.22, -0.56, 0.29, -0.34);

    public final String title;
    public final String subtitle;
    public final double distance;
    public final double sideOffset;
    public final double heightOffset;
    public final double lookOffset;

    SpatialSection(String title, String subtitle, double distance, double sideOffset, double heightOffset, double lookOffset) {
        this.title = title;
        this.subtitle = subtitle;
        this.distance = distance;
        this.sideOffset = sideOffset;
        this.heightOffset = heightOffset;
        this.lookOffset = lookOffset;
    }
}
