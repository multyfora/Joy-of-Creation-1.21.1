package net.multyfora.client.portable_throttle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleBindPacket;
import net.multyfora.network.PortableThrottleSignalPacket;

import org.slf4j.Logger;

/**
 * Client-side handler for the Portable Throttle item.
 * Manages: bind target tracking, left-click detection to open strength screen,
 * keepalive signal sending, and cleanup when the item is deselected.
 **/
public class PortableThrottleClientHandler {

    private static final Logger LOGGER = AeronauticsJoyofcreation.LOGGER;

    // Tracks left mouse button edge (was-down state)
    private static boolean wasLeftDown;
    // Pending bind target, set when right-clicking a redstone link
    private static BlockPos bindTarget;
    // Last set strength value; 0 means inactive
    private static int lastStrength;
    // Cooldown counter for sending keepalive packets
    private static int keepAliveCooldown;
    // Send keepalive every N ticks
    private static final int KEEP_ALIVE_RATE = 10;

    // Records a bind target; the actual packet is sent on the next tick
    public static void startBind(BlockPos pos) {
        LOGGER.info("[THROTTLE_CLIENT] startBind: pos={}", pos);
        bindTarget = pos;
    }

    // Opens the throttle configuration (frequency) screen
    public static void openScreen() {
        LOGGER.info("[THROTTLE_CLIENT] openScreen: opening PortableThrottleScreen (config)");
        Minecraft.getInstance().setScreen(new PortableThrottleScreen());
    }

    public static int getLastStrength() {
        return lastStrength;
    }

    // Updates the current strength and resets the keepalive cooldown
    public static void setStrength(int strength) {
        LOGGER.info("[THROTTLE_CLIENT] setStrength: lastStrength={} -> {}", lastStrength, strength);
        lastStrength = strength;
        keepAliveCooldown = 0;
    }

    /**
     * Called every client tick: handles bind packet sending, left-click detection,
     * strength screen opening, and keepalive signal transmission
     **/
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            LOGGER.info("[THROTTLE_CLIENT] tick: player null or spectator, resetting");
            reset();
            return;
        }

        // Check if the player is holding the throttle in either hand
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof PortableThrottleItem)) {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof PortableThrottleItem)) {
                reset();
                return;
            }
        }

        long window = mc.getWindow().getWindow();

        // Send pending bind packet if we have a target
        if (bindTarget != null) {
            LOGGER.info("[THROTTLE_CLIENT] tick: sending bind packet for {}", bindTarget);
            PacketDistributor.sendToServer(new PortableThrottleBindPacket(bindTarget));
            bindTarget = null;
        }

        // Detect left-click (mouse button 1)
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;

        if (leftDown) {
            if (!wasLeftDown) {
                LOGGER.info("[THROTTLE_CLIENT] tick: left click DETECTED (edge), screen={}", mc.screen);
            }
            // Consume the attack action so the player doesn't swing
            mc.options.keyAttack.consumeClick();
            mc.options.keyAttack.setDown(false);

            // Open the strength slider screen on the rising edge if no screen is open
            if (!wasLeftDown && mc.screen == null) {
                LOGGER.info("[THROTTLE_CLIENT] tick: left click edge + no screen -> opening PortableThrottleStrengthScreen");
                mc.setScreen(new PortableThrottleStrengthScreen());
            }
        }

        wasLeftDown = leftDown;

        // Send keepalive signal while the strength screen is open and strength > 0
        if (lastStrength > 0 && mc.screen == null) {
            if (keepAliveCooldown > 0) {
                keepAliveCooldown--;
            } else {
                LOGGER.info("[THROTTLE_CLIENT] tick: sending KEEPALIVE signal strength={}", lastStrength);
                PacketDistributor.sendToServer(new PortableThrottleSignalPacket(lastStrength));
                keepAliveCooldown = KEEP_ALIVE_RATE;
            }
        }
    }

    /**
     * Resets all state: sends strength 0 to server, closes any open throttle screens,
     * and clears tracking variables
     **/
    private static void reset() {
        LOGGER.info("[THROTTLE_CLIENT] reset: called, lastStrength={} bindTarget={}", lastStrength, bindTarget);
        Screen current = Minecraft.getInstance().screen;
        boolean hadScreen = current instanceof PortableThrottleStrengthScreen || current instanceof PortableThrottleScreen;

        if (hadScreen) {
            LOGGER.info("[THROTTLE_CLIENT] reset: screen open, sending strength 0 before close");
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(0));
        } else if (lastStrength > 0) {
            LOGGER.info("[THROTTLE_CLIENT] reset: sending strength 0 (shutdown)");
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(0));
        }

        if (hadScreen) {
            LOGGER.info("[THROTTLE_CLIENT] reset: closing open screen");
            Minecraft.getInstance().setScreen(null);
        }
        lastStrength = 0;
        keepAliveCooldown = 0;
        bindTarget = null;
        LOGGER.info("[THROTTLE_CLIENT] reset: complete");
    }
}
