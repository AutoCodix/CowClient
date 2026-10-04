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
    private int rowScroll;

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

        SpatialConfig cfg = SpatialConfig.get();
        int accent = 0xFF000000 | cfg.accentRgb;
        int radius = cfg.menuRadius;
        int panelAlpha = Math.round(cfg.menuOpacity / 100.0F * 245.0F);
        int sideAlpha = Math.round(cfg.menuOpacity / 100.0F * 250.0F);
        UiRenderer.shadow(g, panelX, panelY, panelW, panelH, radius);
        UiRenderer.roundedRect(g, panelX, panelY, panelW, panelH, radius, (panelAlpha << 24) | 0x0D0F23);

        UiRenderer.roundedRect(g, panelX + 1, panelY + 1, sidebarW, panelH - 2, Math.max(7, radius - 1), (sideAlpha << 24) | 0x070919);
        g.fill(panelX + sidebarW - radius, panelY + 1, panelX + sidebarW + 1, panelY + panelH - 1, (sideAlpha << 24) | 0x070919);
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
        int capacity = Math.max(1, (panelY + panelH - 14 - y) / (rowH + gap));
        int maxScroll = Math.max(0, visible.size() - capacity);
        rowScroll = Math.max(0, Math.min(rowScroll, maxScroll));
        int first = rowScroll;
        for (int i = first; i < visible.size(); i++) {
            Row row = visible.get(i);
            int ry = y + (i - first) * (rowH + gap);
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
                drawChevron(g, Math.round(right - pillW - 25), ry + 20, 0xFF7D839F);
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
        int capacity = Math.max(1, (panelY + panelH - 14 - ry) / (rowH + gap));
        int first = Math.min(rowScroll, Math.max(0, visible.size() - capacity));
        for (int i = first; i < visible.size(); i++) {
            int yy = ry + (i - first) * (rowH + gap);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<Row> visible = visibleRows();
        int rowH = 47;
        int gap = 7;
        int y = panelY + 98;
        int capacity = Math.max(1, (panelY + panelH - 14 - y) / (rowH + gap));
        int max = Math.max(0, visible.size() - capacity);
        rowScroll = Math.max(0, Math.min(max, rowScroll + (delta < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && codePoint >= 32 && codePoint <= 126 && search.length() < 28) {
            search += codePoint;
            rowScroll = 0;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                rowScroll = 0;
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
        rowScroll = 0;
        SpatialCamera.setSection(s);
        rebuildRows();
    }

    private void layout() {
        float scale = SpatialConfig.get().menuScale;
        panelW = Math.min(width - 28, Math.round(Math.min(700, Math.max(520, width - 56)) * scale));
        panelH = Math.min(height - 24, Math.round(Math.min(422, Math.max(350, height - 50)) * scale));
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
                rows.add(toggle("Custom Crosshair", "Replaces only your local crosshair; never changes server aim or reach.", () -> c.customCrosshair, v -> c.customCrosshair = v));
                rows.add(cycle("Crosshair Style", "Cross, dot or bracket shape.", () -> pretty(c.crosshairStyle.name()), () -> c.crosshairStyle = next(c.crosshairStyle, SpatialConfig.CrosshairStyle.values())));
                rows.add(cycle("Crosshair Size", "Changes the arm length / dot radius.", () -> c.crosshairSize + "px", () -> c.crosshairSize = nextInt(c.crosshairSize, 3, 14, 1)));
                rows.add(cycle("Crosshair Gap", "Controls empty space around the exact center.", () -> c.crosshairGap + "px", () -> c.crosshairGap = nextInt(c.crosshairGap, 0, 8, 1)));
                rows.add(cycle("Crosshair Thickness", "Makes crosshair strokes thinner or thicker.", () -> c.crosshairThickness + "px", () -> c.crosshairThickness = nextInt(c.crosshairThickness, 1, 4, 1)));
                rows.add(cycle("Crosshair Opacity", "Controls crosshair transparency.", () -> c.crosshairOpacity + "%", () -> c.crosshairOpacity = nextInt(c.crosshairOpacity, 20, 100, 5)));
                rows.add(toggle("Crosshair Outline", "Adds a dark outline so the crosshair stays readable on bright blocks.", () -> c.crosshairOutline, v -> c.crosshairOutline = v));
                rows.add(toggle("Crosshair Accent", "Use the client accent color instead of a separate crosshair color.", () -> c.crosshairUseAccent, v -> c.crosshairUseAccent = v));
                rows.add(cycle("Crosshair Color", "Separate local color used when Accent is off.", () -> accentName(c.crosshairColorRgb), () -> c.crosshairColorRgb = nextAccent(c.crosshairColorRgb)));
            }
            case HUD -> {
                rows.add(toggle("FPS Counter", "Live local framerate pill.", () -> c.fpsHud, v -> c.fpsHud = v));
                rows.add(cycle("FPS Scale", "Size of only the FPS widget.", () -> scaleLabel(c.fpsScale), () -> c.fpsScale = nextFloat(c.fpsScale, .70F, 1.55F, .05F)));
                rows.add(cycle("FPS Opacity", "Background transparency of the FPS widget.", () -> pct(c.fpsOpacity), () -> c.fpsOpacity = nextFloat(c.fpsOpacity, .20F, 1.0F, .10F)));
                rows.add(cycle("FPS Corner", "Choose which screen corner owns the FPS widget.", () -> pretty(c.fpsCorner.name()), () -> c.fpsCorner = next(c.fpsCorner, SpatialConfig.HudCorner.values())));
                rows.add(toggle("FPS Accent Bar", "Show the small theme-colored marker.", () -> c.fpsAccentBar, v -> c.fpsAccentBar = v));

                rows.add(toggle("Ping Counter", "Shows your current multiplayer latency locally.", () -> c.pingHud, v -> c.pingHud = v));
                rows.add(cycle("Ping Scale", "Size of only the ping widget.", () -> scaleLabel(c.pingScale), () -> c.pingScale = nextFloat(c.pingScale, .70F, 1.55F, .05F)));
                rows.add(cycle("Ping Opacity", "Background transparency of the ping widget.", () -> pct(c.pingOpacity), () -> c.pingOpacity = nextFloat(c.pingOpacity, .20F, 1.0F, .10F)));
                rows.add(cycle("Ping Corner", "Choose the ping widget anchor corner.", () -> pretty(c.pingCorner.name()), () -> c.pingCorner = next(c.pingCorner, SpatialConfig.HudCorner.values())));
                rows.add(toggle("Ping Accent Bar", "Show the theme marker on the ping widget.", () -> c.pingAccentBar, v -> c.pingAccentBar = v));

                rows.add(toggle("Coordinates", "Displays your local XYZ position.", () -> c.coordinatesHud, v -> c.coordinatesHud = v));
                rows.add(cycle("Coordinates Scale", "Independent size for XYZ.", () -> scaleLabel(c.coordinatesScale), () -> c.coordinatesScale = nextFloat(c.coordinatesScale, .70F, 1.55F, .05F)));
                rows.add(cycle("Coordinates Opacity", "XYZ background transparency.", () -> pct(c.coordinatesOpacity), () -> c.coordinatesOpacity = nextFloat(c.coordinatesOpacity, .20F, 1.0F, .10F)));
                rows.add(cycle("Coordinates Corner", "Move XYZ to any screen corner.", () -> pretty(c.coordinatesCorner.name()), () -> c.coordinatesCorner = next(c.coordinatesCorner, SpatialConfig.HudCorner.values())));
                rows.add(toggle("Decimal Coordinates", "Show one decimal place instead of block coordinates.", () -> c.decimalCoordinates, v -> c.decimalCoordinates = v));
                rows.add(toggle("Coordinates Accent", "Show the accent marker beside XYZ.", () -> c.coordinatesAccentBar, v -> c.coordinatesAccentBar = v));

                rows.add(toggle("Keystrokes", "Animated WASD display using your real local input.", () -> c.keystrokesHud, v -> c.keystrokesHud = v));
                rows.add(cycle("Keystrokes Scale", "Resize the full key cluster.", () -> scaleLabel(c.keystrokesScale), () -> c.keystrokesScale = nextFloat(c.keystrokesScale, .70F, 1.55F, .05F)));
                rows.add(cycle("Keystrokes Opacity", "Transparency of inactive keys.", () -> pct(c.keystrokesOpacity), () -> c.keystrokesOpacity = nextFloat(c.keystrokesOpacity, .20F, 1.0F, .10F)));
                rows.add(cycle("Keystrokes Corner", "Anchor WASD in any corner.", () -> pretty(c.keystrokesCorner.name()), () -> c.keystrokesCorner = next(c.keystrokesCorner, SpatialConfig.HudCorner.values())));
                rows.add(toggle("Mouse Buttons", "Include LMB/RMB below the WASD keys.", () -> c.keystrokesMouseButtons, v -> c.keystrokesMouseButtons = v));

                rows.add(toggle("Armor HUD", "Shows your equipped armor as a compact local overlay.", () -> c.armorHud, v -> c.armorHud = v));
                rows.add(cycle("Armor Scale", "Resize armor item icons.", () -> scaleLabel(c.armorScale), () -> c.armorScale = nextFloat(c.armorScale, .70F, 1.55F, .05F)));
                rows.add(cycle("Armor Anchor", "Above hotbar, top center or bottom right.", () -> pretty(c.armorAnchor.name()), () -> c.armorAnchor = next(c.armorAnchor, SpatialConfig.ArmorAnchor.values())));
                rows.add(toggle("Armor Durability", "Keep vanilla durability/count decorations on armor icons.", () -> c.armorDurability, v -> c.armorDurability = v));

                rows.add(toggle("Effect List", "Lists active potion/status effects locally.", () -> c.effectsHud, v -> c.effectsHud = v));
                rows.add(cycle("Effects Scale", "Resize the effect list.", () -> scaleLabel(c.effectsScale), () -> c.effectsScale = nextFloat(c.effectsScale, .70F, 1.55F, .05F)));
                rows.add(cycle("Effects Opacity", "Effect-card background transparency.", () -> pct(c.effectsOpacity), () -> c.effectsOpacity = nextFloat(c.effectsOpacity, .20F, 1.0F, .10F)));
                rows.add(cycle("Effects Corner", "Choose where the list grows from.", () -> pretty(c.effectsCorner.name()), () -> c.effectsCorner = next(c.effectsCorner, SpatialConfig.HudCorner.values())));
                rows.add(toggle("Effect Durations", "Show remaining time beside every effect.", () -> c.effectsDuration, v -> c.effectsDuration = v));
                rows.add(cycle("Effect Sort", "Sort effects by remaining duration or alphabetically.", () -> pretty(c.effectsSort.name()), () -> c.effectsSort = next(c.effectsSort, SpatialConfig.EffectSort.values())));
            }
            case COSMETICS -> {
                rows.add(toggle("Player Aura", "Curved animated ribbons orbit only your own rendered player.", () -> c.aura, v -> c.aura = v));
                rows.add(cycle("Aura Style", "Orbit rings, vertical helix or diagonal flowing ribbon.", () -> pretty(c.auraStyle.name()), () -> c.auraStyle = next(c.auraStyle, SpatialConfig.AuraStyle.values())));
                rows.add(cycle("Aura Layers", "How many independent animated ribbon layers are rendered.", () -> Integer.toString(c.auraIntensity), () -> c.auraIntensity = nextInt(c.auraIntensity, 1, 4, 1)));
                rows.add(cycle("Aura Speed", "How quickly the ribbons rotate around your body.", () -> speedLabel(c.auraSpeed), () -> c.auraSpeed = nextFloat(c.auraSpeed, .25F, 2.50F, .25F)));
                rows.add(cycle("Aura Radius", "Distance of the ribbon from the player model.", () -> decimal(c.auraRadius), () -> c.auraRadius = nextFloat(c.auraRadius, .30F, .90F, .05F)));
                rows.add(cycle("Aura Height", "Vertical span used by helix/ribbon styles.", () -> decimal(c.auraHeight), () -> c.auraHeight = nextFloat(c.auraHeight, .70F, 2.35F, .10F)));
                rows.add(cycle("Aura Width", "Thickness of the curved ribbon geometry.", () -> decimal(c.auraWidth), () -> c.auraWidth = nextFloat(c.auraWidth, .015F, .12F, .015F)));
                rows.add(cycle("Aura Wave", "How strongly the aura curves and waves while spinning.", () -> decimal(c.auraWave), () -> c.auraWave = nextFloat(c.auraWave, 0.0F, .30F, .025F)));
                rows.add(cycle("Aura Opacity", "Transparency of the animated aura.", () -> pct(c.auraOpacity), () -> c.auraOpacity = nextFloat(c.auraOpacity, .15F, 1.0F, .10F)));
                rows.add(toggle("Aura Accent", "Use the main client accent for the aura.", () -> c.auraUseAccent, v -> c.auraUseAccent = v));
                rows.add(cycle("Aura Color", "Independent aura color when Accent is disabled.", () -> accentName(c.auraColorRgb), () -> c.auraColorRgb = nextAccent(c.auraColorRgb)));

                rows.add(toggle("Halo", "Separate animated halo above your skin; completely client-side.", () -> c.halo, v -> c.halo = v));
                rows.add(cycle("Halo Style", "Clean ring, double ring or crossed orbital rings.", () -> pretty(c.haloStyle.name()), () -> c.haloStyle = next(c.haloStyle, SpatialConfig.HaloStyle.values())));
                rows.add(cycle("Halo Speed", "Spin speed of the halo geometry.", () -> speedLabel(c.haloSpeed), () -> c.haloSpeed = nextFloat(c.haloSpeed, 0.0F, 2.50F, .25F)));
                rows.add(cycle("Halo Radius", "Overall halo diameter around your head.", () -> decimal(c.haloRadius), () -> c.haloRadius = nextFloat(c.haloRadius, .24F, .75F, .04F)));
                rows.add(cycle("Halo Height", "Raises or lowers the halo above the player.", () -> decimal(c.haloHeight), () -> c.haloHeight = nextFloat(c.haloHeight, 1.55F, 2.55F, .05F)));
                rows.add(cycle("Halo Width", "Ring thickness.", () -> decimal(c.haloWidth), () -> c.haloWidth = nextFloat(c.haloWidth, .012F, .10F, .011F)));
                rows.add(cycle("Halo Tilt", "Tilts the ring plane for a more 3D look.", () -> Math.round(c.haloTilt) + " deg", () -> c.haloTilt = nextFloat(c.haloTilt, -45.0F, 45.0F, 5.0F)));
                rows.add(cycle("Halo Opacity", "Transparency of the halo.", () -> pct(c.haloOpacity), () -> c.haloOpacity = nextFloat(c.haloOpacity, .15F, 1.0F, .10F)));
                rows.add(toggle("Halo Accent", "Use the client accent color for the halo.", () -> c.haloUseAccent, v -> c.haloUseAccent = v));
                rows.add(cycle("Halo Color", "Independent halo color when Accent is off.", () -> accentName(c.haloColorRgb), () -> c.haloColorRgb = nextAccent(c.haloColorRgb)));
            }
            case CAMERA -> {
                rows.add(toggle("Head Follow", "Your rendered skin head follows the cursor while the spatial menu is open.", () -> c.headFollow, v -> c.headFollow = v));
                rows.add(toggle("Face Camera", "Render-only body rotation keeps your skin facing the orbiting camera.", () -> c.bodyFacesCamera, v -> c.bodyFacesCamera = v));
                rows.add(cycle("Head Yaw Range", "Maximum left/right head-follow angle.", () -> Math.round(c.headFollowYaw) + " deg", () -> c.headFollowYaw = nextFloat(c.headFollowYaw, 5.0F, 60.0F, 5.0F)));
                rows.add(cycle("Head Pitch Range", "Maximum up/down head-follow angle.", () -> Math.round(c.headFollowPitch) + " deg", () -> c.headFollowPitch = nextFloat(c.headFollowPitch, 4.0F, 32.0F, 4.0F)));
                rows.add(cycle("Head Follow Speed", "How quickly the head eases toward your cursor.", () -> decimal(c.headFollowSpeed), () -> c.headFollowSpeed = nextFloat(c.headFollowSpeed, .05F, .50F, .05F)));
                rows.add(toggle("Cursor Parallax", "Small camera response to cursor movement without changing the base shot.", () -> c.menuParallax, v -> c.menuParallax = v));
                rows.add(cycle("Parallax Strength", "Controls only the tiny cursor camera offset.", () -> decimal(c.parallaxStrength), () -> c.parallaxStrength = nextFloat(c.parallaxStrength, 0.0F, .18F, .02F)));
                rows.add(toggle("Camera Collision", "Stops the spatial camera from clipping inside blocks.", () -> c.cameraCollision, v -> c.cameraCollision = v));
                rows.add(cycle("Collision Padding", "Distance kept away from the hit wall.", () -> decimal(c.collisionPadding), () -> c.collisionPadding = nextFloat(c.collisionPadding, .05F, .45F, .05F)));
                rows.add(cycle("Camera Smoothness", "Easing strength for the spatial camera without changing its base placement.", () -> decimal(c.cameraSmoothness), () -> c.cameraSmoothness = nextFloat(c.cameraSmoothness, .08F, .40F, .04F)));
                rows.add(cycle("Tab Orbit Amount", "How much tab changes orbit around you; 1.0 uses the full choreography.", () -> decimal(c.tabOrbitStrength), () -> c.tabOrbitStrength = nextFloat(c.tabOrbitStrength, .25F, 1.35F, .10F)));
                rows.add(cycle("Tab Vertical Amount", "How strongly tabs travel upward/downward.", () -> decimal(c.tabVerticalStrength), () -> c.tabVerticalStrength = nextFloat(c.tabVerticalStrength, 0.0F, 1.50F, .10F)));
                rows.add(cycle("Tab Transition Speed", "Speed of the smooth 3D orbit between categories.", () -> speedLabel(c.tabTransitionSpeed), () -> c.tabTransitionSpeed = nextFloat(c.tabTransitionSpeed, .55F, 1.80F, .10F)));
            }
            case SETTINGS -> {
                rows.add(cycle("Accent Color", "Master accent used by GUI, HUD, aura and halo when linked.", () -> accentName(c.accentRgb), () -> c.accentRgb = nextAccent(c.accentRgb)));
                rows.add(cycle("Menu Opacity", "Overall transparency of the rounded menu surface.", () -> c.menuOpacity + "%", () -> c.menuOpacity = nextInt(c.menuOpacity, 70, 100, 5)));
                rows.add(cycle("Menu Scale", "Resize the complete Prestige-style client panel.", () -> scaleLabel(c.menuScale), () -> c.menuScale = nextFloat(c.menuScale, .85F, 1.15F, .05F)));
                rows.add(cycle("Corner Radius", "Roundness of the main menu shell.", () -> c.menuRadius + "px", () -> c.menuRadius = nextInt(c.menuRadius, 8, 20, 2)));
                rows.add(cycle("Reset Everything", "Restore every Spatial visual, HUD, cosmetic and camera preference.", () -> "RESET", SpatialMenuScreen::resetVisuals));
            }
            default -> {}
        }
    }

    private static int countFor(SpatialSection s) {
        return switch (s) {
            case VISUALS -> 9;
            case HUD -> 31;
            case COSMETICS -> 22;
            case CAMERA -> 13;
            case SETTINGS -> 5;
            default -> 0;
        };
    }

    private static String pretty(String s) {
        String lower = s.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] words = lower.split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static int nextInt(int value, int min, int max, int step) {
        int next = value + step;
        return next > max ? min : next;
    }

    private static float nextFloat(float value, float min, float max, float step) {
        float next = Math.round((value + step) * 1000.0F) / 1000.0F;
        return next > max + 0.0001F ? min : next;
    }

    private static String pct(float v) {
        return Math.round(v * 100.0F) + "%";
    }

    private static String decimal(float v) {
        String text = String.format(Locale.ROOT, "%.3f", v);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String scaleLabel(float v) {
        return Math.round(v * 100.0F) + "%";
    }

    private static String speedLabel(float v) {
        return decimal(v) + "x";
    }

    private static void resetVisuals() {
        SpatialConfig c = SpatialConfig.get();
        c.customCrosshair = true; c.crosshairStyle = SpatialConfig.CrosshairStyle.CROSS; c.crosshairSize = 7; c.crosshairGap = 1;
        c.crosshairThickness = 1; c.crosshairOpacity = 100; c.crosshairOutline = true; c.crosshairUseAccent = true; c.crosshairColorRgb = 0xFFFFFF;
        c.fpsHud = true; c.fpsScale = 1; c.fpsOpacity = .72F; c.fpsCorner = SpatialConfig.HudCorner.TOP_LEFT; c.fpsAccentBar = true;
        c.pingHud = true; c.pingScale = 1; c.pingOpacity = .72F; c.pingCorner = SpatialConfig.HudCorner.TOP_LEFT; c.pingAccentBar = true;
        c.coordinatesHud = true; c.coordinatesScale = 1; c.coordinatesOpacity = .72F; c.coordinatesCorner = SpatialConfig.HudCorner.TOP_LEFT; c.decimalCoordinates = false; c.coordinatesAccentBar = true;
        c.keystrokesHud = true; c.keystrokesScale = 1; c.keystrokesOpacity = .72F; c.keystrokesCorner = SpatialConfig.HudCorner.BOTTOM_LEFT; c.keystrokesMouseButtons = true;
        c.armorHud = true; c.armorScale = 1; c.armorAnchor = SpatialConfig.ArmorAnchor.ABOVE_HOTBAR; c.armorDurability = true;
        c.effectsHud = true; c.effectsScale = 1; c.effectsOpacity = .72F; c.effectsCorner = SpatialConfig.HudCorner.TOP_RIGHT; c.effectsDuration = true; c.effectsSort = SpatialConfig.EffectSort.DURATION;
        c.aura = true; c.auraStyle = SpatialConfig.AuraStyle.ORBIT; c.auraIntensity = 2; c.auraSpeed = 1; c.auraRadius = .52F; c.auraHeight = 1.75F; c.auraWidth = .055F; c.auraWave = .12F; c.auraOpacity = .72F; c.auraUseAccent = true; c.auraColorRgb = 0x7700FF;
        c.halo = true; c.haloStyle = SpatialConfig.HaloStyle.CLEAN_RING; c.haloSpeed = .8F; c.haloRadius = .43F; c.haloHeight = 2.02F; c.haloWidth = .04F; c.haloTilt = 11; c.haloOpacity = .72F; c.haloUseAccent = true; c.haloColorRgb = 0x7700FF;
        c.headFollow = true; c.headFollowYaw = 28; c.headFollowPitch = 16; c.headFollowSpeed = .18F; c.bodyFacesCamera = true;
        c.menuParallax = true; c.parallaxStrength = .09F; c.cameraCollision = true; c.collisionPadding = .20F; c.cameraSmoothness = .18F; c.tabOrbitStrength = 1; c.tabVerticalStrength = 1; c.tabTransitionSpeed = 1;
        c.accentRgb = 0x7700FF; c.menuOpacity = 95; c.menuScale = 1; c.menuRadius = 15;
    }

    private static int nextAccent(int rgb) {
        if (rgb == 0x7700FF) return 0x8066FF;
        if (rgb == 0x8066FF) return 0x00A8FF;
        if (rgb == 0x00A8FF) return 0x00D6A3;
        if (rgb == 0x00D6A3) return 0xFF4D8D;
        if (rgb == 0xFF4D8D) return 0xFFB020;
        if (rgb == 0xFFB020) return 0xFFFFFF;
        return 0x7700FF;
    }

    private static String accentName(int rgb) {
        return switch (rgb) {
            case 0x7700FF -> "Violet";
            case 0x8066FF -> "Lavender";
            case 0x00A8FF -> "Ocean";
            case 0x00D6A3 -> "Mint";
            case 0xFF4D8D -> "Rose";
            case 0xFFB020 -> "Amber";
            case 0xFFFFFF -> "White";
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
