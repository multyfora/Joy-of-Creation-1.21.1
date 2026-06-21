package net.multyfora.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FreqScreenMenu extends AbstractContainerMenu {

    public FreqScreenMenu(MenuType<?> type, int containerId, Inventory playerInv) {
        super(type, containerId);
        addSlot(new Slot(playerInv, 0, Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    public FreqScreenMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this((MenuType<?>) null, containerId, playerInv);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
