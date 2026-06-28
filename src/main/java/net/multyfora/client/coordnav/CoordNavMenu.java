package net.multyfora.client.coordnav;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.multyfora.content.coordnav.CoordNavBlockEntity;
import net.multyfora.index.JocMenuTypes;

public class CoordNavMenu extends AbstractContainerMenu {
    public final CoordNavBlockEntity blockEntity;
    private final Level level;

    public CoordNavMenu(
        int containerId, Inventory inventory,
        BlockEntity blockEntity
    ) {
        super(JocMenuTypes.THROTTLE_SCREEN.get(), containerId);
        this.blockEntity = (CoordNavBlockEntity)blockEntity;
        this.level = inventory.player.level();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}
