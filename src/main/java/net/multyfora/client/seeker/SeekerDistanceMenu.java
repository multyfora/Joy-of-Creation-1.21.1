package net.multyfora.client.seeker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.multyfora.index.JocMenuTypes;

public class SeekerDistanceMenu extends AbstractSeekerMenu {
    public SeekerDistanceMenu(
            int containerId, Inventory inventory,
            BlockEntity blockEntity
    ) {
        super(JocMenuTypes.SEEKER_DISTANCE_SCREEN.get(), containerId, inventory, blockEntity);
    }

    public SeekerDistanceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, resolveBlockEntity(inventory, buf));
    }
}
