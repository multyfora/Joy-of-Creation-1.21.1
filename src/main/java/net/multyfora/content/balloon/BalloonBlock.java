package net.multyfora.content.balloon;

import com.simibubi.create.foundation.block.IBE;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.multyfora.config.JocConfig;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocBlocks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class BalloonBlock extends Block implements IBE<BalloonBlockEntity>, BlockSubLevelAssemblyListener, BlockSubLevelLiftProvider {

    protected final DyeColor color;

    public BalloonBlock(BlockBehaviour.Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        DyeColor dye = DyeColor.getColor(stack);
        if (dye != null && dye != color) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, JocBlocks.BALLOONS.get(dye).get().defaultBlockState());
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.1f - level.random.nextFloat() * 0.2f);
                stack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {}

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return Direction.UP;
    }

    @Override
    public void sable$contributeLiftAndDrag(
            BlockSubLevelLiftProvider.LiftProviderContext ctx, ServerSubLevel subLevel,
            Pose3d localPose, double timeStep,
            Vector3dc linearVelocity, Vector3dc angularVelocity,
            Vector3d linearImpulse, Vector3d angularImpulse,
            @Nullable BlockSubLevelLiftProvider.LiftProviderGroup group
    ) {
        BlockSubLevelLiftProvider.resetVectors();

        BlockSubLevelLiftProvider.LIFT_POS.set(ctx.pos().getX() + 0.5, ctx.pos().getY() + 0.5, ctx.pos().getZ() + 0.5);

        if (localPose != null) {
            localPose.transformPosition(BlockSubLevelLiftProvider.LIFT_POS);
        }

        Pose3d pose = subLevel.logicalPose();
        double pressure = DimensionPhysicsData.getAirPressure(subLevel.getLevel(),
                pose.transformPosition(BlockSubLevelLiftProvider.LIFT_POS, BlockSubLevelLiftProvider.TEMP));

        DimensionPhysicsData.getGravity(subLevel.getLevel(), BlockSubLevelLiftProvider.TEMP, BlockSubLevelLiftProvider.LIFT_FORCE);
        pose.orientation().transformInverse(BlockSubLevelLiftProvider.LIFT_FORCE);

        if (pressure < 1E-5 || BlockSubLevelLiftProvider.LIFT_FORCE.lengthSquared() == 0) {
            BlockSubLevelLiftProvider.resetVectors();
            return;
        }

        BlockSubLevelLiftProvider.LIFT_FORCE.mul(-JocConfig.LIFT_PER_BALLOON.get() * pressure * timeStep);

        if (group != null) {
            group.totalLift().add(BlockSubLevelLiftProvider.LIFT_FORCE);
            group.liftCenter().fma(BlockSubLevelLiftProvider.LIFT_FORCE.length(), BlockSubLevelLiftProvider.LIFT_POS);
            group.totalLiftStrength += BlockSubLevelLiftProvider.LIFT_FORCE.length();
        }

        linearImpulse.add(BlockSubLevelLiftProvider.LIFT_FORCE);
        BlockSubLevelLiftProvider.LIFT_POS.sub(subLevel.getMassTracker().getCenterOfMass(), BlockSubLevelLiftProvider.TEMP);
        angularImpulse.add(BlockSubLevelLiftProvider.TEMP.cross(BlockSubLevelLiftProvider.LIFT_FORCE));

        BlockSubLevelLiftProvider.resetVectors();
    }

    @Override
    public Class<BalloonBlockEntity> getBlockEntityClass() {
        return BalloonBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BalloonBlockEntity> getBlockEntityType() {
        return JocBlockEntityTypes.BALLOON.get();
    }
}