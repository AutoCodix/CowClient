package dev.spatialclient.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class SkinEditorScreen extends Screen {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("spatialclient").resolve("custom-skin.png");
    private static final int[] PALETTE = {
            0xFF0A0D18, 0xFFFFFFFF, 0xFFB7C4E8, 0xFF5975C7,
            0xFF1878FF, 0xFF00BEEA, 0xFF7700FF, 0xFFC45CFF,
            0xFFFF5EAE, 0xFFFF8D5C, 0xFF8E5A3C, 0xFF4B2E24,
            0xFF6AD48A, 0xFF267D54, 0xFF9B9B9B, 0x00000000
    };

    private final Screen parent;
    private final int[] pixels = new int[64 * 64];
    private int selectedColor = 4;
    private int gridX;
    private int gridY;
    private int cell;
    private String status = "Left click paints. Right click erases.";

    public SkinEditorScreen(Screen parent) {
        super(Component.literal("Spatial Skin Studio"));
        this.parent = parent;
        load();
    }

    @Override
    protected void init() {
        cell = Math.max(3, Math.min(6, (height - 150) / 64));
        int gridSize = cell * 64;
        gridX = Math.max(20, width / 2 - gridSize / 2 - 90);
        gridY = Math.max(58, (height - gridSize) / 2);

        int right = gridX + gridSize + 24;
        addRenderableWidget(Button.builder(Component.literal("Save PNG"), b -> save())
                .bounds(right, gridY + 184, 138, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Reset canvas"), b -> {
            resetTemplate();
            status = "Canvas reset.";
        }).bounds(right, gridY + 216, 138, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(right, gridY + 248, 138, 24).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawSpace(g);
        int gridSize = cell * 64;
        int right = gridX + gridSize + 24;

        UiRenderer.shadow(g, gridX - 14, gridY - 40, gridSize + 188, gridSize + 54, 18);
        UiRenderer.roundedOutline(g, gridX - 14, gridY - 40, gridSize + 188, gridSize + 54,
                18, 1, 0xFF26315B, 0xEE070A16);

        UiFont.draw(g, "Skin Studio", gridX, gridY - 31, 0xFFF4F6FF, 0.48F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "64 x 64 local PNG editor", gridX, gridY - 13, 0xFF7B84A2, 0.235F, UiFont.Weight.REGULAR);

        g.fill(gridX - 1, gridY - 1, gridX + gridSize + 1, gridY + gridSize + 1, 0xFF29304A);
        for (int py = 0; py < 64; py++) {
            for (int px = 0; px < 64; px++) {
                int argb = pixels[py * 64 + px];
                int x = gridX + px * cell;
                int y = gridY + py * cell;
                int checker = ((px + py) & 1) == 0 ? 0xFF14182A : 0xFF101424;
                g.fill(x, y, x + cell, y + cell, checker);
                if (((argb >>> 24) & 255) > 0) g.fill(x, y, x + cell, y + cell, argb);
            }
        }

        UiFont.draw(g, "PALETTE", right, gridY + 2, 0xFF69718F, 0.22F, UiFont.Weight.SEMIBOLD);
        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % 4;
            int row = i / 4;
            int x = right + col * 32;
            int y = gridY + 22 + row * 32;
            int border = i == selectedColor ? 0xFFFFFFFF : 0xFF303750;
            UiRenderer.roundedRect(g, x, y, 24, 24, 7, border);
            int c = PALETTE[i];
            if (((c >>> 24) & 255) == 0) {
                UiRenderer.roundedRect(g, x + 2, y + 2, 20, 20, 6, 0xFF171B2B);
                g.fill(x + 5, y + 11, x + 19, y + 13, 0xFFFF627D);
            } else {
                UiRenderer.roundedRect(g, x + 2, y + 2, 20, 20, 6, c);
            }
        }

        UiFont.draw(g, "Saved locally:", right, gridY + 292, 0xFF707A99, 0.22F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "config/spatialclient/custom-skin.png", right, gridY + 307,
                0xFFA7B2D3, 0.20F, UiFont.Weight.REGULAR);
        UiFont.draw(g, status, right, gridY + 337, 0xFF8296C8, 0.20F, UiFont.Weight.REGULAR);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (paint(mouseX, mouseY, button)) return true;

        int gridSize = cell * 64;
        int right = gridX + gridSize + 24;
        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % 4;
            int row = i / 4;
            int x = right + col * 32;
            int y = gridY + 22 + row * 32;
            if (mouseX >= x && mouseX <= x + 24 && mouseY >= y && mouseY <= y + 24) {
                selectedColor = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (paint(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean paint(double mouseX, double mouseY, int button) {
        int size = cell * 64;
        if (mouseX < gridX || mouseY < gridY || mouseX >= gridX + size || mouseY >= gridY + size) return false;
        int px = (int) ((mouseX - gridX) / cell);
        int py = (int) ((mouseY - gridY) / cell);
        pixels[py * 64 + px] = button == 1 ? 0x00000000 : PALETTE[selectedColor];
        status = "Unsaved changes.";
        return true;
    }

    private void load() {
        Arrays.fill(pixels, 0x00000000);
        try {
            if (Files.isRegularFile(FILE)) {
                BufferedImage image = ImageIO.read(FILE.toFile());
                if (image != null && image.getWidth() == 64 && image.getHeight() == 64) {
                    image.getRGB(0, 0, 64, 64, pixels, 0, 64);
                    status = "Loaded saved local skin.";
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        resetTemplate();
    }

    private void resetTemplate() {
        Arrays.fill(pixels, 0x00000000);
        fill(8, 8, 8, 8, 0xFFD2A17D);
        fill(20, 20, 8, 12, 0xFF263A78);
        fill(4, 20, 4, 12, 0xFF202B55);
        fill(44, 20, 4, 12, 0xFF202B55);
        fill(4, 36, 4, 12, 0xFF12182E);
        fill(20, 52, 4, 12, 0xFF12182E);
        fill(36, 52, 4, 12, 0xFF12182E);
        fill(9, 11, 2, 1, 0xFF68B5FF);
        fill(13, 11, 2, 1, 0xFF68B5FF);
    }

    private void fill(int x, int y, int w, int h, int color) {
        for (int py = y; py < Math.min(64, y + h); py++) {
            for (int px = x; px < Math.min(64, x + w); px++) pixels[py * 64 + px] = color;
        }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 64, 64, pixels, 0, 64);
            ImageIO.write(image, "PNG", FILE.toFile());
            status = "Saved.";
        } catch (Exception e) {
            status = "Save failed.";
        }
    }

    private void drawSpace(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF02040D);
        for (int i = 0; i < 34; i++) {
            int x = Math.floorMod(i * 173 + 47, Math.max(1, width));
            int y = Math.floorMod(i * 97 + 31, Math.max(1, height));
            int c = (i % 6 == 0) ? 0xFF7EA7FF : (i % 9 == 0 ? 0xFFA98DFF : 0xFF4F5F86);
            g.fill(x, y, x + 1 + (i % 8 == 0 ? 1 : 0), y + 1 + (i % 8 == 0 ? 1 : 0), c);
        }
        g.fill(0, 0, width, height / 5, 0x22103D74);
        g.fill(0, height * 4 / 5, width, height, 0x22190E42);
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
