package dev.spatialclient.client;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SpatialTitleScreen extends Screen {
    private final List<MenuButton> buttons = new ArrayList<>();
    private float appear;
    private long openedAt;

    public SpatialTitleScreen() {
        super(Component.literal("Spatial Client"));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        appear = 0.0F;
        buttons.clear();
        int cardW = Math.min(430, Math.max(330, width / 3));
        int x = Math.max(28, width / 2 - cardW / 2);
        int baseY = Math.max(170, height / 2 - 54);
        int full = cardW;
        int half = (cardW - 10) / 2;

        buttons.add(new MenuButton("Singleplayer", "Your worlds", x, baseY, full, 36,
                () -> minecraft.setScreen(new SpatialWorldScreen(this))));
        buttons.add(new MenuButton("Multiplayer", "Servers & LAN", x, baseY + 46, full, 36,
                () -> minecraft.setScreen(new SpatialMultiplayerScreen(this))));
        buttons.add(new MenuButton("Mods", "Installed Forge mods", x, baseY + 92, half, 34,
                () -> minecraft.setScreen(new ModListScreen(this))));
        buttons.add(new MenuButton("Realms", "Minecraft Realms", x + half + 10, baseY + 92, half, 34,
                () -> minecraft.setScreen(new RealmsMainScreen(this))));
        buttons.add(new MenuButton("Options", "Video, audio, controls", x, baseY + 136, half, 34,
                () -> minecraft.setScreen(new SpatialOptionsScreen(this, minecraft.options))));
        buttons.add(new MenuButton("Quit", "Close Minecraft", x + half + 10, baseY + 136, half, 34,
                () -> minecraft.stop()));
    }

    @Override
    public void tick() {
        appear += (1.0F - appear) * 0.14F;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SpatialMenuTheme.drawSpace(g, width, height, mouseX, mouseY);

        int cardW = Math.min(430, Math.max(330, width / 3));
        int x = Math.max(28, width / 2 - cardW / 2);
        int logoSize = Math.min(86, Math.max(64, height / 10));
        int logoX = width / 2 - logoSize / 2;
        int logoY = Math.max(40, height / 2 - 210);

        SpatialMenuTheme.drawLogo(g, logoX, logoY, logoSize);
        String brand = "SPATIAL";
        float brandScale = 0.74F;
        float bw = UiFont.width(brand, brandScale, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, brand, width / 2.0F - bw / 2.0F, logoY + logoSize + 16, 0xFFF7F9FF, brandScale, UiFont.Weight.SEMIBOLD);

        String version = "Forge 1.20.1  /  " + SharedConstants.getCurrentVersion().getName();
        float vw = UiFont.width(version, 0.25F, UiFont.Weight.REGULAR);
        UiFont.draw(g, version, width / 2.0F - vw / 2.0F, logoY + logoSize + 41, 0xFF7685A9, 0.25F, UiFont.Weight.REGULAR);

        String[] splashes = {
                "Make the client yours.",
                "Visuals without the clutter.",
                "Space looks better in motion.",
                "Spatial Client // built to customize."
        };
        int splashIndex = (int)((System.currentTimeMillis() / 4500L) % splashes.length);
        String splash = splashes[splashIndex];
        float sw = UiFont.width(splash, 0.29F, UiFont.Weight.SEMIBOLD);
        if ((splashIndex & 1) == 0) {
            SpatialMenuTheme.rainbowText(g, splash, width / 2.0F - sw / 2.0F, logoY + logoSize + 60, 0.29F);
        } else {
            UiFont.draw(g, splash, width / 2.0F - sw / 2.0F, logoY + logoSize + 60, 0xFFFFD84D, 0.29F, UiFont.Weight.SEMIBOLD);
        }

        for (MenuButton b : buttons) drawButton(g, b, mouseX, mouseY);

        String profile = SpatialLocalAccount.hasAccount() ? SpatialLocalAccount.username() : "local profile";
        UiFont.draw(g, "Signed in as " + profile, 18, height - 26, 0xFF7281A5, 0.23F, UiFont.Weight.REGULAR);
        UiFont.draw(g, "Spatial Client", width - 108, height - 26, 0xFF526080, 0.22F, UiFont.Weight.REGULAR);
    }

    private void drawButton(GuiGraphics g, MenuButton b, int mouseX, int mouseY) {
        boolean hover = b.contains(mouseX, mouseY);
        int outer = hover ? 0xFF4266A6 : 0xFF253456;
        int inner = hover ? 0xEE111B33 : 0xDD0A1021;
        if (hover) {
            UiRenderer.roundedRect(g, b.x - 3, b.y - 3, b.w + 6, b.h + 6, 14, 0x242D74FF);
        }
        UiRenderer.roundedOutline(g, b.x, b.y, b.w, b.h, 11, 1, outer, inner);
        if (hover) UiRenderer.roundedRect(g, b.x + 5, b.y + 5, 3, b.h - 10, 2, 0xFF7700FF);

        UiFont.draw(g, b.title, b.x + 16, b.y + 8, hover ? 0xFFFFFFFF : 0xFFE5EBFA, 0.32F, UiFont.Weight.SEMIBOLD);
        float subW = UiFont.width(b.subtitle, 0.21F, UiFont.Weight.REGULAR);
        UiFont.draw(g, b.subtitle, b.x + b.w - subW - 14, b.y + 10, 0xFF68779A, 0.21F, UiFont.Weight.REGULAR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (MenuButton b : buttons) {
                if (b.contains(mouseX, mouseY)) {
                    SpatialMenuTheme.click(1.0F);
                    b.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        // Full-screen custom background is rendered in render().
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record MenuButton(String title, String subtitle, int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
}
