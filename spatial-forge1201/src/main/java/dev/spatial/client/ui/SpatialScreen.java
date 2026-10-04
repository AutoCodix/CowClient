package dev.spatial.client.ui;

import dev.spatial.client.camera.SpatialCameraController;
import dev.spatial.client.camera.SpatialCameraController.CameraPreset;
import dev.spatial.client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public final class SpatialScreen extends Screen {
    private enum Page {
        HOME("Home", CameraPreset.HOME),
        VISUALS("Visuals", CameraPreset.VISUALS),
        HUD("HUD", CameraPreset.HUD),
        COSMETICS("Cosmetics", CameraPreset.COSMETICS),
        CAMERA("Camera", CameraPreset.CAMERA),
        SETTINGS("Settings", CameraPreset.SETTINGS);

        final String label;
        final CameraPreset preset;

        Page(String label, CameraPreset preset) {
            this.label = label;
            this.preset = preset;
        }
    }

    private static final List<Page> PAGES = Arrays.asList(Page.values());
    private static final int[] ACCENTS = {
            0x5AA8FF, 0x7700FF, 0xF16FA1, 0x56D98B, 0xF2B84B, 0x66E0DF
    };

    private final boolean smokePreview;
    private Page page = Page.HOME;
    private boolean closing;
    private float mxNorm;
    private float myNorm;
    private float panelX = Float.NaN;
    private float pageSlide;

    public SpatialScreen() { this(false); }

    public SpatialScreen(boolean smokePreview) {
        super(Component.literal("Spatial Client"));
        this.smokePreview = smokePreview;
    }

    @Override
    protected void init() {
        if (!smokePreview && SpatialCameraController.isActive()) {
            SpatialCameraController.setPreset(page.preset);
        }
        panelX = targetPanelX();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public float normalizedMouseX() { return mxNorm; }
    public float normalizedMouseY() { return myNorm; }

    public void beginClose() {
        if (closing) return;
        closing = true;
        if (smokePreview) {
            Minecraft.getInstance().setScreen(null);
        } else {
            SpatialCameraController.beginClose();
        }
    }

    @Override
    public void tick() {
        if (!smokePreview && closing && SpatialCameraController.consumeCloseFinished()) {
            ClientConfig.save();
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public void removed() {
        if (!smokePreview && SpatialCameraController.isActive() && !closing) {
            SpatialCameraController.forceClose();
            ClientConfig.save();
        }
        super.removed();
    }

    @Override
    public void onClose() { beginClose(); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            beginClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        mxNorm = clamp((mouseX - width * 0.5f) / Math.max(1f, width * 0.5f), -1f, 1f);
        myNorm = clamp((mouseY - height * 0.5f) / Math.max(1f, height * 0.5f), -1f, 1f);

        int panelW = Math.round(320 * ClientConfig.get().menuScale);
        int panelH = Math.min(height - 70, Math.round(438 * ClientConfig.get().menuScale));
        float tx = targetPanelX();
        if (Float.isNaN(panelX)) panelX = tx;
        panelX += (tx - panelX) * 0.18f;
        int x = Math.round(panelX);
        int y = (height - panelH) / 2;

        int bg = 0xDE101722;
        int border = 0x60405E7A;
        int accent = 0xFF000000 | ClientConfig.get().accentColor;
        g.fill(x - 1, y - 1, x + panelW + 1, y + panelH + 1, border);
        g.fill(x, y, x + panelW, y + panelH, bg);
        g.fill(x, y, x + 3, y + panelH, accent);
        g.drawString(font, "SPATIAL", x + 18, y + 16, 0xFFF5F8FC, false);
        g.drawString(font, page.label, x + 18, y + 31, accent, false);

        int navY = y + 55;
        for (int i = 0; i < PAGES.size(); i++) {
            Page p = PAGES.get(i);
            int nx = x + 14 + i % 3 * 95;
            int ny = navY + (i / 3) * 24;
            boolean selected = p == page;
            if (selected) g.fill(nx - 5, ny - 4, nx + 83, ny + 14, 0x49306086);
            g.drawString(font, p.label, nx, ny, selected ? accent : 0xFF9BAABD, false);
        }

        pageSlide *= 0.78f;
        int contentY = y + 116 + Math.round(pageSlide * 10f);
        int contentX = x + 18;
        int contentW = panelW - 36;
        switch (page) {
            case HOME -> renderHome(g, contentX, contentY, contentW);
            case VISUALS -> renderVisuals(g, contentX, contentY, contentW);
            case HUD -> renderHud(g, contentX, contentY, contentW);
            case COSMETICS -> renderCosmetics(g, contentX, contentY, contentW);
            case CAMERA -> renderCamera(g, contentX, contentY, contentW);
            case SETTINGS -> renderSettings(g, contentX, contentY, contentW);
        }

        g.drawString(font, smokePreview ? "UI smoke test" : "RSHIFT / ESC to return",
                x + 18, y + panelH - 20, 0xFF657486, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private int targetPanelX() {
        int panelW = Math.round(320 * ClientConfig.get().menuScale);
        boolean left = page != Page.HUD && page != Page.SETTINGS;
        return left ? 28 : width - panelW - 28;
    }

    private void selectPage(Page next) {
        if (next == page) return;
        page = next;
        pageSlide = 1f;
        if (!smokePreview && SpatialCameraController.isActive()) {
            SpatialCameraController.setPreset(page.preset);
        }
    }

    public void selectPageForSmoke(int ordinal) {
        if (!Boolean.getBoolean("spatialclient.smokeTest")) {
            throw new IllegalStateException("Smoke controls are disabled");
        }
        if (ordinal < 0 || ordinal >= PAGES.size()) {
            throw new IndexOutOfBoundsException("Page " + ordinal);
        }
        selectPage(PAGES.get(ordinal));
        if (!smokePreview && SpatialCameraController.isActive()) {
            SpatialCameraController.setPreset(page.preset);
        }
    }

    public int pageCountForSmoke() {
        return PAGES.size();
    }

    public String currentPageForSmoke() {
        return page.label;
    }

    private void renderHome(GuiGraphics g, int x, int y, int w) {
        card(g, x, y, w, 62, "Spatial instead of flat", "Pages move the camera around your player while keeping the face in the composition.");
        card(g, x, y + 72, w, 62, "Skin-first", "Your real skin stays in-world and the head subtly follows your cursor.");
        card(g, x, y + 144, w, 62, "Visual-only", "HUD, camera and cosmetics only. No combat, X-ray, packet or movement advantages.");
    }

    private void renderVisuals(GuiGraphics g, int x, int y, int w) {
        ClientConfig.Data c = ClientConfig.get();
        toggle(g, x, y, w, "Body aura", c.auraEnabled);
        option(g, x, y + 34, w, "Aura style", c.auraStyle.displayName());
        slider(g, x, y + 68, w, "Radius", c.auraRadius, 0.45f, 1.8f);
        slider(g, x, y + 104, w, "Opacity", c.auraOpacity, 0.1f, 1f);
        slider(g, x, y + 140, w, "Speed", c.auraSpeed, 0.1f, 2.5f);
        slider(g, x, y + 176, w, "Objects", c.auraCount, 3f, 12f);
        g.drawString(font, "Visible only in F5, detached freelook or this menu.", x, y + 212, 0xFF77879A, false);
    }

    private void renderHud(GuiGraphics g, int x, int y, int w) {
        ClientConfig.Data c = ClientConfig.get();
        toggle(g, x, y, w, "HUD", c.hudEnabled);
        toggle(g, x, y + 30, w, "FPS", c.showFps);
        toggle(g, x, y + 60, w, "Ping", c.showPing);
        toggle(g, x, y + 90, w, "Coordinates", c.showCoordinates);
        toggle(g, x, y + 120, w, "Keystrokes", c.showKeystrokes);
        toggle(g, x, y + 160, w, "Custom crosshair", c.crosshairEnabled);
        slider(g, x, y + 194, w, "Gap", c.crosshairGap, 0, 12);
        slider(g, x, y + 226, w, "Length", c.crosshairLength, 2, 14);
    }

    private void renderCosmetics(GuiGraphics g, int x, int y, int w) {
        option(g, x, y, w, "Aura collection", ClientConfig.get().auraStyle.displayName());
        card(g, x, y + 38, w, 72, "Cards + mice + more", "Orbiting cards, computer-mouse glyphs, shards, stars, runes and a mixed mode are rendered around only your own player.");
        card(g, x, y + 120, w, 70, "Real skin", "The world renderer uses your current skin and normal outer skin layers. No fake avatar preview is substituted.");
        g.drawString(font, "Cosmetics never send custom packets to the server.", x, y + 208, 0xFF77879A, false);
    }

    private void renderCamera(GuiGraphics g, int x, int y, int w) {
        slider(g, x, y, w, "Camera smoothness", ClientConfig.get().cameraSmoothness, 0.06f, 0.35f);
        card(g, x, y + 52, w, 84, "Face-visible navigation", "Opening eases out of first person. Every page blends to a different front/side composition and closing reverses the transition.");
        card(g, x, y + 146, w, 62, "Safe restore", "Your previous camera mode is restored when the menu closes or the world unloads.");
    }

    private void renderSettings(GuiGraphics g, int x, int y, int w) {
        slider(g, x, y, w, "Menu scale", ClientConfig.get().menuScale, 0.8f, 1.25f);
        option(g, x, y + 48, w, "Accent", String.format("#%06X", ClientConfig.get().accentColor & 0xFFFFFF));
        card(g, x, y + 92, w, 84, "Compatibility-first", "One client camera mixin plus Forge render/input events. No world edits, packets, server mixins or gameplay automation.");
        card(g, x, y + 186, w, 62, "Persistent", "Visual settings save to config/spatialclient.json when the menu closes.");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelW = Math.round(320 * ClientConfig.get().menuScale);
        int panelH = Math.min(height - 70, Math.round(438 * ClientConfig.get().menuScale));
        int x = Math.round(panelX);
        int y = (height - panelH) / 2;
        int navY = y + 55;
        for (int i = 0; i < PAGES.size(); i++) {
            int nx = x + 14 + i % 3 * 95;
            int ny = navY + (i / 3) * 24;
            if (hit(mouseX, mouseY, nx - 5, ny - 5, 88, 20)) {
                selectPage(PAGES.get(i));
                return true;
            }
        }

        int cx = x + 18;
        int cy = y + 116;
        int w = panelW - 36;
        ClientConfig.Data c = ClientConfig.get();
        if (page == Page.VISUALS) {
            if (hit(mouseX, mouseY, cx, cy, w, 24)) { c.auraEnabled = !c.auraEnabled; return true; }
            if (hit(mouseX, mouseY, cx, cy + 34, w, 24)) { c.auraStyle = next(c.auraStyle); return true; }
            if (handleSlider(mouseX, mouseY, cx, cy + 68, w, 0.45f, 1.8f, v -> c.auraRadius = v)) return true;
            if (handleSlider(mouseX, mouseY, cx, cy + 104, w, 0.1f, 1f, v -> c.auraOpacity = v)) return true;
            if (handleSlider(mouseX, mouseY, cx, cy + 140, w, 0.1f, 2.5f, v -> c.auraSpeed = v)) return true;
            if (handleSlider(mouseX, mouseY, cx, cy + 176, w, 3f, 12f, v -> c.auraCount = Math.round(v))) return true;
        } else if (page == Page.HUD) {
            if (hit(mouseX, mouseY, cx, cy, w, 24)) { c.hudEnabled = !c.hudEnabled; return true; }
            if (hit(mouseX, mouseY, cx, cy + 30, w, 24)) { c.showFps = !c.showFps; return true; }
            if (hit(mouseX, mouseY, cx, cy + 60, w, 24)) { c.showPing = !c.showPing; return true; }
            if (hit(mouseX, mouseY, cx, cy + 90, w, 24)) { c.showCoordinates = !c.showCoordinates; return true; }
            if (hit(mouseX, mouseY, cx, cy + 120, w, 24)) { c.showKeystrokes = !c.showKeystrokes; return true; }
            if (hit(mouseX, mouseY, cx, cy + 160, w, 24)) { c.crosshairEnabled = !c.crosshairEnabled; return true; }
            if (handleSlider(mouseX, mouseY, cx, cy + 194, w, 0, 12, v -> c.crosshairGap = v)) return true;
            if (handleSlider(mouseX, mouseY, cx, cy + 226, w, 2, 14, v -> c.crosshairLength = v)) return true;
        } else if (page == Page.COSMETICS) {
            if (hit(mouseX, mouseY, cx, cy, w, 24)) { c.auraStyle = next(c.auraStyle); return true; }
        } else if (page == Page.CAMERA) {
            if (handleSlider(mouseX, mouseY, cx, cy, w, 0.06f, 0.35f, v -> c.cameraSmoothness = v)) return true;
        } else if (page == Page.SETTINGS) {
            if (handleSlider(mouseX, mouseY, cx, cy, w, 0.8f, 1.25f, v -> c.menuScale = v)) return true;
            if (hit(mouseX, mouseY, cx, cy + 48, w, 24)) { cycleAccent(c); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return mouseClicked(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private ClientConfig.AuraStyle next(ClientConfig.AuraStyle s) {
        ClientConfig.AuraStyle[] values = ClientConfig.AuraStyle.values();
        return values[(s.ordinal() + 1) % values.length];
    }

    private void cycleAccent(ClientConfig.Data c) {
        int current = c.accentColor & 0xFFFFFF;
        int index = 0;
        for (int i = 0; i < ACCENTS.length; i++) {
            if (ACCENTS[i] == current) { index = i; break; }
        }
        c.accentColor = ACCENTS[(index + 1) % ACCENTS.length];
    }

    private void toggle(GuiGraphics g, int x, int y, int w, String label, boolean on) {
        g.fill(x, y, x + w, y + 24, 0x4A1C2735);
        g.drawString(font, label, x + 8, y + 8, 0xFFE7EDF5, false);
        int c = on ? (0xFF000000 | ClientConfig.get().accentColor) : 0xFF4D5968;
        g.fill(x + w - 34, y + 7, x + w - 10, y + 17, c);
        g.fill(on ? x + w - 20 : x + w - 32, y + 5, on ? x + w - 10 : x + w - 22, y + 19, 0xFFF5F8FC);
    }

    private void option(GuiGraphics g, int x, int y, int w, String label, String value) {
        g.fill(x, y, x + w, y + 24, 0x4A1C2735);
        g.drawString(font, label, x + 8, y + 8, 0xFFE7EDF5, false);
        int tw = font.width(value);
        g.drawString(font, value, x + w - tw - 8, y + 8, 0xFF81BFFF, false);
    }

    private void slider(GuiGraphics g, int x, int y, int w, String label, float value, float min, float max) {
        g.drawString(font, label, x, y, 0xFFE7EDF5, false);
        String val = (max - min > 5f) ? String.format("%.0f", value) : String.format("%.2f", value);
        g.drawString(font, val, x + w - font.width(val), y, 0xFF8FA0B4, false);
        int sy = y + 16;
        g.fill(x, sy, x + w, sy + 3, 0xFF2A3645);
        int filled = (int) (w * ((value - min) / (max - min)));
        g.fill(x, sy, x + filled, sy + 3, 0xFF000000 | ClientConfig.get().accentColor);
    }

    private void card(GuiGraphics g, int x, int y, int w, int h, String title, String body) {
        g.fill(x, y, x + w, y + h, 0x431B2633);
        g.drawString(font, title, x + 9, y + 9, 0xFFF0F4F9, false);
        drawWrapped(g, body, x + 9, y + 26, w - 18, 0xFF8292A6);
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int w, int color) {
        for (var line : font.split(Component.literal(text), w)) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
    }

    private boolean handleSlider(double mx, double my, int x, int y, int w,
                                 float min, float max, FloatSetter set) {
        if (!hit(mx, my, x, y + 8, w, 20)) return false;
        float p = clamp((float) ((mx - x) / (double) w), 0, 1);
        set.set(min + (max - min) * p);
        return true;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    @FunctionalInterface
    private interface FloatSetter { void set(float value); }
}
