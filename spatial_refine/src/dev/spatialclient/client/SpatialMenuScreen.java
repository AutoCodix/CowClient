package dev.spatialclient.client;

import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpatialMenuScreen extends Screen {
    private static final SpatialSection[] MODULE_SECTIONS = {
            SpatialSection.VISUALS, SpatialSection.HUD, SpatialSection.COSMETICS, SpatialSection.CAMERA
    };

    private SpatialSection section = SpatialSection.VISUALS;
    private final List<Row> rows = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sidebarW;
    private int searchX;
    private int searchY;
    private int searchW;
    private boolean searchFocused;
    private String search = "";

    public SpatialMenuScreen() {
        super(Component.literal("Spatial Client"));
    }

    @Override
    protected void init() {
        super.init();
        section = SpatialSection.VISUALS;
        SpatialCamera.setSection(section);
        rebuildRows();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SpatialCamera.setMouse(mouseX, mouseY, width, height);
        HeadTracking.updateMouse(mouseX, mouseY, width, height);
        layout();

        int accent = 0xFF000000 | SpatialConfig.get().accentRgb;
        UiRenderer.shadow(g, panelX, panelY, panelW, panelH, 15.0F);
        UiRenderer.roundedRect(g, panelX, panelY, panelW, panelH, 15.0F, 0xF20D0F23);

        UiRenderer.roundedRect(g, panelX + 1, panelY + 1, sidebarW, panelH - 2, 14.0F, 0xF5070919);
        g.fill(panelX + sidebarW - 14, panelY + 1, panelX + sidebarW + 1, panelY + panelH - 1, 0xF5070919);
        g.fill(panelX + sidebarW, panelY + 16, panelX + sidebarW + 1, panelY + panelH - 16, 0x55262A48);

        drawBrand(g, accent);
        drawSidebar(g, mouseX, mouseY, accent);
        drawContent(g, mouseX, mouseY, accent);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBrand(GuiGraphics g, int accent) {
        UiRenderer.roundedRect(g, panelX + 14, panelY + 14, 22, 22, 7, 0xFF191338);
        UiRenderer.roundedRect(g, panelX + 20, panelY + 19, 10, 3, 2, accent);
        UiRenderer.roundedRect(g, panelX + 18, panelY + 24, 14, 3, 2, 0xFFC7B7FF);
        UiFont.draw(g, "Spatial Client", panelX + 44, panelY + 14, 0xFFF4F2FF, 0.38F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "FORGE 1.20.1", panelX + 44, panelY + 28, 0xFF676D8B, 0.245F, UiFont.Weight.REGULAR);
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY, int accent) {
        int y = panelY + 59;
        UiFont.draw(g, "MODULES", panelX + 17, y, 0xFF555B78, 0.23F, UiFont.Weight.SEMIBOLD);
        y += 17;

        for (SpatialSection s : MODULE_SECTIONS) {
            boolean selected = section == s;
            boolean hover = inside(mouseX, mouseY, panelX + 11, y - 3, sidebarW - 22, 32);
            if (selected || hover) {
                UiRenderer.roundedRect(g, panelX + 10, y - 4, sidebarW - 20, 32, 8,
                        selected ? 0xFF282050 : 0xA3151730);
            }
            if (selected) UiRenderer.roundedRect(g, panelX + 13, y + 6, 3, 12, 2, accent);
            drawNavIcon(g, panelX + 24, y + 10, s, selected ? accent : 0xFF777E9D);
            UiFont.draw(g, s.title, panelX + 39, y + 4, selected ? 0xFFF5F3FF : 0xFFA4A8BE,
                    0.31F, selected ? UiFont.Weight.SEMIBOLD : UiFont.Weight.REGULAR);
            String count = Integer.toString(countFor(s));
            float cw = UiFont.width(count, 0.235F, UiFont.Weight.REGULAR);
            UiFont.draw(g, count, panelX + sidebarW - 19 - cw, y + 6, 0xFF6A6F8A, 0.235F, UiFont.Weight.REGULAR);
            y += 39;
        }

        y += 8;
        UiFont.draw(g, "GENERAL", panelX + 17, y, 0xFF555B78, 0.23F, UiFont.Weight.SEMIBOLD);
        y += 18;
        boolean selected = section == SpatialSection.SETTINGS;
        boolean hover = inside(mouseX, mouseY, panelX + 11, y - 3, sidebarW - 22, 32);
        if (selected || hover) {
            UiRenderer.roundedRect(g, panelX + 10, y - 4, sidebarW - 20, 32, 8,
                    selected ? 0xFF282050 : 0xA3151730);
        }
        if (selected) UiRenderer.roundedRect(g, panelX + 13, y + 6, 3, 12, 2, accent);
        drawNavIcon(g, panelX + 24, y + 10, SpatialSection.SETTINGS, selected ? accent : 0xFF777E9D);
        UiFont.draw(g, "Settings", panelX + 39, y + 4, selected ? 0xFFF5F3FF : 0xFFA4A8BE,
                0.31F, selected ? UiFont.Weight.SEMIBOLD : UiFont.Weight.REGULAR);

        UiFont.draw(g, "RSHIFT  close", panelX + 17, panelY + panelH - 25, 0xFF555B78, 0.245F, UiFont.Weight.REGULAR);
    }

    private void drawContent(GuiGraphics g, int mouseX, int mouseY, int accent) {
        int x = panelX + sidebarW + 18;
        int right = panelX + panelW - 17;
        String title = section == SpatialSection.SETTINGS ? "Settings" : section.title + " Modules";
        List<Row> visible = visibleRows();
        int enabled = 0;
        for (Row row : visible) if (row.kind == RowKind.TOGGLE && row.active.get()) enabled++;

        UiFont.draw(g, title, x, panelY + 17, 0xFFF4F2FF, 0.48F, UiFont.Weight.SEMIBOLD);
        String sub = visible.size() + (visible.size() == 1 ? " module" : " modules") + (section == SpatialSection.SETTINGS ? "" : "  /  " + enabled + " enabled");
        UiFont.draw(g, sub, x, panelY + 38, 0xFF676D8B, 0.255F, UiFont.Weight.REGULAR);

        searchW = Math.min(184, Math.max(126, panelW / 4));
        searchX = right - searchW;
        searchY = panelY + 15;
        UiRenderer.roundedOutline(g, searchX, searchY, searchW, 28, 8, 1,
                searchFocused ? 0xFF41347A : 0xFF20243D, 0xFF0A0C1D);
        UiRenderer.circle(g, searchX + 13, searchY + 13, 4.0F, 0xFF7A809B);
        UiRenderer.circle(g, searchX + 13, searchY + 13, 2.6F, 0xFF0A0C1D);
        g.fill(searchX + 16, searchY + 16, searchX + 20, searchY + 18, 0xFF7A809B);
        String searchText = search.isEmpty() ? "Search modules" : search;
        int searchColor = search.isEmpty() ? 0xFF6F7591 : 0xFFE7E4F4;
        UiFont.draw(g, searchText, searchX + 25, searchY + 7, searchColor, 0.265F, UiFont.Weight.REGULAR);

        int headerY = panelY + 61;
        UiRenderer.roundedRect(g, x, headerY, right - x, 27, 7, 0xFF101329);
        UiFont.draw(g, section == SpatialSection.SETTINGS ? "Client preferences" : "Visual-only / client-side",
                x + 12, headerY + 7, 0xFF858AA5, 0.25F, UiFont.Weight.REGULAR);
        drawViewButtons(g, right - 52, headerY + 5, accent);

        int y = panelY + 98;
        if (visible.isEmpty()) {
            UiRenderer.roundedRect(g, x, y, right - x, 48, 9, 0xFF11142B);
            UiFont.draw(g, "No matching modules", x + 13, y + 12, 0xFFC9C6D8, 0.31F, UiFont.Weight.SEMIBOLD);
            UiFont.draw(g, "Try a different search.", x + 13, y + 28, 0xFF686E89, 0.245F, UiFont.Weight.REGULAR);
            return;
        }

        int rowH = 47;
        int gap = 7;
        for (int i = 0; i < visible.size(); i++) {
            Row row = visible.get(i);
            int ry = y + i * (rowH + gap);
            if (ry + rowH > panelY + panelH - 14) break;
            boolean hover = inside(mouseX, mouseY, x, ry, right - x, rowH);
            UiRenderer.roundedOutline(g, x, ry, right - x, rowH, 9, 1,
                    hover ? 0xFF262A49 : 0xFF1B1E38,
                    hover ? 0xFF14172E : 0xFF11142A);
            UiFont.draw(g, row.name, x + 13, ry + 9, 0xFFF0EDF8, 0.305F, UiFont.Weight.SEMIBOLD);
            UiFont.draw(g, row.description, x + 13, ry + 26, 0xFF676D87, 0.235F, UiFont.Weight.REGULAR);

            if (row.kind == RowKind.TOGGLE) {
                drawChevron(g, right - 53, ry + 20, 0xFF7D839F);
                drawToggle(g, right - 35, ry + 15, row.active.get(), accent);
            } else {
                String value = row.value.get();
                float vw = UiFont.width(value, 0.235F, UiFont.Weight.SEMIBOLD);
                float pillW = Math.max(42.0F, vw + 18.0F);
                UiRenderer.roundedRect(g, right - pillW - 12, ry + 12, pillW, 23, 7, 0xFF1C2040);
                UiFont.draw(g, value, right - pillW - 12 + (pillW - vw) * 0.5F, ry + 18,
                        0xFFCFC8F0, 0.235F, UiFont.Weight.SEMIBOLD);
                drawChevron(g, right - pillW - 25, ry + 20, 0xFF7D839F);
            }
        }
    }

    private void drawViewButtons(GuiGraphics g, int x, int y, int accent) {
        UiRenderer.roundedRect(g, x, y, 21, 17, 7, 0xFF27214E);
        g.fill(x + 6, y + 5, x + 15, y + 7, accent);
        g.fill(x + 6, y + 9, x + 15, y + 11, 0xFF8E88AC);
        UiRenderer.roundedRect(g, x + 25, y, 21, 17, 7, 0xFF14172E);
        UiRenderer.circle(g, x + 31, y + 6, 1.5F, 0xFF777D98);
        UiRenderer.circle(g, x + 39, y + 6, 1.5F, 0xFF777D98);
        UiRenderer.circle(g, x + 31, y + 12, 1.5F, 0xFF777D98);
        UiRenderer.circle(g, x + 39, y + 12, 1.5F, 0xFF777D98);
    }

    private void drawNavIcon(GuiGraphics g, int cx, int cy, SpatialSection s, int color) {
        switch (s) {
            case VISUALS -> {
                UiRenderer.circle(g, cx, cy + 1, 5, color);
                UiRenderer.circle(g, cx, cy + 1, 2.7F, 0xFF0A0C1D);
            }
            case HUD -> {
                UiRenderer.roundedRect(g, cx - 5, cy - 4, 10, 8, 2, color);
                UiRenderer.roundedRect(g, cx - 3, cy - 2, 6, 4, 1, 0xFF0A0C1D);
            }
            case COSMETICS -> {
                UiRenderer.circle(g, cx, cy - 1, 4.2F, color);
                UiRenderer.roundedRect(g, cx - 5, cy + 4, 10, 2, 1, color);
            }
            case CAMERA -> {
                UiRenderer.roundedRect(g, cx - 5, cy - 4, 10, 8, 2, color);
                UiRenderer.circle(g, cx, cy, 2.2F, 0xFF0A0C1D);
            }
            case SETTINGS -> {
                UiRenderer.circle(g, cx, cy, 5, color);
                UiRenderer.circle(g, cx, cy, 2.1F, 0xFF0A0C1D);
            }
            default -> UiRenderer.circle(g, cx, cy, 4, color);
        }
    }

    private void drawChevron(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y - 3, x + 2, y + 1, color);
        g.fill(x + 2, y - 1, x + 4, y + 3, color);
    }

    private void drawToggle(GuiGraphics g, int x, int y, boolean on, int accent) {
        UiRenderer.roundedRect(g, x, y, 25, 15, 8, on ? 0xFF3A2F72 : 0xFF24283F);
        UiRenderer.circle(g, on ? x + 17.5F : x + 7.5F, y + 7.5F, 5.0F, on ? 0xFFF3EFFF : 0xFF9A9FB5);
        if (on) UiRenderer.roundedRect(g, x + 3, y + 5.5F, 8, 4, 2, accent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        layout();

        if (inside(mouseX, mouseY, searchX, searchY, searchW, 28)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        int y = panelY + 76;
        for (SpatialSection s : MODULE_SECTIONS) {
            if (inside(mouseX, mouseY, panelX + 10, y - 4, sidebarW - 20, 32)) {
                switchSection(s);
                return true;
            }
            y += 39;
        }
        y += 26;
        if (inside(mouseX, mouseY, panelX + 10, y - 4, sidebarW - 20, 32)) {
            switchSection(SpatialSection.SETTINGS);
            return true;
        }

        int x = panelX + sidebarW + 18;
        int right = panelX + panelW - 17;
        int ry = panelY + 98;
        int rowH = 47;
        int gap = 7;
        List<Row> visible = visibleRows();
        for (int i = 0; i < visible.size(); i++) {
            int yy = ry + i * (rowH + gap);
            if (yy + rowH > panelY + panelH - 14) break;
            if (inside(mouseX, mouseY, x, yy, right - x, rowH)) {
                visible.get(i).action.run();
                SpatialConfig.get().save();
                rebuildRows();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && codePoint >= 32 && codePoint <= 126 && search.length() < 28) {
            search += codePoint;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) {
                search = "";
                searchFocused = false;
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        SpatialCamera.beginClose();
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void switchSection(SpatialSection s) {
        section = s;
        search = "";
        searchFocused = false;
        SpatialCamera.setSection(s);
        rebuildRows();
    }

    private void layout() {
        panelW = Math.min(700, Math.max(520, width - 56));
        panelH = Math.min(422, Math.max(350, height - 50));
        sidebarW = Math.min(154, Math.max(132, panelW / 5));
        panelX = Math.max(18, (width - panelW) / 2 - Math.min(66, width / 16));
        panelY = Math.max(16, (height - panelH) / 2);
    }

    private List<Row> visibleRows() {
        if (search.isBlank()) return new ArrayList<>(rows);
        String q = search.toLowerCase(Locale.ROOT);
        List<Row> out = new ArrayList<>();
        for (Row row : rows) {
            if (row.name.toLowerCase(Locale.ROOT).contains(q) || row.description.toLowerCase(Locale.ROOT).contains(q)) out.add(row);
        }
        return out;
    }

    private void rebuildRows() {
        rows.clear();
        SpatialConfig c = SpatialConfig.get();
        switch (section) {
            case VISUALS -> {
                rows.add(toggle("Custom Crosshair", "Clean local crosshair replacement with three styles", () -> c.customCrosshair, v -> c.customCrosshair = v));
                rows.add(cycle("Crosshair Style", "Switch between cross, dot and bracket shapes", () -> pretty(c.crosshairStyle.name()), () -> c.crosshairStyle = next(c.crosshairStyle, SpatialConfig.CrosshairStyle.values())));
            }
            case HUD -> {
                rows.add(toggle("FPS Counter", "Small rounded performance counter", () -> c.fpsHud, v -> c.fpsHud = v));
                rows.add(toggle("Ping Counter", "Current multiplayer latency", () -> c.pingHud, v -> c.pingHud = v));
                rows.add(toggle("Coordinates", "Compact XYZ display", () -> c.coordinatesHud, v -> c.coordinatesHud = v));
                rows.add(toggle("Keystrokes", "WASD and mouse input display", () -> c.keystrokesHud, v -> c.keystrokesHud = v));
                rows.add(toggle("Armor HUD", "Armor items above the hotbar", () -> c.armorHud, v -> c.armorHud = v));
                rows.add(toggle("Effect List", "Active effects with clean timing text", () -> c.effectsHud, v -> c.effectsHud = v));
            }
            case COSMETICS -> {
                rows.add(toggle("Player Aura", "Animated client-side ribbons around your own body", () -> c.aura, v -> c.aura = v));
                rows.add(cycle("Aura Style", "Flowing orbit, vertical helix or halo flow", () -> pretty(c.auraStyle.name()), () -> c.auraStyle = next(c.auraStyle, SpatialConfig.AuraStyle.values())));
                rows.add(cycle("Aura Intensity", "Controls how many animated aura layers render", () -> Integer.toString(c.auraIntensity), () -> c.auraIntensity = c.auraIntensity >= 3 ? 1 : c.auraIntensity + 1));
            }
            case CAMERA -> {
                rows.add(toggle("Head Follow", "Your rendered skin looks toward the cursor", () -> c.headFollow, v -> c.headFollow = v));
                rows.add(toggle("Cursor Parallax", "Very small menu camera response to mouse movement", () -> c.menuParallax, v -> c.menuParallax = v));
                rows.add(toggle("Camera Collision", "Prevents the spatial camera from entering blocks", () -> c.cameraCollision, v -> c.cameraCollision = v));
                rows.add(cycle("Camera Motion", "Choose soft, balanced or snappy interpolation", () -> smoothingLabel(c.cameraSmoothness), () -> c.cameraSmoothness = nextSmoothness(c.cameraSmoothness)));
            }
            case SETTINGS -> {
                rows.add(cycle("Accent Color", "Changes the client accent without changing the layout", () -> accentName(c.accentRgb), () -> c.accentRgb = nextAccent(c.accentRgb)));
                rows.add(cycle("Reset Visuals", "Restores every visual setting to the defaults", () -> "RESET", SpatialMenuScreen::resetVisuals));
            }
            default -> {}
        }
    }

    private static int countFor(SpatialSection s) {
        return switch (s) {
            case VISUALS -> 2;
            case HUD -> 6;
            case COSMETICS -> 3;
            case CAMERA -> 4;
            case SETTINGS -> 2;
            default -> 0;
        };
    }

    private static String pretty(String s) {
        String lower = s.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void resetVisuals() {
        SpatialConfig c = SpatialConfig.get();
        c.fpsHud = true;
        c.pingHud = true;
        c.coordinatesHud = true;
        c.keystrokesHud = true;
        c.armorHud = true;
        c.effectsHud = true;
        c.customCrosshair = true;
        c.aura = true;
        c.headFollow = true;
        c.menuParallax = true;
        c.cameraCollision = true;
        c.auraStyle = SpatialConfig.AuraStyle.ORBIT;
        c.crosshairStyle = SpatialConfig.CrosshairStyle.CROSS;
        c.auraIntensity = 2;
        c.cameraSmoothness = 0.18F;
        c.accentRgb = 0x7700FF;
    }

    private static float nextSmoothness(float current) {
        if (current < 0.14F) return 0.18F;
        if (current < 0.24F) return 0.30F;
        return 0.10F;
    }

    private static String smoothingLabel(float v) {
        if (v < 0.14F) return "Soft";
        if (v < 0.24F) return "Balanced";
        return "Snappy";
    }

    private static int nextAccent(int rgb) {
        if (rgb == 0x7700FF) return 0x8066FF;
        if (rgb == 0x8066FF) return 0x00A8FF;
        if (rgb == 0x00A8FF) return 0x00D6A3;
        if (rgb == 0x00D6A3) return 0xFF4D8D;
        return 0x7700FF;
    }

    private static String accentName(int rgb) {
        return switch (rgb) {
            case 0x7700FF -> "Violet";
            case 0x8066FF -> "Lavender";
            case 0x00A8FF -> "Ocean";
            case 0x00D6A3 -> "Mint";
            case 0xFF4D8D -> "Rose";
            default -> "Custom";
        };
    }

    private static <E extends Enum<E>> E next(E value, E[] all) {
        return all[(value.ordinal() + 1) % all.length];
    }

    private static Row toggle(String name, String desc, BoolGet get, BoolSet set) {
        return new Row(name, desc, RowKind.TOGGLE, () -> get.get() ? "ON" : "OFF", get, () -> set.set(!get.get()));
    }

    private static Row cycle(String name, String desc, TextGet value, Runnable action) {
        return new Row(name, desc, RowKind.CYCLE, value, () -> true, action);
    }

    private static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private interface BoolGet { boolean get(); }
    private interface BoolSet { void set(boolean v); }
    private interface TextGet { String get(); }
    private enum RowKind { TOGGLE, CYCLE }
    private record Row(String name, String description, RowKind kind, TextGet value, BoolGet active, Runnable action) {}
}
