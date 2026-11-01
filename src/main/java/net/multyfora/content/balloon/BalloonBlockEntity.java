package net.multyfora.content.balloon;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import net.multyfora.config.JocConfig;
import net.multyfora.index.JocBlockEntityTypes;

import java.util.List;

public class BalloonBlockEntity extends SmartBlockEntity {

    private static final int PRESSURE_CHECK_TICKS = 20;

    public BalloonBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.BALLOON.get(), pos, state);
        setLazyTickRate(PRESSURE_CHECK_TICKS);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!(level instanceof ServerLevel serverLevel))
            return;

        double pressure = DimensionPhysicsData.getAirPressure(level,
                Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.atCenterOf(worldPosition)));
        if (pressure < JocConfig.BALLOON_POP_PRESSURE.get())
            BalloonBlock.pop(serverLevel, worldPosition);
    }
}
