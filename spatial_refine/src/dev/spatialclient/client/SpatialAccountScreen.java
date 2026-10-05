package dev.spatialclient.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public final class SpatialAccountScreen extends Screen {
    private final Screen nextScreen;
    private final boolean openMenuAfter;

    private EditBox username;
    private EditBox password;
    private boolean registerMode;
    private boolean remember = true;
    private String status = "";

    private int cardX, cardY, cardW, cardH;
    private int formX, formW;

    public SpatialAccountScreen() {
        this(null, true);
    }

    public SpatialAccountScreen(Screen nextScreen, boolean openMenuAfter) {
        super(Component.literal("Spatial Client"));
        this.nextScreen = nextScreen;
        this.openMenuAfter = openMenuAfter;
        SpatialLocalAccount.init();
        registerMode = !SpatialLocalAccount.hasAccount();
    }

    @Override
    protected void init() {
        cardW = Math.min(680, width - 42);
        cardH = Math.min(390, height - 52);
        cardX = (width - cardW) / 2;
        cardY = Math.max(26, (height - cardH) / 2);

        int artW = Math.min(250, cardW / 2 - 24);
        formX = cardX + artW + 38;
        formW = cardX + cardW - 30 - formX;

        username = new EditBox(font, formX + 2, cardY + 136, formW - 4, 26, Component.literal("Username"));
        username.setBordered(false);
        username.setMaxLength(20);
        if (!registerMode && SpatialLocalAccount.hasAccount()) username.setValue(SpatialLocalAccount.username());
        addRenderableWidget(username);

        password = new EditBox(font, formX + 2, cardY + 194, formW - 4, 26, Component.literal("Password"));
        password.setBordered(false);
        password.setMaxLength(64);
        password.setFormatter((value, index) -> FormattedCharSequence.forward("•".repeat(value.length()), Style.EMPTY));
        addRenderableWidget(password);

        setInitialFocus(registerMode ? username : password);
    }

    private void submit() {
        SpatialLocalAccount.Result result = registerMode
                ? SpatialLocalAccount.register(username.getValue(), password.getValue(), remember)
                : SpatialLocalAccount.login(username.getValue(), password.getValue(), remember);
        status = result.message();
        password.setValue("");
        if (result.ok()) {
            SpatialMenuTheme.click(1.08F);
            continueAfterLogin();
        }
    }

    private void continueAfterLogin() {
        if (minecraft == null) return;
        if (nextScreen != null) {
            minecraft.setScreen(nextScreen);
            return;
        }
        if (openMenuAfter && minecraft.player != null && minecraft.level != null) {
            SpatialCamera.open();
            if (SpatialCamera.isMenuCameraActive()) minecraft.setScreen(new SpatialMenuScreen());
            return;
        }
        minecraft.setScreen(new SpatialTitleScreen());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SpatialMenuTheme.drawSpace(g, width, height, mouseX, mouseY);

        int artW = Math.min(250, cardW / 2 - 24);
        int accent = 0xFF000000 | dev.spatialclient.config.SpatialConfig.get().accentRgb;

        UiRenderer.shadow(g, cardX, cardY, cardW, cardH, 22);
        UiRenderer.roundedOutline(g, cardX, cardY, cardW, cardH, 22, 1, 0xFF273B68, 0xF2070B18);

        // left visual panel
        UiRenderer.roundedRect(g, cardX + 12, cardY + 12, artW - 6, cardH - 24, 18, 0xFF080E20);
        for (int i = 0; i < 30; i++) {
            int sx = cardX + 22 + Math.floorMod(i * 71, Math.max(1, artW - 36));
            int sy = cardY + 24 + Math.floorMod(i * 47, Math.max(1, cardH - 52));
            int col = i % 7 == 0 ? 0xFF9D86FF : (i % 5 == 0 ? 0xFF73BFFF : 0xFF334766);
            g.fill(sx, sy, sx + (i % 11 == 0 ? 2 : 1), sy + (i % 11 == 0 ? 2 : 1), col);
        }

        int logo = Math.min(112, artW - 70);
        SpatialMenuTheme.drawLogo(g, cardX + artW / 2 - logo / 2 + 4, cardY + 56, logo);
        String brand = "SPATIAL";
        float brandW = UiFont.width(brand, 0.56F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, brand, cardX + artW / 2.0F - brandW / 2.0F + 4, cardY + 182,
                0xFFF7F9FF, 0.56F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "LOCAL PROFILE", cardX + 33, cardY + 218, 0xFF6C7B9C, 0.22F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "Your Spatial settings, configs and", cardX + 33, cardY + 244, 0xFF8A96B5, 0.235F, UiFont.Weight.REGULAR);
        UiFont.draw(g, "skin studio stay on this device.", cardX + 33, cardY + 260, 0xFF8A96B5, 0.235F, UiFont.Weight.REGULAR);
        UiFont.draw(g, "PBKDF2 password hash / no cloud account", cardX + 33, cardY + cardH - 43,
                0xFF526281, 0.19F, UiFont.Weight.REGULAR);

        // right form
        UiFont.draw(g, registerMode ? "Create your Spatial profile" : "Welcome back", formX, cardY + 38,
                0xFFF5F7FF, 0.54F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, registerMode
                        ? "One local profile. Remembering is optional."
                        : "Unlock your local Spatial profile to continue.",
                formX, cardY + 67, 0xFF7988AA, 0.235F, UiFont.Weight.REGULAR);

        UiFont.draw(g, "USERNAME", formX, cardY + 112, 0xFF697A9E, 0.215F, UiFont.Weight.SEMIBOLD);
        drawField(g, formX, cardY + 130, formW, 38, username.isFocused(), accent);

        UiFont.draw(g, "PASSWORD", formX, cardY + 170, 0xFF697A9E, 0.215F, UiFont.Weight.SEMIBOLD);
        drawField(g, formX, cardY + 188, formW, 38, password.isFocused(), accent);

        int rememberY = cardY + 240;
        boolean rememberHover = inside(mouseX, mouseY, formX, rememberY, formW, 26);
        UiRenderer.roundedRect(g, formX, rememberY, formW, 26, 9, rememberHover ? 0xFF111A31 : 0xFF0C1326);
        UiRenderer.roundedOutline(g, formX + 7, rememberY + 6, 14, 14, 5, 1, remember ? accent : 0xFF34405F, 0xFF0A1020);
        if (remember) UiRenderer.roundedRect(g, formX + 10, rememberY + 9, 8, 8, 3, accent);
        UiFont.draw(g, "Remember this device", formX + 30, rememberY + 7, 0xFFC9D2E8, 0.24F, UiFont.Weight.REGULAR);

        int primaryY = cardY + 282;
        boolean primaryHover = inside(mouseX, mouseY, formX, primaryY, formW, 38);
        if (primaryHover) UiRenderer.roundedRect(g, formX - 3, primaryY - 3, formW + 6, 44, 13, 0x222A70FF);
        UiRenderer.roundedOutline(g, formX, primaryY, formW, 38, 11, 1,
                primaryHover ? 0xFF5B6FD5 : 0xFF384980,
                primaryHover ? 0xFF182347 : 0xFF111A34);
        String primary = registerMode ? "Create local profile" : "Sign in";
        float pw = UiFont.width(primary, 0.30F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, primary, formX + (formW - pw) / 2.0F, primaryY + 11, 0xFFF5F7FF, 0.30F, UiFont.Weight.SEMIBOLD);

        int switchY = cardY + 330;
        String switchText = registerMode ? "Already have a profile? Sign in" : "Need a new profile? Register";
        float sw = UiFont.width(switchText, 0.225F, UiFont.Weight.REGULAR);
        UiFont.draw(g, switchText, formX + (formW - sw) / 2.0F, switchY,
                inside(mouseX, mouseY, formX, switchY - 4, formW, 20) ? 0xFFB7C9FF : 0xFF7180A2,
                0.225F, UiFont.Weight.REGULAR);

        if (!status.isBlank()) {
            int statusColor = status.toLowerCase().contains("incorrect") || status.toLowerCase().contains("must")
                    ? 0xFFFF9AA8 : 0xFFAFC6FF;
            UiFont.draw(g, status, formX, cardY + cardH - 24, statusColor, 0.215F, UiFont.Weight.REGULAR);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawField(GuiGraphics g, int x, int y, int w, int h, boolean focused, int accent) {
        UiRenderer.roundedOutline(g, x, y, w, h, 10, 1,
                focused ? accent : 0xFF2A3657,
                focused ? 0xFF0E1730 : 0xFF0A1122);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int rememberY = cardY + 240;
            int primaryY = cardY + 282;
            int switchY = cardY + 330;
            if (inside(mouseX, mouseY, formX, rememberY, formW, 26)) {
                remember = !remember;
                SpatialMenuTheme.click(1.15F);
                return true;
            }
            if (inside(mouseX, mouseY, formX, primaryY, formW, 38)) {
                submit();
                return true;
            }
            if (inside(mouseX, mouseY, formX, switchY - 4, formW, 20)) {
                registerMode = !registerMode;
                status = "";
                if (!registerMode && SpatialLocalAccount.hasAccount()) username.setValue(SpatialLocalAccount.username());
                SpatialMenuTheme.click(1.22F);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            submit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        if (nextScreen != null) minecraft.setScreen(nextScreen);
        else minecraft.setScreen(null);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        // handled in render()
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
