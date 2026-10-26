package net.multyfora.client.portable_typewriter;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.multyfora.network.PortableTypewriterStatePayloads;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

public final class TypewriterTypists {
    private static final Set<Integer> REMOTE_TYPISTS = new HashSet<>();
    private static boolean sentTyping;

    private TypewriterTypists() {}

    public static void set(int entityId, boolean typing) {
        if (typing) {
            REMOTE_TYPISTS.add(entityId);
        } else {
            REMOTE_TYPISTS.remove(entityId);
        }
    }

    public static boolean isTyping(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) {
            return isLocalTyping();
        }
        return REMOTE_TYPISTS.contains(entity.getId());
    }

    private static boolean isLocalTyping() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null
            && TypewriterVisualState.getTypewriterStack(mc.player) != null
            && PortableTypewriterClientHandler.MODE != PortableTypewriterClientHandler.Mode.IDLE;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            sentTyping = false;
            REMOTE_TYPISTS.clear();
            return;
        }
        boolean typing = isLocalTyping();
        if (typing != sentTyping) {
            sentTyping = typing;
            PacketDistributor.sendToServer(new PortableTypewriterStatePayloads.SetTyping(typing));
        }
    }
}
