package net.multyfora.client.portable_throttle;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

import net.multyfora.client.FreqScreenMenu;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.network.PortableThrottleBindPacket;
import net.multyfora.network.PortableThrottleSignalPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortableThrottleClientHandler {

    private static boolean wasLeftDown;
    private static BlockPos bindTarget;
    private static int lastStrength;
    private static int keepAliveCooldown;
    private static final int KEEP_ALIVE_RATE = 10;

    private static float liveValue;
    private static final Map<UUID, Float> smoothedStrength = new HashMap<>();
    private static final Map<UUID, Float> prevSmoothedStrength = new HashMap<>();

    private static float gripProgress;

    public static void startBind(BlockPos pos) {
        bindTarget = pos;
    }

    public static void openScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        FreqScreenMenu menu = new FreqScreenMenu(
                JocMenuTypes.THROTTLE_SCREEN.get(),
                0,
                mc.player.getInventory());
        mc.setScreen(new PortableThrottleLinkScreen(menu, mc.player.getInventory(),
                Component.translatable("item.joc.portable_throttle")));
    }

    public static int getLastStrength() {
        return lastStrength;
    }

    public static void setStrength(int strength) {
        lastStrength = strength;
        keepAliveCooldown = 0;
    }

    public static void setLiveValue(float value) {
        liveValue = value;
    }

    public static float getLiveValue() {
        return liveValue;
    }

    public static float getSmoothedStrength(UUID uuid) {
        return smoothedStrength.getOrDefault(uuid, lastStrength / 15f);
    }

    public static void updateSmoothedStrength(UUID uuid, float target) {
        float current = smoothedStrength.getOrDefault(uuid, target);
        float smoothed = current * 0.15f + target * 0.85f;
        prevSmoothedStrength.put(uuid, current);
        smoothedStrength.put(uuid, smoothed);
    }

    public static void tickGrip() {
        Minecraft mc = Minecraft.getInstance();
        boolean open = mc.screen instanceof PortableThrottleStrengthScreen;
        float target = open ? 1f : 0f;
        gripProgress += (target - gripProgress) * 0.3f;
    }

    public static float getGripProgress() {
        return gripProgress;
    }

    private static ItemStack findThrottleInInventory(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof PortableThrottleItem) return held;

        held = player.getOffhandItem();
        if (held.getItem() instanceof PortableThrottleItem) return held;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof PortableThrottleItem) return stack;
        }

        return null;
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player.isSpectator()) {
            reset();
            return;
        }

        if (findThrottleInInventory(player) == null) {
            reset();
            return;
        }

        if (bindTarget != null) {
            PacketDistributor.sendToServer(new PortableThrottleBindPacket(bindTarget));
            bindTarget = null;
        }

        boolean throttleInHand = player.getMainHandItem().getItem() instanceof PortableThrottleItem
            || player.getOffhandItem().getItem() instanceof PortableThrottleItem;
        if (throttleInHand && client.options.keyUse.isDown()) {
            if (client.screen == null) {
                if (client.options.keyShift.isDown()) {
                    PacketDistributor.sendToServer(
                            new PortableThrottleSignalPacket(lastStrength)
                    );
                } else {
                    client.setScreen(
                            new PortableThrottleStrengthScreen()
                    );
                }
            }
        }

        if (0 < lastStrength && client.screen == null) {
            if (0 < keepAliveCooldown) {
                keepAliveCooldown--;
            } else {
                PacketDistributor.sendToServer(
                        new PortableThrottleSignalPacket(lastStrength)
                );
                keepAliveCooldown = KEEP_ALIVE_RATE;
            }
        }

        tickGrip();

        updateSmoothedStrength(player.getUUID(), lastStrength / 15f);
    }

    private static void reset() {
        Screen current = Minecraft.getInstance().screen;
        boolean hadScreen = current instanceof PortableThrottleStrengthScreen || current instanceof PortableThrottleLinkScreen;

        if (hadScreen) {
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(0));
        } else if (0 < lastStrength) {
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(0));
        }

        if (hadScreen) {
            Minecraft.getInstance().setScreen(null);
        }
        lastStrength = 0;
        keepAliveCooldown = 0;
        bindTarget = null;
        liveValue = 0f;
        gripProgress = 0f;
    }
}
