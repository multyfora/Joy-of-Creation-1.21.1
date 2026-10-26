package net.multyfora.client.portable_typewriter;

import com.mojang.blaze3d.platform.InputConstants;
import dev.simulated_team.simulated.index.SimSoundEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TypewriterVisualState {
    private static final float HOLD_SPEED = 3.0F;
    private static final float PRESS_SPEED = 40.0F;
    private static final float RELEASE_SPEED = 14.0F;
    private static final float DIP_SPEED = 18.0F;
    private static final float DIP_RELEASE_SPEED = 7.0F;

    private static final Map<Integer, Float> PRESS = new HashMap<>();
    private static final Set<Integer> DOWN = new HashSet<>();

    private static float hold;
    private static float leftDip;
    private static float rightDip;
    private static long lastNanos;

    private TypewriterVisualState() {}

    public static void update() {
        long now = Util.getNanos();
        float dt = lastNanos == 0 ? 0.0F : Math.min((now - lastNanos) / 1.0e9F, 0.1F);
        lastNanos = now;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean typing = player != null
            && getTypewriterStack(player) != null
            && PortableTypewriterClientHandler.MODE != PortableTypewriterClientHandler.Mode.IDLE
            && mc.screen == null;

        hold = approach(hold, typing ? 1.0F : 0.0F, HOLD_SPEED * dt);

        long window = mc.getWindow().getWindow();
        float half = TypewriterLayout.ROW_UNITS / 2.0F;
        boolean leftTyping = false;
        boolean rightTyping = false;
        for (TypewriterLayout.PlacedKey placed : TypewriterLayout.PLACED_KEYS) {
            int code = placed.key().glfwCode();
            boolean isDown = typing && hold > 0.6F && InputConstants.isKeyDown(window, code);

            if (isDown && DOWN.add(code)) {
                playTap(mc, true);
            } else if (!isDown && DOWN.remove(code)) {
                playTap(mc, false);
            }

            float press = PRESS.getOrDefault(code, 0.0F);
            press = approach(press, isDown ? 1.0F : 0.0F, (isDown ? PRESS_SPEED : RELEASE_SPEED) * dt);
            if (press <= 0.0F) {
                PRESS.remove(code);
            } else {
                PRESS.put(code, press);
            }

            if (isDown) {
                if (placed.centerX() < half) {
                    leftTyping = true;
                } else {
                    rightTyping = true;
                }
            }
        }

        leftDip = approach(leftDip, leftTyping ? 1.0F : 0.0F, (leftTyping ? DIP_SPEED : DIP_RELEASE_SPEED) * dt);
        rightDip = approach(rightDip, rightTyping ? 1.0F : 0.0F, (rightTyping ? DIP_SPEED : DIP_RELEASE_SPEED) * dt);
    }

    private static void playTap(Minecraft mc, boolean down) {
        if (mc.level == null || mc.player == null) return;
        float pitch = 0.9F + mc.level.random.nextFloat() * 0.25F;
        if (down) {
            SimSoundEvents.LINKED_TYPEWRITER_TAP.playAt(mc.level, mc.player.getEyePosition(), 0.25F, pitch, false);
        } else {
            SimSoundEvents.LINKED_TYPEWRITER_UNTAP.playAt(mc.level, mc.player.getEyePosition(), 0.12F, pitch, false);
        }
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    public static ItemStack getTypewriterStack(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof PortableTypewriterItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof PortableTypewriterItem) return player.getOffhandItem();
        return null;
    }

    public static float getHold() {
        return hold;
    }

    public static float getPress(int glfwCode) {
        return PRESS.getOrDefault(glfwCode, 0.0F);
    }

    public static boolean isDown(int glfwCode) {
        return DOWN.contains(glfwCode);
    }

    public static float getLeftDip() {
        return smooth(leftDip);
    }

    public static float getRightDip() {
        return smooth(rightDip);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    public static float ease(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return smooth(t);
    }
}
