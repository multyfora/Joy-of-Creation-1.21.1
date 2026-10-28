package net.multyfora.client.portable_throttle;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleSignalPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class PortableThrottleGripHandler implements InteractCallback {
    public static final PortableThrottleGripHandler INSTANCE = new PortableThrottleGripHandler();

    private static final double PITCH_PER_FULL_SWEEP = 180.0;

    private boolean gripping;
    private float value;
    private int signal;
    private InteractionHand hand;
    private int slot;

    private PortableThrottleGripHandler() {}

    public boolean isGripping() {
        return gripping;
    }

    public float getValue() {
        return value;
    }

    public int getSignal() {
        return signal;
    }

    @Override
    public Result onUse(int modifiers, int action, KeyMapping mapping) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return Result.empty();

        if (action == GLFW.GLFW_RELEASE) {
            if (gripping) {
                stop();
                return new Result(true);
            }
            return Result.empty();
        }
        if (action != GLFW.GLFW_PRESS) {
            return gripping ? new Result(true) : Result.empty();
        }
        if (gripping) return new Result(true);
        if (throttleHand(player) == null || player.isShiftKeyDown() || isLookingAtLink(mc)) {
            return Result.empty();
        }

        gripping = true;
        hand = throttleHand(player);
        slot = gripSlot(player, hand);
        signal = PortableThrottleItem.getStrength(player.getItemInHand(hand));
        value = signal / 15.0F;
        return new Result(true);
    }

    @Override
    public Result onMouseMove(double yaw, double pitch) {
        if (!gripping) return Result.empty();

        value = Math.clamp(value - (float) (pitch / PITCH_PER_FULL_SWEEP), 0.0F, 1.0F);
        int newSignal = Math.round(value * 15.0F);
        if (newSignal != signal) {
            signal = newSignal;
            ItemStack lever = Minecraft.getInstance().player.getItemInHand(hand);
            if (lever.getItem() instanceof PortableThrottleItem) {
                PortableThrottleItem.setStrength(lever, signal);
            }
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(hand, signal));
            playDetent();
        }
        return new Result(true);
    }

    public void tick() {
        if (!gripping) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !isUseHeld(mc)
                || !(player.getItemInHand(hand).getItem() instanceof PortableThrottleItem)
                || gripSlot(player, hand) != slot) {
            stop();
        }
    }

    private void stop() {
        gripping = false;
        value = signal / 15.0F;
    }

    private void playDetent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        AllSoundEvents.SCROLL_VALUE.playAt(mc.level, mc.player.position(), 0.25F, 0.8F + value * 0.5F, false);
    }

    private static int gripSlot(LocalPlayer player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
    }

    private static boolean isUseHeld(Minecraft mc) {
        InputConstants.Key key = mc.options.keyUse.getKey();
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private static boolean isLookingAtLink(Minecraft mc) {
        return mc.hitResult instanceof BlockHitResult hit
            && hit.getType() == HitResult.Type.BLOCK
            && mc.level != null
            && AllBlocks.REDSTONE_LINK.has(mc.level.getBlockState(hit.getBlockPos()));
    }

    public static InteractionHand throttleHand(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof PortableThrottleItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof PortableThrottleItem) return InteractionHand.OFF_HAND;
        return null;
    }
}
