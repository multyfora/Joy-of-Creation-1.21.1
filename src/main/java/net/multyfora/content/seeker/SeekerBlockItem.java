package net.multyfora.content.seeker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.multyfora.index.JocDataComponents;

public class SeekerBlockItem extends BlockItem {

    public SeekerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        if (!(level.getBlockEntity(pos) instanceof SeekerBlockEntity be)) {
            return InteractionResult.PASS;
        }

        // Sneaking -> normal placement, don't intercept
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        SeekerBlockEntity.ModuleType module = be.getModule();
        if (module != SeekerBlockEntity.ModuleType.SPYGLASS && module != SeekerBlockEntity.ModuleType.MODULATING) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            BlockPos storedPos = stack.get(JocDataComponents.LINKED_SEEKER_POS.get());
            if (storedPos == null) {
                stack.set(JocDataComponents.LINKED_SEEKER_POS.get(), pos);
            } else {
                stack.remove(JocDataComponents.LINKED_SEEKER_POS.get());
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos storedPos = stack.get(JocDataComponents.LINKED_SEEKER_POS.get());

        if (storedPos == null) {
            stack.set(JocDataComponents.LINKED_SEEKER_POS.get(), pos);
            player.displayClientMessage(
                Component.translatable("item.joc.seeker.captured", pos.getX(), pos.getY(), pos.getZ()),
                true
            );
            return InteractionResult.SUCCESS;
        }

        if (storedPos.equals(pos)) {
            stack.remove(JocDataComponents.LINKED_SEEKER_POS.get());
            player.displayClientMessage(
                Component.translatable("item.joc.seeker.link_self"),
                true
            );
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(storedPos) instanceof SeekerBlockEntity other)) {
            stack.remove(JocDataComponents.LINKED_SEEKER_POS.get());
            player.displayClientMessage(
                Component.translatable("item.joc.seeker.link_gone"),
                true
            );
            return InteractionResult.SUCCESS;
        }

        SeekerBlockEntity.ModuleType otherModule = other.getModule();
        if (otherModule != SeekerBlockEntity.ModuleType.SPYGLASS && otherModule != SeekerBlockEntity.ModuleType.MODULATING) {
            stack.remove(JocDataComponents.LINKED_SEEKER_POS.get());
            player.displayClientMessage(
                Component.translatable("item.joc.seeker.link_incompatible"),
                true
            );
            return InteractionResult.SUCCESS;
        }

        be.linkTo(storedPos);
        other.linkTo(pos);

        // Copy target + use3D from the captured seeker to the new one
        if (other.hasTarget()) {
            be.setTarget(other.getTargetX(), other.getTargetY(), other.getTargetZ());
            be.setUse3D(other.isUse3D());
        } else if (be.hasTarget()) {
            other.setTarget(be.getTargetX(), be.getTargetY(), be.getTargetZ());
            other.setUse3D(be.isUse3D());
        }

        stack.remove(JocDataComponents.LINKED_SEEKER_POS.get());
        player.displayClientMessage(
            Component.translatable("item.joc.seeker.linked", storedPos.getX(), storedPos.getY(), storedPos.getZ()),
            true
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        BlockPos storedPos = stack.get(JocDataComponents.LINKED_SEEKER_POS.get());
        if (storedPos != null) {
            if (level.getBlockEntity(storedPos) instanceof SeekerBlockEntity capturedBE) {
                SeekerBlockEntity.ModuleType capturedModule = capturedBE.getModule();
                if (capturedModule == SeekerBlockEntity.ModuleType.SPYGLASS || capturedModule == SeekerBlockEntity.ModuleType.MODULATING) {
                    if (level.getBlockEntity(pos) instanceof SeekerBlockEntity newBE) {
                        newBE.linkTo(storedPos);
                        capturedBE.linkTo(pos);
                        if (capturedBE.hasTarget()) {
                            newBE.setTarget(capturedBE.getTargetX(), capturedBE.getTargetY(), capturedBE.getTargetZ());
                            newBE.setUse3D(capturedBE.isUse3D());
                        }
                    }
                }
            }
            stack.remove(JocDataComponents.LINKED_SEEKER_POS.get());
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }
}