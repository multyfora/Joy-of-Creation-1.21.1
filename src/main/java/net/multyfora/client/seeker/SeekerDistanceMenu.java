package net.multyfora.client.seeker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.index.JocMenuTypes;

import org.jetbrains.annotations.NotNull;

public class SeekerDistanceMenu extends AbstractContainerMenu {
    public final SeekerBlockEntity blockEntity;
    private final Level level;

    public SeekerDistanceMenu(
            int containerId, Inventory inventory,
            BlockEntity blockEntity
    ) {
        super(JocMenuTypes.SEEKER_DISTANCE_SCREEN.get(), containerId);
        this.blockEntity = (SeekerBlockEntity) blockEntity;
        this.level = inventory.player.level();
    }

    public SeekerDistanceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, resolveBlockEntity(inventory, buf));
    }

    private static BlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = inventory.player.level();
        return level.getBlockEntity(pos);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public Level getLevel() {
        return level;
    }
}
