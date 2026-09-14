package dev.cowclient.core;

public record Module(String id, String name, String description, String category, String preview, boolean defaultEnabled) {
    public boolean isHud() { return category.equals("HUD"); }
}
