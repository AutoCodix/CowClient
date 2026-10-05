package dev.spatialclient.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class SpatialAccountScreen extends Screen {
    private EditBox username;
    private EditBox password;
    private boolean registerMode;
    private boolean remember = true;
    private String status = "";

    public SpatialAccountScreen() {
        super(Component.literal("Spatial Client"));
        SpatialLocalAccount.init();
        registerMode = !SpatialLocalAccount.hasAccount();
    }

    @Override
    protected void init() {
        int cardW = Math.min(390, width - 36);
        int left = (width - cardW) / 2;
        int top = Math.max(54, height / 2 - 145);

        username = new EditBox(font, left + 36, top + 92, cardW - 72, 24, Component.literal("Username"));
        username.setMaxLength(20);
        if (!registerMode && SpatialLocalAccount.hasAccount()) username.setValue(SpatialLocalAccount.username());
        addRenderableWidget(username);

        password = new EditBox(font, left + 36, top + 134, cardW - 72, 24, Component.literal("Password"));
        password.setMaxLength(64);
        addRenderableWidget(password);

        addRenderableWidget(Button.builder(Component.literal(remember ? "Remember this device: ON" : "Remember this device: OFF"), b -> {
            remember = !remember;
            b.setMessage(Component.literal(remember ? "Remember this device: ON" : "Remember this device: OFF"));
        }).bounds(left + 36, top + 174, cardW - 72, 22).build());

        addRenderableWidget(Button.builder(Component.literal(registerMode ? "Create local profile" : "Sign in"), b -> submit())
                .bounds(left + 36, top + 208, cardW - 72, 26).build());

        addRenderableWidget(Button.builder(Component.literal(registerMode ? "I already have a profile" : "Create a new local profile"), b -> {
            if (SpatialLocalAccount.hasAccount()) {
                registerMode = false;
                username.setValue(SpatialLocalAccount.username());
                status = "";
            } else {
                registerMode = true;
                username.setValue("");
                status = "";
            }
        }).bounds(left + 36, top + 242, cardW - 72, 22).build());

        setInitialFocus(registerMode ? username : password);
    }

    private void submit() {
        SpatialLocalAccount.Result result = registerMode
                ? SpatialLocalAccount.register(username.getValue(), password.getValue(), remember)
                : SpatialLocalAccount.login(username.getValue(), password.getValue(), remember);
        status = result.message();
        password.setValue("");
        if (result.ok()) openSpatial();
    }

    private void openSpatial() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        SpatialCamera.open();
        if (SpatialCamera.isMenuCameraActive()) minecraft.setScreen(new SpatialMenuScreen());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawSpace(g);

        int cardW = Math.min(390, width - 36);
        int cardH = 300;
        int left = (width - cardW) / 2;
        int top = Math.max(54, height / 2 - 145);
        int accent = 0xFF000000 | dev.spatialclient.config.SpatialConfig.get().accentRgb;

        UiRenderer.shadow(g, left, top, cardW, cardH, 18);
        UiRenderer.roundedOutline(g, left, top, cardW, cardH, 18, 1, 0xFF2A315A, 0xEE080B18);
        UiRenderer.roundedRect(g, left + 18, top + 18, 42, 42, 13, 0xFF101735);
        UiRenderer.circle(g, left + 39, top + 39, 10.5F, accent);
        UiRenderer.circle(g, left + 39, top + 39, 6.0F, 0xFF080B18);

        UiFont.draw(g, registerMode ? "Create Spatial profile" : "Welcome back", left + 74, top + 20,
                0xFFF4F6FF, 0.48F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "Local profile only - credentials never leave this device.", left + 74, top + 43,
                0xFF7781A3, 0.235F, UiFont.Weight.REGULAR);

        UiFont.draw(g, "USERNAME", left + 36, top + 78, 0xFF69718F, 0.22F, UiFont.Weight.SEMIBOLD);
        UiFont.draw(g, "PASSWORD", left + 36, top + 120, 0xFF69718F, 0.22F, UiFont.Weight.SEMIBOLD);

        if (!status.isBlank()) {
            UiRenderer.roundedRect(g, left + 36, top + 272, cardW - 72, 18, 7, 0x99212A48);
            UiFont.draw(g, status, left + 45, top + 276, status.toLowerCase().contains("incorrect") ? 0xFFFF9AA8 : 0xFFAFC6FF,
                    0.22F, UiFont.Weight.REGULAR);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawSpace(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF02040D);
        int[][] stars = {
                {7, 12}, {16, 27}, {28, 9}, {36, 22}, {49, 14}, {61, 31}, {73, 11}, {87, 23}, {95, 7},
                {12, 62}, {24, 48}, {39, 71}, {52, 57}, {66, 83}, {79, 52}, {91, 69}, {5, 88},
                {31, 91}, {58, 96}, {82, 92}
        };
        for (int i = 0; i < stars.length; i++) {
            int x = stars[i][0] * width / 100;
            int y = stars[i][1] * height / 100;
            int c = (i % 5 == 0) ? 0xFF8CB7FF : (i % 7 == 0 ? 0xFFB69CFF : 0xFF60709B);
            g.fill(x, y, x + (i % 6 == 0 ? 2 : 1), y + (i % 6 == 0 ? 2 : 1), c);
        }
        g.fill(0, 0, width, height / 4, 0x22123A74);
        g.fill(0, height * 3 / 4, width, height, 0x221B0D45);
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
    public boolean isPauseScreen() {
        return false;
    }
}
