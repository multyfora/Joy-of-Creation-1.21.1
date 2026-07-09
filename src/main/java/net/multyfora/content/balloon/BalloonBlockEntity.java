package net.multyfora.content.balloon;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.multyfora.index.JocBlockEntityTypes;

import java.util.List;

public class BalloonBlockEntity extends SmartBlockEntity {

    public BalloonBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.BALLOON.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }
}
