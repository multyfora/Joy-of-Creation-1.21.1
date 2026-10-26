package net.multyfora.client;

import net.minecraft.world.entity.player.Player;

public final class RenderingPlayer {
    private static final ThreadLocal<Player> CURRENT = new ThreadLocal<>();

    private RenderingPlayer() {}

    public static void set(Player player) {
        CURRENT.set(player);
    }

    public static Player get() {
        return CURRENT.get();
    }
}
