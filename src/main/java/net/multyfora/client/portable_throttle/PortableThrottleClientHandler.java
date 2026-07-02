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

/**
 * Client-side handler for the Portable Throttle item.
 * Manages: bind target tracking, left-click detection to open strength screen,
 * keepalive signal sending, and cleanup when the item is deselected.
 **/
public class PortableThrottleClientHandler {

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
        bindTarget = pos;
    }

    // Opens the throttle configuration (frequency) screen
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

    // Updates the current strength and resets the keepalive cooldown
    public static void setStrength(int strength) {
        lastStrength = strength;
        keepAliveCooldown = 0;
    }

    /**
     * Called every client tick: handles bind packet sending, left-click detection,
     * strength screen opening, and keepalive signal transmission
     **/
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player.isSpectator()) {
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

        long window = client.getWindow().getWindow();

        // Send pending bind packet if we have a target
        if (bindTarget != null) {
            PacketDistributor.sendToServer(new PortableThrottleBindPacket(bindTarget));
            bindTarget = null;
        }

        if( client.options.keyAttack.isDown() ) {
            // Open the strength slider screen on the rising edge if no screen is open
            if(client.screen == null) {
                client.setScreen( new PortableThrottleStrengthScreen() );
            }
        }

        // Send keepalive signal while the strength screen is open and strength > 0
        if(0 < lastStrength && client.screen == null) {
            if(0 < keepAliveCooldown) {
                keepAliveCooldown--;
            } else {
                PacketDistributor.sendToServer(
                    new PortableThrottleSignalPacket(lastStrength)
                );
                keepAliveCooldown = KEEP_ALIVE_RATE;
            }
        }
    }

    /**
     * Resets all state: sends strength 0 to server, closes any open throttle screens,
     * and clears tracking variables
     **/
    private static void reset() {
        Screen current = Minecraft.getInstance().screen;
        boolean hadScreen = current instanceof PortableThrottleStrengthScreen || current instanceof PortableThrottleLinkScreen;

        if(hadScreen) {
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(0));
        } else if(0 < lastStrength) {
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(0));
        }

        if(hadScreen) {
            Minecraft.getInstance().setScreen(null);
        }
        lastStrength = 0;
        keepAliveCooldown = 0;
        bindTarget = null;
    }
}
