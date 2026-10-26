package net.multyfora.client.portable_throttle;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleSignalPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class PortableThrottleGripHandler implements InteractCallback {
    public static final PortableThrottleGripHandler INSTANCE = new PortableThrottleGripHandler();

    private static final double PITCH_PER_FULL_SWEEP = 180.0;
    private static final int HINT_RATE = 30;

    private boolean gripping;
    private float value;
    private int signal;
    private int hintCooldown;

    private PortableThrottleGripHandler() {}

    public boolean isGripping() {
        return gripping;
    }

    public float getValue() {
        return value;
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
        signal = PortableThrottleClientHandler.getLastStrength();
        value = signal / 15.0F;
        hintCooldown = 0;
        return new Result(true);
    }

    @Override
    public Result onMouseMove(double yaw, double pitch) {
        if (!gripping) return Result.empty();

        value = Math.clamp(value - (float) (pitch / PITCH_PER_FULL_SWEEP), 0.0F, 1.0F);
        int newSignal = Math.round(value * 15.0F);
        if (newSignal != signal) {
            signal = newSignal;
            PortableThrottleClientHandler.setStrength(signal);
            PacketDistributor.sendToServer(new PortableThrottleSignalPacket(signal));
            playDetent();
            hintCooldown = 0;
        }
        return new Result(true);
    }

    public void tick() {
        if (!gripping) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || throttleHand(player) == null || !isUseHeld(mc)) {
            stop();
            return;
        }
        if (hintCooldown-- <= 0) {
            player.displayClientMessage(Component.translatable("screen.joc.portable_throttle.bound_strength", signal), true);
            hintCooldown = HINT_RATE;
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
