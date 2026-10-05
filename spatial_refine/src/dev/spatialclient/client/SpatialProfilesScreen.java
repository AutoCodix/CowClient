package dev.spatialclient.client;

import dev.spatialclient.config.SpatialConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SpatialProfilesScreen extends Screen {
    private static final Path DIR = FMLPaths.CONFIGDIR.get().resolve("spatialclient").resolve("profiles");

    private final Screen parent;
    private final List<Path> profiles = new ArrayList<>();
    private EditBox nameBox;
    private int selected = -1;
    private int scroll;
    private String status = "Named configs are stored locally as readable JSON.";

    public SpatialProfilesScreen(Screen parent) {
        super(Component.literal("Spatial Local Configs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refresh();
        int panelW = Math.min(520, width - 36);
        int x = (width - panelW) / 2;
        int top = Math.max(46, height / 2 - 220);

        nameBox = new EditBox(font, x + 28, top + 62, panelW - 56, 24, Component.literal("Config name"));
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        int y = top + 332;
        int gap = 8;
        int bw = (panelW - 56 - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.literal("Create"), b -> create())
                .bounds(x + 28, y, bw, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Save over"), b -> saveOver())
                .bounds(x + 28 + bw + gap, y, bw, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Load"), b -> loadSelected())
                .bounds(x + 28 + (bw + gap) * 2, y, bw, 24).build());

        y += 32;
        addRenderableWidget(Button.builder(Component.literal("Duplicate"), b -> duplicate())
                .bounds(x + 28, y, bw, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Rename"), b -> rename())
                .bounds(x + 28 + bw + gap, y, bw, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> delete())
                .bounds(x + 28 + (bw + gap) * 2, y, bw, 24).build());

        y += 34;
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(x + 28, y, panelW - 56, 24).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawSpace(g);
        int panelW = Math.min(520, width - 36);
        int panelH = Math.min(450, height - 40);
        int x = (width - panelW) / 2;
        int top = Math.max(20, height / 2 - panelH / 2);

        UiRenderer.shadow(g, x, top, panelW, panelH, 18);
        UiRenderer.roundedOutline(g, x, top, panelW, panelH, 18, 1, 0xFF27315A, 0xEE070A16);

        UiFont.draw(g, "Local Configs", x + 28, top + 20, 0xFFF4F6FF, 0.48F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "Create, save, load, duplicate, rename or delete local profiles.", x + 28, top + 42,
                0xFF7882A2, 0.235F, UiFont.Weight.REGULAR);

        int listTop = top + 102;
        int listBottom = top + 316;
        int rowH = 34;
        int visible = Math.max(1, (listBottom - listTop) / rowH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, profiles.size() - visible)));

        if (profiles.isEmpty()) {
            UiRenderer.roundedRect(g, x + 28, listTop, panelW - 56, 54, 10, 0xFF11172B);
            UiFont.draw(g, "No saved configs yet.", x + 42, listTop + 17, 0xFFC6CCE0, 0.29F, UiFont.Weight.SEMIBOLD);
        } else {
            for (int i = scroll; i < Math.min(profiles.size(), scroll + visible); i++) {
                Path path = profiles.get(i);
                int ry = listTop + (i - scroll) * rowH;
                boolean sel = i == selected;
                boolean hover = mouseX >= x + 28 && mouseX <= x + panelW - 28 && mouseY >= ry && mouseY <= ry + 28;
                UiRenderer.roundedOutline(g, x + 28, ry, panelW - 56, 28, 8, 1,
                        sel ? 0xFF5160A2 : (hover ? 0xFF303B61 : 0xFF202742),
                        sel ? 0xFF171E38 : 0xFF10162A);
                UiFont.draw(g, displayName(path), x + 40, ry + 8, sel ? 0xFFFFFFFF : 0xFFD7DBE8,
                        0.255F, UiFont.Weight.SEMIBOLD);
            }
        }

        UiFont.draw(g, status, x + 28, top + panelH - 22, 0xFF8190B6, 0.205F, UiFont.Weight.REGULAR);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelW = Math.min(520, width - 36);
        int panelH = Math.min(450, height - 40);
        int x = (width - panelW) / 2;
        int top = Math.max(20, height / 2 - panelH / 2);
        int listTop = top + 102;
        int rowH = 34;
        int visible = Math.max(1, 214 / rowH);

        if (button == 0) {
            for (int i = scroll; i < Math.min(profiles.size(), scroll + visible); i++) {
                int ry = listTop + (i - scroll) * rowH;
                if (mouseX >= x + 28 && mouseX <= x + panelW - 28 && mouseY >= ry && mouseY <= ry + 28) {
                    selected = i;
                    nameBox.setValue(displayName(profiles.get(i)));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll + (delta < 0 ? 1 : -1));
        return true;
    }

    private void create() {
        String name = safeName(nameBox.getValue());
        if (name == null) {
            status = "Use 1-32 letters, numbers, spaces, dash or underscore.";
            return;
        }
        try {
            Files.createDirectories(DIR);
            Path target = DIR.resolve(name + ".json");
            if (Files.exists(target)) {
                status = "That config already exists.";
                return;
            }
            Files.writeString(target, SpatialConfig.snapshotJson(), StandardCharsets.UTF_8);
            status = "Created " + name + ".";
            refresh();
            selectByName(name);
        } catch (Exception e) {
            status = "Could not create config.";
        }
    }

    private void saveOver() {
        Path path = selectedPath();
        if (path == null) {
            status = "Select a config first.";
            return;
        }
        try {
            Files.writeString(path, SpatialConfig.snapshotJson(), StandardCharsets.UTF_8);
            status = "Saved current Spatial settings.";
        } catch (Exception e) {
            status = "Could not save config.";
        }
    }

    private void loadSelected() {
        Path path = selectedPath();
        if (path == null) {
            status = "Select a config first.";
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (SpatialConfig.applyJson(json)) {
                status = "Loaded " + displayName(path) + ".";
            } else {
                status = "Config is malformed; current settings were kept.";
            }
        } catch (Exception e) {
            status = "Could not load config.";
        }
    }

    private void duplicate() {
        Path path = selectedPath();
        if (path == null) {
            status = "Select a config first.";
            return;
        }
        String wanted = safeName(nameBox.getValue());
        if (wanted == null || wanted.equalsIgnoreCase(displayName(path))) {
            wanted = displayName(path) + " copy";
        }
        try {
            Files.createDirectories(DIR);
            Path target = uniquePath(wanted);
            Files.copy(path, target);
            status = "Duplicated as " + displayName(target) + ".";
            refresh();
            selectByName(displayName(target));
        } catch (Exception e) {
            status = "Could not duplicate config.";
        }
    }

    private void rename() {
        Path path = selectedPath();
        String wanted = safeName(nameBox.getValue());
        if (path == null) {
            status = "Select a config first.";
            return;
        }
        if (wanted == null) {
            status = "Enter a valid new name.";
            return;
        }
        try {
            Path target = DIR.resolve(wanted + ".json");
            if (Files.exists(target) && !target.equals(path)) {
                status = "A config with that name already exists.";
                return;
            }
            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
            status = "Renamed to " + wanted + ".";
            refresh();
            selectByName(wanted);
        } catch (Exception e) {
            status = "Could not rename config.";
        }
    }

    private void delete() {
        Path path = selectedPath();
        if (path == null) {
            status = "Select a config first.";
            return;
        }
        try {
            String name = displayName(path);
            Files.deleteIfExists(path);
            status = "Deleted " + name + ".";
            selected = -1;
            refresh();
        } catch (Exception e) {
            status = "Could not delete config.";
        }
    }

    private void refresh() {
        profiles.clear();
        try {
            Files.createDirectories(DIR);
            try (var stream = Files.list(DIR)) {
                stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(profiles::add);
            }
        } catch (Exception ignored) {
        }
        if (selected >= profiles.size()) selected = profiles.isEmpty() ? -1 : profiles.size() - 1;
    }

    private Path selectedPath() {
        return selected >= 0 && selected < profiles.size() ? profiles.get(selected) : null;
    }

    private void selectByName(String name) {
        for (int i = 0; i < profiles.size(); i++) {
            if (displayName(profiles.get(i)).equalsIgnoreCase(name)) {
                selected = i;
                nameBox.setValue(displayName(profiles.get(i)));
                return;
            }
        }
    }

    private static String displayName(Path path) {
        String file = path.getFileName().toString();
        return file.toLowerCase(Locale.ROOT).endsWith(".json") ? file.substring(0, file.length() - 5) : file;
    }

    private static String safeName(String value) {
        if (value == null) return null;
        String clean = value.trim().replaceAll("\\s+", " ");
        if (clean.length() < 1 || clean.length() > 32) return null;
        if (!clean.matches("[A-Za-z0-9 _-]+")) return null;
        return clean;
    }

    private static Path uniquePath(String base) {
        Path candidate = DIR.resolve(base + ".json");
        int i = 2;
        while (Files.exists(candidate)) candidate = DIR.resolve(base + " " + i++ + ".json");
        return candidate;
    }

    private void drawSpace(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF02040D);
        for (int i = 0; i < 38; i++) {
            int x = Math.floorMod(i * 149 + 61, Math.max(1, width));
            int y = Math.floorMod(i * 83 + 17, Math.max(1, height));
            int c = (i % 7 == 0) ? 0xFF84B3FF : (i % 10 == 0 ? 0xFFA38EFF : 0xFF536386);
            g.fill(x, y, x + 1 + (i % 9 == 0 ? 1 : 0), y + 1 + (i % 9 == 0 ? 1 : 0), c);
        }
        g.fill(0, 0, width, height / 5, 0x22103B73);
        g.fill(0, height * 4 / 5, width, height, 0x22180D40);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
