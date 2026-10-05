package dev.spatialclient.client;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.ModListScreen;

import java.util.ArrayList;
import java.util.List;

public final class SpatialTitleScreen extends Screen {
    private final List<MenuButton> buttons = new ArrayList<>();

    private int shellX;
    private int shellY;
    private int shellW;
    private int shellH;
    private int heroW;

    public SpatialTitleScreen() {
        super(Component.literal("Spatial Client"));
    }

    @Override
    protected void init() {
        buttons.clear();
        layout();

        int bx = shellX + heroW + 28;
        int bw = shellW - heroW - 56;
        int by = shellY + 104;

        buttons.add(new MenuButton("Singleplayer", "Play your worlds", "01", bx, by, bw, 45,
                () -> minecraft.setScreen(new SpatialWorldScreen(this))));
        buttons.add(new MenuButton("Multiplayer", "Servers and LAN", "02", bx, by + 55, bw, 45,
                () -> minecraft.setScreen(new SpatialMultiplayerScreen(this))));

        int half = (bw - 10) / 2;
        buttons.add(new MenuButton("Mods", "Forge", "03", bx, by + 110, half, 42,
                () -> minecraft.setScreen(new ModListScreen(this))));
        buttons.add(new MenuButton("Realms", "Online", "04", bx + half + 10, by + 110, half, 42,
                () -> minecraft.setScreen(new RealmsMainScreen(this))));

        buttons.add(new MenuButton("Options", "Customize Minecraft", "05", bx, by + 162, bw, 42,
                () -> minecraft.setScreen(new SpatialOptionsScreen(this, minecraft.options))));
        buttons.add(new MenuButton("Quit Game", "See you in space", "06", bx, by + 214, bw, 42,
                () -> minecraft.stop()));
    }

    private void layout() {
        shellW = Math.min(910, Math.max(690, width - 150));
        shellH = Math.min(500, Math.max(410, height - 140));
        shellX = (width - shellW) / 2;
        shellY = (height - shellH) / 2;
        heroW = Math.min(360, Math.max(300, shellW * 42 / 100));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        layout();
        SpatialMenuTheme.drawSpace(g, width, height, mouseX, mouseY);

        long now = System.nanoTime();
        float t = (now % 60_000_000_000L) / 1_000_000_000.0F;

        // Outer floating shell.
        UiRenderer.shadow(g, shellX, shellY, shellW, shellH, 28);
        UiRenderer.roundedOutline(g, shellX, shellY, shellW, shellH, 28, 1,
                0xFF293D68, 0xE7080C18);

        // Left hero glass.
        UiRenderer.roundedRect(g, shellX + 12, shellY + 12, heroW - 18, shellH - 24, 22, 0xD9081020);
        g.fill(shellX + heroW, shellY + 28, shellX + heroW + 1, shellY + shellH - 28, 0x66405172);

        // Hero starfield is contained inside the card.
        SpatialMenuTheme.drawPanelSpace(g, shellX + 14, shellY + 14, heroW - 22, shellH - 28, mouseX, mouseY);

        int logo = Math.min(126, Math.max(96, shellH / 4));
        int logoX = shellX + heroW / 2 - logo / 2 - 4;
        int logoY = shellY + 55;

        drawOrbit(g, logoX + logo / 2, logoY + logo / 2, logo, t);
        SpatialMenuTheme.drawLogo(g, logoX, logoY, logo);

        float brandScale = 0.70F;
        float brandW = UiFont.width("SPATIAL", brandScale, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "SPATIAL", shellX + heroW / 2.0F - brandW / 2.0F - 4,
                logoY + logo + 23, 0xFFF8FAFF, brandScale, UiFont.Weight.SEMIBOLD);

        float clientW = UiFont.width("CLIENT", 0.245F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "CLIENT", shellX + heroW / 2.0F - clientW / 2.0F - 4,
                logoY + logo + 54, 0xFF8090B5, 0.245F, UiFont.Weight.SEMIBOLD);

        int pillY = logoY + logo + 82;
        chip(g, shellX + 36, pillY, 104, "FORGE 1.20.1");
        chip(g, shellX + 148, pillY, 102, "VISUAL ONLY");

        String[] promos = {
                "Make it yours.",
                "Built for clean visuals.",
                "Your client. Your space.",
                "Spatial Client // orbit different."
        };
        int promo = (int)((System.currentTimeMillis() / 4200L) % promos.length);
        String line = promos[promo];
        float lineScale = 0.255F;
        float lineW = UiFont.width(line, lineScale, UiFont.Weight.SEMIBOLD);
        if ((promo & 1) == 0) {
            SpatialMenuTheme.rainbowText(g, line,
                    shellX + heroW / 2.0F - lineW / 2.0F - 4,
                    shellY + shellH - 88, lineScale);
        } else {
            UiFont.draw(g, line,
                    shellX + heroW / 2.0F - lineW / 2.0F - 4,
                    shellY + shellH - 88, 0xFFFFD84A, lineScale, UiFont.Weight.SEMIBOLD);
        }

        UiFont.draw(g, "RSHIFT  opens Spatial in-game",
                shellX + 34, shellY + shellH - 49,
                0xFF5D6C8D, 0.205F, UiFont.Weight.REGULAR);

        // Right header.
        int rx = shellX + heroW + 28;
        UiFont.draw(g, "Welcome back", rx, shellY + 37, 0xFFF5F8FF, 0.48F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "Choose where you want to go.", rx, shellY + 63,
                0xFF7483A6, 0.225F, UiFont.Weight.REGULAR);

        String user = SpatialLocalAccount.hasAccount() ? SpatialLocalAccount.username() : "Local profile";
        float userW = UiFont.width(user, 0.215F, UiFont.Weight.SEMIBOLD);
        float chipW = Math.max(78.0F, userW + 28.0F);
        UiRenderer.roundedRect(g, shellX + shellW - chipW - 27, shellY + 31, chipW, 26, 9, 0xFF111A30);
        UiRenderer.circle(g, shellX + shellW - chipW - 16, shellY + 44, 4.0F, 0xFF62D6A2);
        UiFont.draw(g, user, shellX + shellW - chipW - 6, shellY + 37,
                0xFFCAD5EA, 0.215F, UiFont.Weight.SEMIBOLD);

        for (MenuButton b : buttons) drawButton(g, b, mouseX, mouseY);

        String version = SharedConstants.getCurrentVersion().getName();
        UiFont.draw(g, "Minecraft " + version, shellX + shellW - 118, shellY + shellH - 33,
                0xFF52617F, 0.195F, UiFont.Weight.REGULAR);
    }

    private void drawOrbit(GuiGraphics g, int cx, int cy, int logo, float t) {
        int outer = logo + 34;
        UiRenderer.roundedOutline(g, cx - outer / 2, cy - outer / 2,
                outer, outer, outer / 2.0F, 1, 0x305E7CBA, 0x00000000);

        int inner = logo + 14;
        UiRenderer.roundedOutline(g, cx - inner / 2, cy - inner / 2,
                inner, inner, inner / 2.0F, 1, 0x253C57A0, 0x00000000);

        for (int i = 0; i < 3; i++) {
            double a = t * (0.55 + i * 0.13) + i * 2.094;
            float radius = outer * (0.46F - i * 0.035F);
            float px = cx + (float)Math.cos(a) * radius;
            float py = cy + (float)Math.sin(a) * radius * 0.42F;
            int color = i == 0 ? 0xFF8B62FF : (i == 1 ? 0xFF63C7FF : 0xFFFFD25E);
            UiRenderer.circle(g, px, py, i == 2 ? 2.2F : 2.8F, color);
        }
    }

    private void chip(GuiGraphics g, int x, int y, int w, String text) {
        UiRenderer.roundedOutline(g, x, y, w, 25, 9, 1, 0xFF2B3959, 0xB90C1325);
        float tw = UiFont.width(text, 0.185F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, text, x + (w - tw) / 2.0F, y + 7, 0xFF8290AD, 0.185F, UiFont.Weight.SEMIBOLD);
    }

    private void drawButton(GuiGraphics g, MenuButton b, int mouseX, int mouseY) {
        boolean hover = b.contains(mouseX, mouseY);
        int ox = hover ? 4 : 0;

        if (hover) {
            UiRenderer.roundedRect(g, b.x - 3, b.y - 3, b.w + 10, b.h + 6, 14, 0x203E7FFF);
        }

        UiRenderer.roundedOutline(g, b.x + ox, b.y, b.w - ox, b.h, 12, 1,
                hover ? 0xFF45679A : 0xFF283653,
                hover ? 0xE7121D36 : 0xCB0B1121);

        UiRenderer.roundedRect(g, b.x + 10 + ox, b.y + 9, 27, b.h - 18, 8,
                hover ? 0xFF273C69 : 0xFF151F36);
        UiFont.draw(g, b.index, b.x + 17 + ox, b.y + 13, 0xFF8295BF, 0.185F, UiFont.Weight.SEMIBOLD);

        UiFont.draw(g, b.title, b.x + 49 + ox, b.y + 9,
                hover ? 0xFFFFFFFF : 0xFFE8EEF9, 0.285F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, b.subtitle, b.x + 49 + ox, b.y + 25,
                hover ? 0xFF8FA1C7 : 0xFF657492, 0.185F, UiFont.Weight.REGULAR);

        UiFont.draw(g, ">", b.x + b.w - 25, b.y + 14,
                hover ? 0xFFB8CFFF : 0xFF53617E, 0.255F, UiFont.Weight.SEMIBOLD);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (MenuButton b : buttons) {
                if (b.contains(mouseX, mouseY)) {
                    SpatialMenuTheme.click(1.05F);
                    b.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        // Spatial draws its own background.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record MenuButton(String title, String subtitle, String index,
                              int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
}
