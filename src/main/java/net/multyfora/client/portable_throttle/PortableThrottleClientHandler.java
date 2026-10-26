package net.multyfora.client.portable_throttle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import net.multyfora.client.FreqScreenMenu;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.network.PortableThrottleBindPacket;
import net.multyfora.network.PortableThrottleSignalPacket;

public class PortableThrottleClientHandler {

    private static BlockPos bindTarget;
    private static int lastStrength;
    private static int keepAliveCooldown;
    private static final int KEEP_ALIVE_RATE = 10;

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
        if (throttleInHand && client.screen == null && client.options.keyUse.isDown() && client.options.keyShift.isDown()) {
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(lastStrength));
        }

        if (0 < lastStrength && client.screen == null) {
            if (0 < keepAliveCooldown) {
                keepAliveCooldown--;
            } else {
                PacketDistributor.sendToServer(new PortableThrottleSignalPacket(lastStrength));
                keepAliveCooldown = KEEP_ALIVE_RATE;
            }
        }
    }

    private static void reset() {
        if (0 < lastStrength) {
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(0));
        }
        if (Minecraft.getInstance().screen instanceof PortableThrottleLinkScreen) {
            Minecraft.getInstance().setScreen(null);
        }
        lastStrength = 0;
        keepAliveCooldown = 0;
        bindTarget = null;
    }
}
