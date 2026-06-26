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

        // Check that the used item is a PORTABLE-TYPEWRITER
        ItemStack usedItem = player.getMainHandItem();
        if( usedItem.getItem().asItem() != validItem ) {
            usedItem = player.getOffhandItem();
            if( usedItem.getItem().asItem() != validItem ) {
                return null;
            }
        }
        return usedItem;
    }
}
