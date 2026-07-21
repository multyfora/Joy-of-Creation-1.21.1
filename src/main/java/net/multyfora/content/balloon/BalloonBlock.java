package net.multyfora.content.balloon;

import com.simibubi.create.foundation.block.IBE;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;

import net.minecraft.core.BlockPos;
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

import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocBlocks;

public class BalloonBlock extends Block implements IBE<BalloonBlockEntity>, BlockSubLevelAssemblyListener {

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
    public Class<BalloonBlockEntity> getBlockEntityClass() {
        return BalloonBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BalloonBlockEntity> getBlockEntityType() {
        return JocBlockEntityTypes.BALLOON.get();
    }
}