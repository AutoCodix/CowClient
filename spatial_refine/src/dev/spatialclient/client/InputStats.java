package dev.spatialclient.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public final class InputStats {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private static boolean lastLeft;
    private static boolean lastRight;
    private static Object lastLevel;
    private static long sessionStarted = System.currentTimeMillis();

    private InputStats() {}

    public static void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        Object level = mc.level;
        if (level != lastLevel) {
            lastLevel = level;
            sessionStarted = now;
            LEFT.clear();
            RIGHT.clear();
        }

        if (mc.getWindow() == null) return;
        long window = mc.getWindow().getWindow();
        boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (mc.screen == null && mc.player != null) {
            if (left && !lastLeft) LEFT.addLast(now);
            if (right && !lastRight) RIGHT.addLast(now);
        }

        lastLeft = left;
        lastRight = right;
        trim(LEFT, now);
        trim(RIGHT, now);
    }

    public static int leftCps() {
        trim(LEFT, System.currentTimeMillis());
        return LEFT.size();
    }

    public static int rightCps() {
        trim(RIGHT, System.currentTimeMillis());
        return RIGHT.size();
    }

    public static long sessionSeconds() {
        return Math.max(0L, (System.currentTimeMillis() - sessionStarted) / 1000L);
    }

    private static void trim(Deque<Long> queue, long now) {
        while (!queue.isEmpty() && now - queue.peekFirst() > 1000L) queue.removeFirst();
    }
}
