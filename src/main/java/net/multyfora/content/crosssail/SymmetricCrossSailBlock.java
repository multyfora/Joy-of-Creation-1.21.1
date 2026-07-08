package net.multyfora.content.crosssail;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.utility.BlockHelper;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.multyfora.index.JocBlocks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class SymmetricCrossSailBlock extends RotatedPillarBlock implements IWrenchable, BlockSubLevelLiftProvider {

    private static final float PLANAR_DRAG_SCALAR = 1.75F;

    private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());
    private static final int sidePlacementHelperId = PlacementHelpers.register(new SidePlacementHelper());

    protected final DyeColor color;

    public SymmetricCrossSailBlock(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    private static final VoxelShaper SHAPE = net.createmod.catnip.math.VoxelShaper.forAxis(
            Shapes.or(Block.box(6, 0, 0, 10, 16, 16), Block.box(0, 0.01, 6, 16, 15.99, 10)),
            Direction.Axis.Y
    );

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getNearestLookingDirection().getAxis());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(AXIS));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Dye handling takes priority over placement helpers
        DyeColor clickedColor = DyeColor.getColor(stack);
        if (clickedColor != null) {
            if (!level.isClientSide) {
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS,
                        1.0f, 1.1f - level.random.nextFloat() * 0.2f);
            }
            applyDye(state, level, pos, hitResult.getLocation(), clickedColor);
            return ItemInteractionResult.SUCCESS;
        }

        if (!player.isShiftKeyDown() && player.mayBuild()) {

            IPlacementHelper stackHelper = PlacementHelpers.get(placementHelperId);
            if (stackHelper.matchesItem(stack)) {
                PlacementOffset offset = stackHelper.getOffset(player, level, state, pos, hitResult);
                if (offset.isSuccessful()) {
                    offset.placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            IPlacementHelper sideHelper = PlacementHelpers.get(sidePlacementHelperId);
            if (sideHelper.matchesItem(stack)) {
                PlacementOffset offset = sideHelper.getOffset(player, level, state, pos, hitResult);
                if (offset.isSuccessful()) {
                    offset.placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /**
     * Recolors this sail, or spreads the new color through the connected cluster of
     * same-axis symmetric cross sails, mirroring SymmetricSailBlock's algorithm:
     * 1. Try recoloring this exact block.
     * 2. Otherwise, find the nearest same-axis neighbor (by click position) with a
     *    different color and recolor just that one.
     * 3. Otherwise, flood-fill through all connected same-axis sails and recolor them all.
     **/
    public void applyDye(BlockState state, Level world, BlockPos pos, Vec3 hit, DyeColor color) {
        BlockState newState = JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get().defaultBlockState();
        newState = BlockHelper.copyProperties(state, newState);

        if (state != newState) {
            world.setBlockAndUpdate(pos, newState);
            return;
        }

        for (Direction d : IPlacementHelper.orderedByDistanceExceptAxis(pos, hit, state.getValue(AXIS))) {
            BlockPos offset = pos.relative(d);
            BlockState adjacentState = world.getBlockState(offset);
            Block block = adjacentState.getBlock();
            if (block instanceof SymmetricCrossSailBlock
                    && state.getValue(AXIS) == adjacentState.getValue(AXIS)
                    && state != adjacentState) {
                world.setBlockAndUpdate(offset, newState);
                return;
            }
        }

        List<BlockPos> frontier = new ArrayList<>();
        frontier.add(pos);
        Set<BlockPos> visited = new HashSet<>();
        int timeout = 100;

        while (!frontier.isEmpty() && timeout-- >= 0) {
            BlockPos currentPos = frontier.removeFirst();
            visited.add(currentPos);

            for (Direction d : Iterate.directions) {
                if (d.getAxis() != state.getValue(AXIS)) {
                    BlockPos offset = currentPos.relative(d);
                    if (!visited.contains(offset)) {
                        BlockState adjacentState = world.getBlockState(offset);
                        Block block = adjacentState.getBlock();
                        if (block instanceof SymmetricCrossSailBlock
                                && adjacentState.getValue(AXIS) == state.getValue(AXIS)) {
                            if (state != adjacentState) {
                                world.setBlockAndUpdate(offset, newState);
                            }
                            frontier.add(offset);
                            visited.add(offset);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return Direction.get(AxisDirection.POSITIVE, state.getValue(AXIS));
    }

    @Override
    public float sable$getLiftScalar() { return 0.0F; }

    @Override
    public float sable$getParallelDragScalar() { return 0.0F; }

    @Override
    public float sable$getDirectionlessDragScalar() { return 0.0F; }

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


    //THIS SHIT WAS SO PAINFUL TO DO! please kill me asap
    //and NEVER ask me to fix or change ts
    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SymmetricCrossSailBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return state -> state.getBlock() instanceof SymmetricCrossSailBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
            Direction.Axis axis = state.getValue(AXIS);

            Direction posDir = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            Direction negDir = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);

            Vec3 hit = ray.getLocation().subtract(Vec3.atLowerCornerOf(pos));
            double hitOffset = axis.choose(hit.x, hit.y, hit.z);

            Direction primaryDir = hitOffset >= 0.5 ? posDir : negDir;
            Direction secondaryDir = primaryDir.getOpposite();

            BlockPos primaryPos = pos.relative(primaryDir);
            if (world.getBlockState(primaryPos).canBeReplaced()) {
                return PlacementOffset.success(primaryPos, s -> s.setValue(AXIS, axis));
            }

            BlockPos secondaryPos = pos.relative(secondaryDir);
            if (world.getBlockState(secondaryPos).canBeReplaced()) {
                return PlacementOffset.success(secondaryPos, s -> s.setValue(AXIS, axis));
            }

            return PlacementOffset.fail();
        }
    }

    @MethodsReturnNonnullByDefault
    private static class SidePlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return stack -> {
                if (stack.getItem() instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();

                    if (block instanceof SymmetricCrossSailBlock) {
                        return false;
                    }

                    String registryName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                    return block instanceof com.simibubi.create.content.contraptions.bearing.SailBlock ||
                            block.getClass().getSimpleName().toLowerCase().contains("sail") ||
                            registryName.contains("sail");
                }
                return false;
            };
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return state -> state.getBlock() instanceof SymmetricCrossSailBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
            Direction.Axis crossAxis = state.getValue(AXIS);

            List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(
                    pos, ray.getLocation(), crossAxis,
                    dir -> world.getBlockState(pos.relative(dir)).canBeReplaced()
            );

            if (directions.isEmpty()) {
                return PlacementOffset.fail();
            }

            final Direction placementDir = directions.get(0);
            BlockPos targetPos = pos.relative(placementDir);
            Direction.Axis placementAxis = placementDir.getAxis();

            final Direction.Axis thirdAxis =
                    (crossAxis != Direction.Axis.X && placementAxis != Direction.Axis.X) ? Direction.Axis.X :
                            (crossAxis != Direction.Axis.Y && placementAxis != Direction.Axis.Y) ? Direction.Axis.Y :
                            Direction.Axis.Z;

            final Direction thirdAxisDir = Direction.fromAxisAndDirection(thirdAxis, Direction.AxisDirection.POSITIVE);

            return PlacementOffset.success(targetPos, placedState -> {
                Block placedBlock = placedState.getBlock();
                String registryName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(placedBlock).getPath();
                boolean isSymmetric = registryName.contains("symmetric") ||
                        placedBlock.getClass().getSimpleName().toLowerCase().contains("symmetric");

                Direction targetFacing = isSymmetric ? placementDir : thirdAxisDir;
                Direction.Axis targetAxis = isSymmetric ? thirdAxis : placementAxis;

                for (Property<?> prop : placedState.getProperties()) {
                    if (prop instanceof DirectionProperty dirProp) {
                        if (dirProp.getPossibleValues().contains(targetFacing)) {
                            @SuppressWarnings("unchecked")
                            DirectionProperty safeProp = (DirectionProperty) prop;
                            placedState = placedState.setValue(safeProp, targetFacing);
                        }
                    }
                }

                for (Property<?> prop : placedState.getProperties()) {
                    if (prop instanceof EnumProperty<?> enumProp && enumProp.getValueClass() == Direction.Axis.class) {
                        @SuppressWarnings("unchecked")
                        Property<Direction.Axis> axisProp = (Property<Direction.Axis>) prop;
                        if (axisProp.getPossibleValues().contains(targetAxis)) {
                            placedState = placedState.setValue(axisProp, targetAxis);
                        }
                    }
                }

                return placedState;
            });
        }
    }
}