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

public class PortableThrottleClientHandler {

    private static BlockPos bindTarget;

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
    }

    private static void reset() {
        if (Minecraft.getInstance().screen instanceof PortableThrottleLinkScreen) {
            Minecraft.getInstance().setScreen(null);
        }
        bindTarget = null;
    }
}
