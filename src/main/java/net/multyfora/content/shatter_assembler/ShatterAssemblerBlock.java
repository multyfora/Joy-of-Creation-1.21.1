package net.multyfora.content.shatter_assembler;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.simulated_team.simulated.index.SimBlockShapes;
import net.multyfora.index.JocBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ShatterAssemblerBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<ShatterAssemblerBlockEntity>, IWrenchable, BlockSubLevelAssemblyListener {

    public static final MapCodec<ShatterAssemblerBlock> CODEC = simpleCodec(ShatterAssemblerBlock::new);

    public ShatterAssemblerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canAttach(level, pos, getConnectedDirection(state).getOpposite());
    }

    public static boolean canAttach(LevelReader reader, BlockPos pos, Direction direction) {
        BlockPos blockpos = pos.relative(direction);
        return !reader.getBlockState(blockpos).getBlockSupportShape(reader, pos).getFaceShape(direction.getOpposite()).isEmpty();
    }

    @Override
    public Class<ShatterAssemblerBlockEntity> getBlockEntityClass() {
        return ShatterAssemblerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShatterAssemblerBlockEntity> getBlockEntityType() {
        return JocBlockEntityTypes.SHATTER_ASSEMBLER.get();
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case CEILING -> SimBlockShapes.PHYSICS_ASSEMBLER_CEILING_OUTLINE.get(facing);
            case FLOOR -> SimBlockShapes.PHYSICS_ASSEMBLER_OUTLINE.get(facing);
            case WALL -> SimBlockShapes.PHYSICS_ASSEMBLER_WALL_OUTLINE.get(facing.getOpposite());
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case CEILING -> SimBlockShapes.PHYSICS_ASSEMBLER_CEILING_COLLISION.get(facing);
            case FLOOR -> SimBlockShapes.PHYSICS_ASSEMBLER_COLLISION.get(facing);
            case WALL -> SimBlockShapes.PHYSICS_ASSEMBLER_WALL_COLLISION.get(facing.getOpposite());
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide && player.isLocalPlayer()) {
            net.multyfora.index.JocClickInteractions.SHATTER_ASSEMBLER_MANAGER.startHold(level, player, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    public static Direction getStickyFacing(BlockState state) {
        return switch (state.getValue(FACE)) {
            case CEILING -> Direction.UP;
            case FLOOR -> Direction.DOWN;
            case WALL -> state.getValue(FACING).getOpposite();
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, FACE));
    }

    @Override
    public void afterMove(ServerLevel serverLevel, ServerLevel serverLevel1, BlockState blockState, BlockPos blockPos, BlockPos blockPos1) {
    }
}
