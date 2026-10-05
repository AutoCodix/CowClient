package dev.spatialclient.client;

import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpatialMenuScreen extends Screen {
    private static final SpatialSection[] MODULE_SECTIONS = {
            SpatialSection.VISUALS, SpatialSection.HUD, SpatialSection.COSMETICS, SpatialSection.CAMERA
    };

    private SpatialSection section = SpatialSection.VISUALS;
    private final List<Row> rows = new ArrayList<>();
    private final List<String> groups = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sidebarW;
    private int groupW;

    private int searchX;
    private int searchY;
    private int searchW;
    private boolean searchFocused;
    private String search = "";

    private int selectedGroup;
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
        rebuildGroups();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SpatialCamera.setMouse(mouseX, mouseY, width, height);
        HeadTracking.updateMouse(mouseX, mouseY, width, height);
        layout();

        SpatialConfig cfg = SpatialConfig.get();
        int accent = 0xFF000000 | cfg.accentRgb;
        int radius = Math.max(14, cfg.menuRadius);

        UiRenderer.shadow(g, panelX, panelY, panelW, panelH, radius);
        UiRenderer.roundedRect(g, panelX, panelY, panelW, panelH, radius, 0xD60A0E1C);
        SpatialMenuTheme.drawPanelSpace(g, panelX + 2, panelY + 2, panelW - 4, panelH - 4, mouseX, mouseY);
        UiRenderer.roundedOutline(g, panelX, panelY, panelW, panelH, radius, 1, 0xFF263553, 0x08000000);

        UiRenderer.roundedRect(g, panelX + 1, panelY + 1, sidebarW, panelH - 2, radius - 1, 0xD8060914);
        g.fill(panelX + sidebarW - radius, panelY + 1, panelX + sidebarW + 1, panelY + panelH - 1, 0xD8060914);
        g.fill(panelX + sidebarW, panelY + 17, panelX + sidebarW + 1, panelY + panelH - 17, 0x66334462);

        drawBrand(g);
        drawSidebar(g, mouseX, mouseY, accent);
        drawContent(g, mouseX, mouseY, accent);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBrand(GuiGraphics g) {
        int bx = panelX + 11;
        int by = panelY + 10;
        SpatialMenuTheme.drawLogo(g, bx, by, 38);
        UiFont.draw(g, "SPATIAL", panelX + 57, panelY + 13, 0xFFF5F8FF, 0.36F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "FORGE 1.20.1", panelX + 57, panelY + 29, 0xFF69799B, 0.205F, UiFont.Weight.REGULAR);
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY, int accent) {
        int y = panelY + 62;
        UiFont.draw(g, "MODULES", panelX + 16, y, 0xFF53617D, 0.205F, UiFont.Weight.SEMIBOLD);
        y += 16;

        for (SpatialSection s : MODULE_SECTIONS) {
            boolean selected = section == s;
            boolean hover = inside(mouseX, mouseY, panelX + 10, y - 2, sidebarW - 20, 30);

            if (selected || hover) {
                UiRenderer.roundedRect(g, panelX + 9, y - 3, sidebarW - 18, 31, 9,
                        selected ? 0xFF251C48 : 0x9A11192B);
            }
            if (selected) UiRenderer.roundedRect(g, panelX + 12, y + 5, 3, 14, 2, accent);

            drawNavIcon(g, panelX + 25, y + 11, s, selected ? accent : 0xFF72809D);
            UiFont.draw(g, s.title, panelX + 40, y + 5,
                    selected ? 0xFFF5F7FF : 0xFFA1ACC4,
                    0.265F, selected ? UiFont.Weight.SEMIBOLD : UiFont.Weight.REGULAR);
            y += 36;
        }

        y += 7;
        UiFont.draw(g, "GENERAL", panelX + 16, y, 0xFF53617D, 0.205F, UiFont.Weight.SEMIBOLD);
        y += 17;

        boolean settings = section == SpatialSection.SETTINGS;
        boolean hover = inside(mouseX, mouseY, panelX + 10, y - 2, sidebarW - 20, 30);
        if (settings || hover) {
            UiRenderer.roundedRect(g, panelX + 9, y - 3, sidebarW - 18, 31, 9,
                    settings ? 0xFF251C48 : 0x9A11192B);
        }
        if (settings) UiRenderer.roundedRect(g, panelX + 12, y + 5, 3, 14, 2, accent);
        drawNavIcon(g, panelX + 25, y + 11, SpatialSection.SETTINGS, settings ? accent : 0xFF72809D);
        UiFont.draw(g, "Settings", panelX + 40, y + 5,
                settings ? 0xFFF5F7FF : 0xFFA1ACC4,
                0.265F, settings ? UiFont.Weight.SEMIBOLD : UiFont.Weight.REGULAR);

        UiFont.draw(g, "RSHIFT  close", panelX + 16, panelY + panelH - 23,
                0xFF52607D, 0.205F, UiFont.Weight.REGULAR);
    }

    private void drawContent(GuiGraphics g, int mouseX, int mouseY, int accent) {
        int contentX = panelX + sidebarW + 14;
        int right = panelX + panelW - 14;
        int headerY = panelY + 13;

        UiFont.draw(g, section == SpatialSection.SETTINGS ? "Settings" : section.title,
                contentX, headerY, 0xFFF5F7FF, 0.43F, UiFont.Weight.SEMIBOLD);

        String subtitle = section == SpatialSection.SETTINGS
                ? "Client, configs and appearance"
                : "Visual-only / client-side";
        UiFont.draw(g, subtitle, contentX, headerY + 21,
                0xFF69799A, 0.215F, UiFont.Weight.REGULAR);

        searchW = Math.min(150, Math.max(116, panelW / 4));
        searchX = right - searchW;
        searchY = panelY + 12;
        UiRenderer.roundedOutline(g, searchX, searchY, searchW, 27, 9, 1,
                searchFocused ? 0xFF55478E : 0xFF26324D, 0xC9090D1B);
        UiRenderer.circle(g, searchX + 13, searchY + 13, 4.0F, 0xFF7B88A7);
        UiRenderer.circle(g, searchX + 13, searchY + 13, 2.4F, 0xFF090D1B);
        g.fill(searchX + 16, searchY + 16, searchX + 20, searchY + 18, 0xFF7B88A7);
        UiFont.draw(g, search.isEmpty() ? "Search" : search, searchX + 25, searchY + 7,
                search.isEmpty() ? 0xFF667391 : 0xFFE8EEF9, 0.225F, UiFont.Weight.REGULAR);

        int bodyY = panelY + 55;
        int bodyH = panelY + panelH - 14 - bodyY;

        groupW = Math.min(146, Math.max(126, (right - contentX) / 3));
        drawGroupRail(g, contentX, bodyY, groupW, bodyH, mouseX, mouseY, accent);

        int settingsX = contentX + groupW + 10;
        int settingsW = right - settingsX;
        drawGroupSettings(g, settingsX, bodyY, settingsW, bodyH, mouseX, mouseY, accent);
    }

    private void drawGroupRail(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, int accent) {
        UiRenderer.roundedRect(g, x, y, w, h, 11, 0xA70A1020);

        UiFont.draw(g, "MODULES", x + 11, y + 10, 0xFF596785, 0.195F, UiFont.Weight.SEMIBOLD);

        int yy = y + 29;
        for (int i = 0; i < groups.size(); i++) {
            String group = groups.get(i);
            boolean selected = i == selectedGroup;
            boolean hover = inside(mouseX, mouseY, x + 7, yy, w - 14, 31);

            if (selected || hover) {
                UiRenderer.roundedRect(g, x + 7, yy, w - 14, 31, 9,
                        selected ? 0xFF1E2947 : 0x8C151D31);
            }
            if (selected) {
                UiRenderer.roundedRect(g, x + 10, yy + 7, 3, 17, 2, accent);
            }

            int enabled = enabledCountForGroup(group);
            UiFont.draw(g, group, x + 20, yy + 7,
                    selected ? 0xFFF3F6FF : 0xFFA7B1C8,
                    0.245F, selected ? UiFont.Weight.SEMIBOLD : UiFont.Weight.REGULAR);

            String n = Integer.toString(enabled);
            float nw = UiFont.width(n, 0.19F, UiFont.Weight.REGULAR);
            UiFont.draw(g, n, x + w - nw - 16, yy + 9, 0xFF697897, 0.19F, UiFont.Weight.REGULAR);
            yy += 36;

            if (yy + 31 > y + h - 8) break;
        }
    }

    private void drawGroupSettings(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, int accent) {
        UiRenderer.roundedRect(g, x, y, w, h, 11, 0x86080E1D);

        String group = currentGroup();
        UiFont.draw(g, group, x + 12, y + 10, 0xFFF4F7FF, 0.31F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, groupDescription(group), x + 12, y + 28, 0xFF657492, 0.195F, UiFont.Weight.REGULAR);

        List<Row> visible = visibleRowsForCurrentGroup();
        int rowH = 38;
        int gap = 6;
        int startY = y + 51;
        int capacity = Math.max(1, (h - 58 + gap) / (rowH + gap));
        int maxScroll = Math.max(0, visible.size() - capacity);
        rowScroll = Math.max(0, Math.min(rowScroll, maxScroll));

        if (visible.isEmpty()) {
            UiRenderer.roundedOutline(g, x + 8, startY, w - 16, 48, 9, 1, 0xFF26324C, 0x9A0A1020);
            UiFont.draw(g, "No matching settings", x + 19, startY + 10, 0xFFDCE4F5, 0.245F, UiFont.Weight.SEMIBOLD);
            UiFont.draw(g, "Try another search.", x + 19, startY + 27, 0xFF687593, 0.19F, UiFont.Weight.REGULAR);
            return;
        }

        for (int i = rowScroll; i < visible.size(); i++) {
            int slot = i - rowScroll;
            if (slot >= capacity) break;
            Row row = visible.get(i);
            int ry = startY + slot * (rowH + gap);
            boolean hover = inside(mouseX, mouseY, x + 8, ry, w - 16, rowH);

            if (hover) {
                UiRenderer.roundedRect(g, x + 6, ry - 2, w - 12, rowH + 4, 10, 0x173A6EFF);
            }
            UiRenderer.roundedOutline(g, x + 8, ry, w - 16, rowH, 9, 1,
                    hover ? 0xFF3B4B70 : 0xFF25304B,
                    hover ? 0xD811192E : 0xB80B1121);

            int marker = row.kind == RowKind.TOGGLE && row.active.get() ? accent : 0xFF39445F;
            UiRenderer.roundedRect(g, x + 16, ry + 8, 3, rowH - 16, 2, marker);

            UiFont.draw(g, row.name, x + 27, ry + 7, 0xFFF0F4FF, 0.245F, UiFont.Weight.SEMIBOLD);
            String desc = fit(row.description, Math.max(60, w - 126), 0.185F);
            UiFont.draw(g, desc, x + 27, ry + 23, 0xFF65718F, 0.185F, UiFont.Weight.REGULAR);

            if (row.kind == RowKind.TOGGLE) {
                drawToggle(g, x + w - 43, ry + 11, row.active.get(), accent);
            } else {
                String value = fit(row.value.get(), 60, 0.195F);
                float vw = UiFont.width(value, 0.195F, UiFont.Weight.SEMIBOLD);
                float pillW = Math.max(40.0F, Math.min(70.0F, vw + 16.0F));
                UiRenderer.roundedRect(g, x + w - pillW - 16, ry + 8, pillW, 22, 7, 0xFF18213B);
                UiFont.draw(g, value, x + w - pillW - 16 + (pillW - vw) * 0.5F,
                        ry + 14, 0xFFD0DAEF, 0.195F, UiFont.Weight.SEMIBOLD);
            }
        }

        if (maxScroll > 0) {
            int trackY = startY;
            int trackH = capacity * (rowH + gap) - gap;
            int trackX = x + w - 5;
            UiRenderer.roundedRect(g, trackX, trackY, 2, trackH, 1, 0x55384660);
            int thumbH = Math.max(18, Math.round(trackH * (capacity / (float)visible.size())));
            int thumbY = trackY + Math.round((trackH - thumbH) * (rowScroll / (float)maxScroll));
            UiRenderer.roundedRect(g, trackX, thumbY, 2, thumbH, 1, accent);
        }
    }

    private void drawNavIcon(GuiGraphics g, int cx, int cy, SpatialSection s, int color) {
        switch (s) {
            case VISUALS -> {
                UiRenderer.circle(g, cx, cy, 4.5F, color);
                UiRenderer.circle(g, cx, cy, 2.2F, 0xFF080C18);
            }
            case HUD -> {
                UiRenderer.roundedRect(g, cx - 5, cy - 4, 10, 8, 2, color);
                UiRenderer.roundedRect(g, cx - 3, cy - 2, 6, 4, 1, 0xFF080C18);
            }
            case COSMETICS -> {
                UiRenderer.circle(g, cx, cy - 1, 4.0F, color);
                UiRenderer.roundedRect(g, cx - 5, cy + 4, 10, 2, 1, color);
            }
            case CAMERA -> {
                UiRenderer.roundedRect(g, cx - 5, cy - 4, 10, 8, 2, color);
                UiRenderer.circle(g, cx, cy, 2.0F, 0xFF080C18);
            }
            case SETTINGS -> {
                UiRenderer.circle(g, cx, cy, 5, color);
                UiRenderer.circle(g, cx, cy, 2, 0xFF080C18);
            }
            default -> UiRenderer.circle(g, cx, cy, 4, color);
        }
    }

    private void drawToggle(GuiGraphics g, int x, int y, boolean on, int accent) {
        UiRenderer.roundedRect(g, x, y, 27, 16, 8, on ? 0xFF3A2D69 : 0xFF242B3E);
        UiRenderer.circle(g, on ? x + 19.0F : x + 8.0F, y + 8.0F, 5.2F, on ? 0xFFF4F0FF : 0xFF9BA5B9);
        if (on) UiRenderer.roundedRect(g, x + 3, y + 5.5F, 9, 5, 2, accent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        layout();

        if (inside(mouseX, mouseY, searchX, searchY, searchW, 27)) {
            searchFocused = true;
            SpatialMenuTheme.click(1.18F);
            return true;
        }
        searchFocused = false;

        int y = panelY + 78;
        for (SpatialSection s : MODULE_SECTIONS) {
            if (inside(mouseX, mouseY, panelX + 9, y - 3, sidebarW - 18, 31)) {
                SpatialMenuTheme.click(1.10F);
                switchSection(s);
                return true;
            }
            y += 36;
        }
        y += 24;
        if (inside(mouseX, mouseY, panelX + 9, y - 3, sidebarW - 18, 31)) {
            SpatialMenuTheme.click(1.10F);
            switchSection(SpatialSection.SETTINGS);
            return true;
        }

        int contentX = panelX + sidebarW + 14;
        int right = panelX + panelW - 14;
        int bodyY = panelY + 55;
        int bodyH = panelY + panelH - 14 - bodyY;

        int yy = bodyY + 29;
        for (int i = 0; i < groups.size(); i++) {
            if (inside(mouseX, mouseY, contentX + 7, yy, groupW - 14, 31)) {
                selectedGroup = i;
                rowScroll = 0;
                search = "";
                SpatialMenuTheme.click(1.14F);
                return true;
            }
            yy += 36;
            if (yy + 31 > bodyY + bodyH - 8) break;
        }

        int settingsX = contentX + groupW + 10;
        int settingsW = right - settingsX;
        int startY = bodyY + 51;
        int rowH = 38;
        int gap = 6;
        List<Row> visible = visibleRowsForCurrentGroup();
        int capacity = Math.max(1, (bodyH - 58 + gap) / (rowH + gap));

        for (int i = rowScroll; i < visible.size(); i++) {
            int slot = i - rowScroll;
            if (slot >= capacity) break;
            int ry = startY + slot * (rowH + gap);
            if (inside(mouseX, mouseY, settingsX + 8, ry, settingsW - 16, rowH)) {
                Row row = visible.get(i);
                SpatialMenuTheme.click(row.kind == RowKind.TOGGLE ? 1.17F : 1.04F);
                row.action.run();
                SpatialConfig.get().save();
                rebuildRows();
                rebuildGroups();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<Row> visible = visibleRowsForCurrentGroup();
        int bodyY = panelY + 55;
        int bodyH = panelY + panelH - 14 - bodyY;
        int rowH = 38;
        int gap = 6;
        int capacity = Math.max(1, (bodyH - 58 + gap) / (rowH + gap));
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
        selectedGroup = 0;
        SpatialCamera.setSection(s);
        rebuildRows();
        rebuildGroups();
    }

    private void layout() {
        float scale = SpatialConfig.get().menuScale;

        int baseW = Math.min(670, Math.max(590, width * 43 / 100));
        int baseH = Math.min(360, Math.max(324, height - 215));

        panelW = Math.min(width - 42, Math.round(baseW * scale));
        panelH = Math.min(height - 72, Math.round(baseH * scale));
        sidebarW = Math.min(118, Math.max(106, panelW / 6));

        int preferredX = width / 2 + Math.max(45, width / 30);
        panelX = Math.min(width - panelW - 18, Math.max(18, preferredX));
        panelY = Math.min(height - panelH - 24, Math.max(38, (height - panelH) / 2 + 12));
    }

    private void rebuildGroups() {
        String previous = currentGroup();
        groups.clear();

        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (Row row : rows) seen.put(groupFor(row.name), Boolean.TRUE);
        groups.addAll(seen.keySet());

        if (groups.isEmpty()) groups.add("General");
        int oldIndex = groups.indexOf(previous);
        selectedGroup = oldIndex >= 0 ? oldIndex : Math.min(selectedGroup, groups.size() - 1);
        selectedGroup = Math.max(0, selectedGroup);
    }

    private String currentGroup() {
        if (groups.isEmpty()) return "General";
        return groups.get(Math.max(0, Math.min(selectedGroup, groups.size() - 1)));
    }

    private int enabledCountForGroup(String group) {
        int count = 0;
        for (Row row : rows) {
            if (!groupFor(row.name).equals(group)) continue;
            if (row.kind == RowKind.TOGGLE && row.active.get()) count++;
        }
        return count;
    }

    private List<Row> visibleRowsForCurrentGroup() {
        String group = currentGroup();
        String q = search.toLowerCase(Locale.ROOT).trim();
        List<Row> out = new ArrayList<>();
        for (Row row : rows) {
            if (!groupFor(row.name).equals(group)) continue;
            if (q.isEmpty()
                    || row.name.toLowerCase(Locale.ROOT).contains(q)
                    || row.description.toLowerCase(Locale.ROOT).contains(q)
                    || row.value.get().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(row);
            }
        }
        return out;
    }

    private String groupFor(String name) {
        if (section == SpatialSection.VISUALS) return "Crosshair";
        if (section == SpatialSection.CAMERA) return "Camera";
        if (section == SpatialSection.COSMETICS) {
            return name.startsWith("Aura") || name.equals("Player Aura") ? "Aura" : "Halo";
        }
        if (section == SpatialSection.SETTINGS) {
            if (name.contains("Config") || name.contains("Skin") || name.contains("Account")) return "Tools";
            return "Client";
        }

        if (name.startsWith("FPS")) return "FPS";
        if (name.startsWith("Ping")) return "Ping";
        if (name.startsWith("Coordinates") || name.startsWith("Decimal")) return "Coordinates";
        if (name.startsWith("Keystrokes") || name.startsWith("Mouse Buttons")) return "Keystrokes";
        if (name.startsWith("Armor")) return "Armor";
        if (name.startsWith("Effect")) return "Effects";
        if (name.startsWith("CPS")) return "CPS";
        if (name.startsWith("Inventory")) return "Inventory";
        if (name.startsWith("Clock") || name.startsWith("Session") || name.startsWith("Biome")
                || name.startsWith("Direction") || name.startsWith("Memory") || name.startsWith("Held")
                || name.startsWith("Info")) return "Info";
        return "Appearance";
    }

    private String groupDescription(String group) {
        return switch (group) {
            case "Crosshair" -> "Shape, color and readability";
            case "FPS" -> "Frame-rate widget";
            case "Ping" -> "Latency widget";
            case "Coordinates" -> "Position widget";
            case "Keystrokes" -> "Keyboard and mouse input";
            case "Armor" -> "Equipped gear display";
            case "Effects" -> "Potion/status list";
            case "CPS" -> "Click-rate display";
            case "Inventory" -> "Hotbar inventory strip";
            case "Info" -> "Clock, biome, memory and session";
            case "Appearance" -> "Watermark and screen styling";
            case "Aura" -> "Player-local animated rings";
            case "Halo" -> "Player-local head halo";
            case "Camera" -> "Orbit, follow and parallax";
            case "Tools" -> "Configs, account and skin studio";
            default -> "Spatial client preferences";
        };
    }

    private static String fit(String text, int maxWidth, float scale) {
        if (text == null) return "";
        if (UiFont.width(text, scale, UiFont.Weight.REGULAR) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && UiFont.width(text.substring(0, end) + suffix, scale, UiFont.Weight.REGULAR) > maxWidth) end--;
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
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

                rows.add(toggle("CPS Counter", "Shows your real local left/right click rate.", () -> c.cpsHud, v -> c.cpsHud = v));
                rows.add(cycle("CPS Scale", "Resize the CPS pill independently.", () -> scaleLabel(c.cpsScale), () -> c.cpsScale = nextFloat(c.cpsScale, .70F, 1.55F, .05F)));
                rows.add(cycle("CPS Opacity", "CPS background transparency.", () -> pct(c.cpsOpacity), () -> c.cpsOpacity = nextFloat(c.cpsOpacity, .20F, 1.0F, .10F)));
                rows.add(cycle("CPS Corner", "Anchor the CPS display in any corner.", () -> pretty(c.cpsCorner.name()), () -> c.cpsCorner = next(c.cpsCorner, SpatialConfig.HudCorner.values())));

                rows.add(toggle("Clock", "Local system time in a compact Spatial pill.", () -> c.clockHud, v -> c.clockHud = v));
                rows.add(toggle("Session Timer", "Time spent in the current world/session.", () -> c.sessionHud, v -> c.sessionHud = v));
                rows.add(toggle("Biome", "Shows the local biome name.", () -> c.biomeHud, v -> c.biomeHud = v));
                rows.add(toggle("Direction", "Compact cardinal direction display.", () -> c.directionHud, v -> c.directionHud = v));
                rows.add(toggle("Memory", "Shows current Java heap usage.", () -> c.memoryHud, v -> c.memoryHud = v));
                rows.add(toggle("Held Durability", "Shows remaining durability for your held item.", () -> c.durabilityHud, v -> c.durabilityHud = v));
                rows.add(cycle("Info Corner", "Anchor clock/session/biome/direction/memory/durability together.", () -> pretty(c.infoCorner.name()), () -> c.infoCorner = next(c.infoCorner, SpatialConfig.HudCorner.values())));
                rows.add(cycle("Info Scale", "Scale the compact information widgets.", () -> scaleLabel(c.infoScale), () -> c.infoScale = nextFloat(c.infoScale, .70F, 1.55F, .05F)));
                rows.add(cycle("Info Opacity", "Transparency of compact information widgets.", () -> pct(c.infoOpacity), () -> c.infoOpacity = nextFloat(c.infoOpacity, .20F, 1.0F, .10F)));

                rows.add(toggle("Inventory HUD", "Client-side hotbar inventory strip with a rounded selected slot.", () -> c.inventoryHud, v -> c.inventoryHud = v));
                rows.add(cycle("Inventory Scale", "Resize the inventory strip.", () -> scaleLabel(c.inventoryScale), () -> c.inventoryScale = nextFloat(c.inventoryScale, .60F, 1.40F, .05F)));
                rows.add(cycle("Inventory Opacity", "Inventory strip background transparency.", () -> pct(c.inventoryOpacity), () -> c.inventoryOpacity = nextFloat(c.inventoryOpacity, .20F, 1.0F, .10F)));
                rows.add(toggle("Spatial Watermark", "Minimal centered Spatial branding.", () -> c.watermarkHud, v -> c.watermarkHud = v));
                rows.add(toggle("Vignette", "Subtle dark screen-edge framing rendered locally.", () -> c.vignette, v -> c.vignette = v));
                rows.add(cycle("Vignette Strength", "Darkness of the local screen-edge vignette.", () -> c.vignetteStrength + "%", () -> c.vignetteStrength = nextInt(c.vignetteStrength, 5, 80, 5)));
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
                rows.add(cycle("Menu Opacity", "Overall transparency of the compact floating menu surface.", () -> c.menuOpacity + "%", () -> c.menuOpacity = nextInt(c.menuOpacity, 45, 95, 5)));
                rows.add(cycle("Menu Scale", "Resize the compact floating panel without stretching it across the screen.", () -> scaleLabel(c.menuScale), () -> c.menuScale = nextFloat(c.menuScale, .72F, 1.00F, .04F)));
                rows.add(cycle("Corner Radius", "Roundness of the main menu shell.", () -> c.menuRadius + "px", () -> c.menuRadius = nextInt(c.menuRadius, 8, 20, 2)));
                rows.add(cycle("Local Config Manager", "Create, save, load, duplicate, rename and delete local JSON configs.", () -> "OPEN", () -> {
                    if (minecraft != null) minecraft.setScreen(new SpatialProfilesScreen(this));
                }));
                rows.add(cycle("Skin Studio", "Built-in 64x64 local skin pixel editor with PNG export.", () -> "OPEN", () -> {
                    if (minecraft != null) minecraft.setScreen(new SkinEditorScreen(this));
                }));
                rows.add(cycle("Local Account", "Sign out of the local Spatial profile. Password hashes stay on this device.", () -> SpatialLocalAccount.username(), () -> {
                    SpatialLocalAccount.lock();
                    SpatialCamera.forceRestore();
                    if (minecraft != null) minecraft.setScreen(new SpatialAccountScreen());
                }));
                rows.add(cycle("Reset Everything", "Restore every Spatial visual, HUD, cosmetic and camera preference.", () -> "RESET", SpatialMenuScreen::resetVisuals));
            }
            default -> {}
        }
    }

    private static int countFor(SpatialSection s) {
        return switch (s) {
            case VISUALS -> 9;
            case HUD -> 51;
            case COSMETICS -> 22;
            case CAMERA -> 13;
            case SETTINGS -> 8;
            default -> 0;
        };
    }

    private static String ellipsize(String text, int maxWidth, float scale) {
        if (text == null) return "";
        if (UiFont.width(text, scale, UiFont.Weight.REGULAR) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && UiFont.width(text.substring(0, end) + suffix, scale, UiFont.Weight.REGULAR) > maxWidth) end--;
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
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
        c.cpsHud = true; c.cpsScale = .95F; c.cpsOpacity = .72F; c.cpsCorner = SpatialConfig.HudCorner.TOP_LEFT; c.cpsAccentBar = true;
        c.clockHud = false; c.sessionHud = false; c.biomeHud = false; c.directionHud = false; c.memoryHud = false; c.durabilityHud = false; c.infoCorner = SpatialConfig.HudCorner.TOP_RIGHT; c.infoScale = .90F; c.infoOpacity = .68F;
        c.inventoryHud = false; c.inventoryScale = .85F; c.inventoryOpacity = .68F; c.watermarkHud = true; c.vignette = false; c.vignetteStrength = 28; c.toggleNotifications = true;
        c.aura = true; c.auraStyle = SpatialConfig.AuraStyle.ORBIT; c.auraIntensity = 2; c.auraSpeed = 1; c.auraRadius = .52F; c.auraHeight = 1.75F; c.auraWidth = .055F; c.auraWave = .12F; c.auraOpacity = .72F; c.auraUseAccent = true; c.auraColorRgb = 0x7700FF;
        c.halo = true; c.haloStyle = SpatialConfig.HaloStyle.CLEAN_RING; c.haloSpeed = .8F; c.haloRadius = .43F; c.haloHeight = 2.02F; c.haloWidth = .04F; c.haloTilt = 11; c.haloOpacity = .72F; c.haloUseAccent = true; c.haloColorRgb = 0x7700FF;
        c.headFollow = true; c.headFollowYaw = 28; c.headFollowPitch = 16; c.headFollowSpeed = .18F; c.bodyFacesCamera = true;
        c.menuParallax = true; c.parallaxStrength = .09F; c.cameraCollision = true; c.collisionPadding = .20F; c.cameraSmoothness = .18F; c.tabOrbitStrength = 1; c.tabVerticalStrength = 1; c.tabTransitionSpeed = 1;
        c.accentRgb = 0x7700FF; c.menuOpacity = 72; c.menuScale = .86F; c.menuRadius = 15;
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
