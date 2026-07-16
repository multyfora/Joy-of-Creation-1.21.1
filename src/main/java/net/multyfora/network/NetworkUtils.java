package net.multyfora.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NetworkUtils {
    @Nullable public static ItemStack getItemIfValid(Player player, Item validItem) {
        if(
            !(player instanceof ServerPlayer serverPlayer)
            || serverPlayer.isSpectator()
        ) {
            return null;
        }

        // Check main hand, offhand, then full inventory
        ItemStack usedItem = player.getMainHandItem();
        if (usedItem.getItem().asItem() == validItem) return usedItem;

        usedItem = player.getOffhandItem();
        if (usedItem.getItem().asItem() == validItem) return usedItem;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem().asItem() == validItem) return stack;
        }
        return null;
    }
}
