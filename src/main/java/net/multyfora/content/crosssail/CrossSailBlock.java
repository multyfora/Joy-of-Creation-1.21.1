package net.multyfora.content.crosssail;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Regular Cross Sail: a propeller-shaped sail block (two crossed blades) used purely
 * as a thrust-generating windmill sail on a Propeller/Windmill Bearing. It intentionally
 * implements NO lift/drag physics interface — outside of a bearing contraption it behaves
 * as an inert decorative block, exactly as the vanilla Create SailBlock does.
 *
 * Counting toward propeller/windmill thrust is done entirely via the "create:windmill_sails"
 * block tag (see the accompanying tag JSON), matching how BearingContraption detects sails —
 * no interface implementation is required for that purpose.
 **/
public class CrossSailBlock extends RotatedPillarBlock implements IWrenchable {

    public CrossSailBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getNearestLookingDirection().getAxis());
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}