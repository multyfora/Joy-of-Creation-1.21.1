package net.multyfora.content.balloon;

import com.simibubi.create.foundation.block.IBE;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import net.multyfora.index.JocBlockEntityTypes;

/**
 * Balloon block: a decorative/functional block available in 16 dye colors.
 * Implements IBE for Create's SmartBlockEntity integration and BlockSubLevelAssemblyListener
 * for Sable sublevel physics (balloons are super-light and can be assembled onto ships).
 **/
public class BalloonBlock extends Block implements IBE<BalloonBlockEntity>, BlockSubLevelAssemblyListener {
    // Integer property 0-15 representing the dye color index
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 15);

    public BalloonBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Default to color 0 (white/gray) on placement
        registerDefaultState(stateDefinition.any().setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    // Ensure the block entity is removed when the block is destroyed
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    // Called after a sublevel assembly moves the block between levels; no action needed
    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {}

    @Override
    public Class<BalloonBlockEntity> getBlockEntityClass() {
        return BalloonBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BalloonBlockEntity> getBlockEntityType() {
        return JocBlockEntityTypes.BALLOON.get();
    }
}
