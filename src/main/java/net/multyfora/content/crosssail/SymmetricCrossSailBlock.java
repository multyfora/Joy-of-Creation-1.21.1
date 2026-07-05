package net.multyfora.content.crosssail;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Symmetric Cross Sail: similar cross/propeller model as CrossSailBlock, but purely a drag
 * provider — never used for propeller thrust. Unlike the vanilla SymmetricSailBlock (whose
 * drag only opposes velocity along its axis/normal), this block drags velocity in EVERY
 * direction parallel to its face — i.e. any motion that slides across the sail, regardless
 * of direction within that plane, is resisted equally. Motion straight through the axis
 * (perpendicular to the sail's face) produces zero drag.
 **/
public class SymmetricCrossSailBlock extends RotatedPillarBlock implements IWrenchable, BlockSubLevelLiftProvider {

    /** Drag coefficient applied to the in-plane (axis-perpendicular) velocity component. */
    private static final float PLANAR_DRAG_SCALAR = 1.75F;

    public SymmetricCrossSailBlock(Properties properties) {
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

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return Direction.get(AxisDirection.POSITIVE, state.getValue(AXIS));
    }

    // These defaults are unused since we fully override sable$contributeLiftAndDrag below,
    // but kept at zero/off for clarity and in case anything else queries them (e.g. tooltips).
    @Override
    public float sable$getLiftScalar() {
        return 0.0F;
    }

    @Override
    public float sable$getParallelDragScalar() {
        return 0.0F;
    }

    @Override
    public float sable$getDirectionlessDragScalar() {
        return 0.0F;
    }

    /**
     * Custom lift/drag contribution: drags only the velocity component perpendicular to
     * the block's axis (i.e. everything parallel to the sail's face), in every direction
     * within that plane equally. No lift, no axis-aligned drag.
     *
     * Structurally mirrors BlockSubLevelLiftProvider's default implementation so contraption
     * force-grouping (totalDrag/dragCenter/totalDragStrength) stays consistent with how other
     * lift/drag providers report into the same group.
     **/
    @Override
    public void sable$contributeLiftAndDrag(
            LiftProviderContext ctx, ServerSubLevel subLevel, @NotNull Pose3d localPose,
            double timeStep, Vector3dc linearVelocity, Vector3dc angularVelocity,
            Vector3d linearImpulse, Vector3d angularImpulse, @Nullable LiftProviderGroup group
    ) {
        BlockSubLevelLiftProvider.resetVectors();

        LIFT_NORMAL.set(ctx.dir().x(), ctx.dir().y(), ctx.dir().z());
        LIFT_POS.set(ctx.pos().getX() + 0.5, ctx.pos().getY() + 0.5, ctx.pos().getZ() + 0.5);

        if (localPose != null) {
            localPose.transformNormal(LIFT_NORMAL);
            localPose.transformPosition(LIFT_POS);
        }

        Pose3d pose = subLevel.logicalPose();
        double pressure = DimensionPhysicsData.getAirPressure(subLevel.getLevel(), pose.transformPosition(LIFT_POS, TEMP));

        pose.transformPosition(LIFT_POS, TEMP).sub(pose.position());
        LIFT_VELO.set(linearVelocity).add(angularVelocity.cross(TEMP, TEMP));
        pose.transformNormalInverse(LIFT_VELO);

        LIFT_FORCE.zero();

        if (PLANAR_DRAG_SCALAR > 0.0F) {
            double alongNormal = LIFT_NORMAL.dot(LIFT_VELO);

            Vector3d planarVelocity = new Vector3d(LIFT_VELO)
                    .sub(new Vector3d(LIFT_NORMAL).mul(alongNormal));

            double dragStrength = PLANAR_DRAG_SCALAR * pressure * timeStep;
            Vector3d planarDrag = new Vector3d(planarVelocity).mul(dragStrength);

            LIFT_FORCE.add(planarDrag);

            if (group != null) {
                group.totalDrag().sub(planarDrag);
                group.dragCenter().fma(planarDrag.length(), LIFT_POS);
                group.totalDragStrength += planarDrag.length();
            }
        }

        linearImpulse.sub(LIFT_FORCE);
        LIFT_POS.sub(subLevel.getMassTracker().getCenterOfMass(), TEMP);
        angularImpulse.sub(TEMP.cross(LIFT_FORCE));

        BlockSubLevelLiftProvider.resetVectors();
    }
}