package net.multyfora.client.seeker;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.multyfora.client.SeekerPartialModels;
import net.multyfora.network.SeekerPayloads;
import net.neoforged.neoforge.network.PacketDistributor;

public class SeekerScreen extends AbstractSeekerScreen<SeekerMenu> {
    public SeekerScreen(SeekerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.blockEntity);
    }

    @Override
    protected PartialModel setButtonModel() {
        return SeekerPartialModels.SEEKER_SPYGLASS;
    }

    @Override
    protected float setButtonScale() {
        return 30f;
    }

    @Override
    protected void sendTarget(int x, int y, int z) {
        PacketDistributor.sendToServer(new SeekerPayloads.UpdateSeekerPayload(pos, x, y, z));
    }
}
