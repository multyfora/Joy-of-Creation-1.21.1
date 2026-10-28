package net.multyfora.client.portable_throttle;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottlePosePayloads;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public final class ThrottleVisualState {
    private static final float GRIP_SPEED = 5.0F;
    private static final float KNOB_SPEED = 16.0F;

    private static final class Remote {
        boolean gripping;
        int strength;
        float grip;
        float knob;
    }

    private static final Map<Integer, Remote> REMOTE = new HashMap<>();

    private static float grip;
    private static float knob;
    private static long lastNanos;

    private static boolean sentGripping;
    private static int sentStrength = -1;

    private ThrottleVisualState() {}

    public static void update() {
        long now = Util.getNanos();
        float dt = lastNanos == 0 ? 0.0F : Math.min((now - lastNanos) / 1.0e9F, 0.1F);
        lastNanos = now;

        PortableThrottleGripHandler handler = PortableThrottleGripHandler.INSTANCE;
        grip = approach(grip, handler.isGripping() ? 1.0F : 0.0F, GRIP_SPEED * dt);
        float knobTarget = handler.isGripping() ? handler.getValue() : localStrength() / 15.0F;
        knob = approach(knob, knobTarget, KNOB_SPEED * dt);

        for (Remote remote : REMOTE.values()) {
            remote.grip = approach(remote.grip, remote.gripping ? 1.0F : 0.0F, GRIP_SPEED * dt);
            remote.knob = approach(remote.knob, remote.strength / 15.0F, KNOB_SPEED * dt);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.getConnection() == null || player == null) {
            sentStrength = -1;
            REMOTE.clear();
            return;
        }
        if (PortableThrottleGripHandler.throttleHand(player) == null) {
            sentStrength = -1;
            return;
        }
        boolean gripping = PortableThrottleGripHandler.INSTANCE.isGripping();
        int strength = localStrength();
        if (gripping != sentGripping || strength != sentStrength) {
            sentGripping = gripping;
            sentStrength = strength;
            PacketDistributor.sendToServer(new PortableThrottlePosePayloads.SetPose(gripping, strength));
        }
    }

    public static void set(int entityId, boolean gripping, int strength) {
        Remote remote = REMOTE.computeIfAbsent(entityId, id -> new Remote());
        remote.gripping = gripping;
        remote.strength = strength;
    }

    public static float getGrip(Entity entity) {
        if (entity == Minecraft.getInstance().player) return smooth(grip);
        Remote remote = REMOTE.get(entity.getId());
        return remote == null ? 0.0F : smooth(remote.grip);
    }

    public static float getKnob(Entity entity) {
        if (entity == Minecraft.getInstance().player) return knob;
        Remote remote = REMOTE.get(entity.getId());
        return remote == null ? 0.0F : remote.knob;
    }

    public static int getStrength(Entity entity) {
        if (entity == Minecraft.getInstance().player) return localStrength();
        Remote remote = REMOTE.get(entity.getId());
        return remote == null ? 0 : remote.strength;
    }

    public static boolean isGripping(Entity entity) {
        if (entity == Minecraft.getInstance().player) return PortableThrottleGripHandler.INSTANCE.isGripping();
        Remote remote = REMOTE.get(entity.getId());
        return remote != null && remote.gripping;
    }

    public static boolean isActiveStack(Entity holder, ItemStack stack) {
        if (!(holder instanceof Player player)) return false;
        InteractionHand hand = activeHand(player);
        return hand != null && player.getItemInHand(hand) == stack;
    }

    private static int localStrength() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;
        PortableThrottleGripHandler handler = PortableThrottleGripHandler.INSTANCE;
        if (handler.isGripping()) return handler.getSignal();
        InteractionHand hand = activeHand(player);
        return hand == null ? 0 : PortableThrottleItem.getStrength(player.getItemInHand(hand));
    }

    private static InteractionHand activeHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof PortableThrottleItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof PortableThrottleItem) return InteractionHand.OFF_HAND;
        return null;
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
