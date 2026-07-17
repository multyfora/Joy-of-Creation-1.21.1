package net.multyfora.client.portable_typewriter;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

import net.createmod.catnip.outliner.Outliner;
import net.multyfora.client.FreqScreenMenu;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.network.PortableTypewriterBindPacket;
import net.multyfora.network.PortableTypewriterInputPacket;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

/**
 * Client-side handler for the Portable Typewriter item.
 * Manages three modes: IDLE (inactive), ACTIVE (monitoring bound keys), and BIND (waiting
 * to bind a selected key to a redstone link). In ACTIVE mode, it detects key presses/releases
 * for bound keys and sends them to the server as input packets.
 **/
public class PortableTypewriterClientHandler {

    // Current mode of the typewriter
    public static Mode MODE = Mode.IDLE;
    // The key code currently selected in the keyboard GUI
    public static int selectedKey = -1;
    // Set of keys currently reported as pressed (to detect release edges)
    private static final Collection<Integer> currentlyPressed = new HashSet<>();
    // Cooldown for sending keepalive press packets
    private static int packetCooldown;
    private static final int PACKET_RATE = 5;
    // Pending bind target position
    private static BlockPos bindTarget;

    private static final Set<Integer> bindPrevPressed = new HashSet<>();
    private static boolean bindBaselineRecorded = false;

    private static final int[] BINDABLE_KEYS = {
        GLFW.GLFW_KEY_ESCAPE,
        GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5,
        GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0,
        GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_BACKSPACE,
        GLFW.GLFW_KEY_TAB,
        GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R,
        GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I,
        GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P,
        GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_BACKSLASH,
        GLFW.GLFW_KEY_CAPS_LOCK,
        GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F,
        GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L,
        GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_ENTER,
        GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
        GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V,
        GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M,
        GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH,
        GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
        GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
        GLFW.GLFW_KEY_SPACE
    };

    // Toggles between IDLE and ACTIVE mode
    public static void toggle() {
        if (MODE == Mode.IDLE) {
            MODE = Mode.ACTIVE;
        } else {
            MODE = Mode.IDLE;
            onReset();
        }
    }

    public static void toggleBindMode(BlockPos target) {
        MODE = Mode.BIND;
        bindTarget = target;
        bindBaselineRecorded = false;
        bindPrevPressed.clear();
    }

    // Opens the keyboard layout GUI and clears the selected key
    public static void openScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        FreqScreenMenu menu = new FreqScreenMenu(
                JocMenuTypes.TYPEWRITER_SCREEN.get(),
                0,
                mc.player.getInventory());
        mc.setScreen(new PortableTypewriterScreen(menu, mc.player.getInventory(),
                Component.translatable("item.joc.portable_typewriter")));
        selectedKey = -1;
    }

    // Sets the selected key code on both the client handler and the item stack
    public static void setSelectedKey(int glfwKeyCode) {
        selectedKey = glfwKeyCode;
        ItemStack held = getHeldItem();
        if (held != null) {
            PortableTypewriterItem.setSelectedKey(held, glfwKeyCode);
        }
    }

    private static ItemStack getHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof PortableTypewriterItem) return held;
        held = mc.player.getOffhandItem();
        return held.getItem() instanceof PortableTypewriterItem ? held : null;
    }

    // Called when deactivating: sends release events for all currently pressed keys
    private static void onReset() {
        packetCooldown = 0;
        bindTarget = null;
        bindPrevPressed.clear();
        bindBaselineRecorded = false;
        if (!currentlyPressed.isEmpty()) {
            PacketDistributor.sendToServer(new PortableTypewriterInputPacket(currentlyPressed, false));
        }
        currentlyPressed.clear();
    }

    /**
     * Client tick: processes binding, monitors key presses in ACTIVE mode, and sends
     * appropriate packets to the server
     **/
    public static void tick() {
        if (MODE == Mode.IDLE) return;
        if (packetCooldown > 0) packetCooldown--;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        // Check if the player is still holding the typewriter
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof PortableTypewriterItem)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof PortableTypewriterItem)) {
                MODE = Mode.IDLE;
                onReset();
                return;
            }
        }

        // Deactivate if any screen is open (to avoid conflicts)
        if (mc.screen != null) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        long window = mc.getWindow().getWindow();

        if (MODE == Mode.BIND && bindTarget != null) {
            VoxelShape shape = mc.level.getBlockState(bindTarget).getShape(mc.level, bindTarget);
            if (!shape.isEmpty()) {
                Outliner.getInstance()
                    .showAABB("typewriter_bind", shape.bounds().move(bindTarget))
                    .colored(0xB74B2D)
                    .lineWidth(1/16f);
            }

            if (!bindBaselineRecorded) {
                bindPrevPressed.clear();
                for (int key : BINDABLE_KEYS) {
                    if (InputConstants.isKeyDown(window, key)) {
                        bindPrevPressed.add(key);
                    }
                }
                bindBaselineRecorded = true;
                return;
            }

            for (int key : BINDABLE_KEYS) {
                boolean currentlyDown = InputConstants.isKeyDown(window, key);
                boolean wasDown = bindPrevPressed.contains(key);
                if (currentlyDown && !wasDown) {
                    PacketDistributor.sendToServer(new PortableTypewriterBindPacket(key, bindTarget));
                    MODE = Mode.IDLE;
                    bindTarget = null;
                    bindPrevPressed.clear();
                    bindBaselineRecorded = false;
                    return;
                }
                if (wasDown != currentlyDown) {
                    if (currentlyDown) bindPrevPressed.add(key);
                    else bindPrevPressed.remove(key);
                }
            }
            return;
        }

        // ACTIVE mode: monitor bound keys
        if (MODE == Mode.ACTIVE) {
            Collection<Integer> boundKeys = PortableTypewriterItem.getAllBoundKeys(heldItem);
            Collection<Integer> pressedKeys = new HashSet<>();
            for (int key : boundKeys) {
                if (InputConstants.isKeyDown(window, key)) {
                    pressedKeys.add(key);
                }
            }

            // Compute diff: newly pressed keys and newly released keys
            Collection<Integer> newKeys = new HashSet<>(pressedKeys);
            Collection<Integer> releasedKeys = new HashSet<>(currentlyPressed);
            newKeys.removeAll(releasedKeys);
            releasedKeys.removeAll(pressedKeys);

            // Send release events
            if (!releasedKeys.isEmpty()) {
                PacketDistributor.sendToServer(new PortableTypewriterInputPacket(releasedKeys, false));
            }

            // Send press events for newly pressed keys
            if (!newKeys.isEmpty()) {
                PacketDistributor.sendToServer(new PortableTypewriterInputPacket(newKeys, true));
                packetCooldown = PACKET_RATE;
            }

            // Periodic keepalive for held keys to prevent server timeout
            if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
                PacketDistributor.sendToServer(new PortableTypewriterInputPacket(pressedKeys, true));
                packetCooldown = PACKET_RATE;
            }

            currentlyPressed.clear();
            currentlyPressed.addAll(pressedKeys);

            // Consume all key input to prevent normal key actions while in typewriter mode
            KeyMapping.releaseAll();
            for (KeyMapping mapping : mc.options.keyMappings) {
                InputConstants.Key boundKey = mapping.getKey();
                if (boundKey != null && boundKey.getType() == InputConstants.Type.KEYSYM) {
                    mapping.consumeClick();
                    mapping.setDown(false);
                }
            }
        }
    }

    // State machine for the typewriter
    public enum Mode {
        IDLE,    // Not active; item is just an item
        ACTIVE,  // Monitoring bound keys and sending press/release events
        BIND     // Waiting to bind a selected key to a redstone link
    }
}
