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


public class PortableTypewriterClientHandler {

    public static Mode MODE = Mode.IDLE;
    public static int selectedKey = -1;
    private static final Collection<Integer> currentlyPressed = new HashSet<>();
    private static int packetCooldown;
    private static final int PACKET_RATE = 5;
    private static BlockPos bindTarget;

    private static final Set<Integer> bindPrevPressed = new HashSet<>();
    private static boolean bindBaselineRecorded = false;

    private static final int HINT_RATE = 40;
    private static int hintCooldown;

    private static final int[] BINDABLE_KEYS = TypewriterLayout.PLACED_KEYS.stream()
        .mapToInt(placed -> placed.key().glfwCode())
        .distinct()
        .toArray();

    public static void toggle() {
        if (MODE == Mode.IDLE) {
            MODE = Mode.ACTIVE;
            hintCooldown = 0;
        } else {
            MODE = Mode.IDLE;
            onReset();
            showHint(Component.translatable("item.joc.portable_typewriter.inactive"));
        }
    }

    public static void toggleBindMode(BlockPos target) {
        MODE = Mode.BIND;
        bindTarget = target;
        bindBaselineRecorded = false;
        bindPrevPressed.clear();
        hintCooldown = 0;
    }

    private static void showHint(Component message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

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

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof PortableTypewriterItem)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof PortableTypewriterItem)) {
                MODE = Mode.IDLE;
                onReset();
                return;
            }
        }

        if (mc.screen != null) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        long window = mc.getWindow().getWindow();

        if (hintCooldown > 0) {
            hintCooldown--;
        } else {
            showHint(Component.translatable(MODE == Mode.BIND
                ? "item.joc.portable_typewriter.bind_prompt"
                : "item.joc.portable_typewriter.active"));
            hintCooldown = HINT_RATE;
        }

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
                    showHint(Component.translatable("item.joc.portable_typewriter.bound",
                        InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName()));
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

        if (MODE == Mode.ACTIVE) {
            Collection<Integer> boundKeys = PortableTypewriterItem.getAllBoundKeys(heldItem);
            Collection<Integer> pressedKeys = new HashSet<>();
            for (int key : boundKeys) {
                if (InputConstants.isKeyDown(window, key)) {
                    pressedKeys.add(key);
                }
            }

            Collection<Integer> newKeys = new HashSet<>(pressedKeys);
            Collection<Integer> releasedKeys = new HashSet<>(currentlyPressed);
            newKeys.removeAll(releasedKeys);
            releasedKeys.removeAll(pressedKeys);

            if (!releasedKeys.isEmpty()) {
                PacketDistributor.sendToServer(new PortableTypewriterInputPacket(releasedKeys, false));
            }

            if (!newKeys.isEmpty()) {
                PacketDistributor.sendToServer(new PortableTypewriterInputPacket(newKeys, true));
                packetCooldown = PACKET_RATE;
            }

            if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
                PacketDistributor.sendToServer(new PortableTypewriterInputPacket(pressedKeys, true));
                packetCooldown = PACKET_RATE;
            }

            currentlyPressed.clear();
            currentlyPressed.addAll(pressedKeys);

            for (KeyMapping mapping : mc.options.keyMappings) {
                InputConstants.Key boundKey = mapping.getKey();
                if (boundKey != null && boundKey.getType() == InputConstants.Type.KEYSYM) {
                    while (mapping.consumeClick()) {}
                    mapping.setDown(false);
                }
            }
        }
    }

    public enum Mode {
        IDLE,
        ACTIVE,
        BIND
    }
}
