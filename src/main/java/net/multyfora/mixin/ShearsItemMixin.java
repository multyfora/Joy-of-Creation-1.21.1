package net.multyfora.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.multyfora.content.shears_cut.ShearsCutState;

@Mixin(Item.class)
public class ShearsItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void joc$onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!context.getItemInHand().is(Items.SHEARS)) return;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        SubLevel sub = Sable.HELPER.getContaining(level, pos);
        if (sub == null || sub.isRemoved()) {
            if (level.isClientSide() && ShearsCutState.getMode() == ShearsCutState.Mode.PLACING) {
                ShearsCutState.reset();
            }
            return;
        }

        if (!level.isClientSide()) return;

        switch (ShearsCutState.getMode()) {
            case IDLE -> {
                ShearsCutState.startCut(pos, context.getClickedFace());
                cir.setReturnValue(InteractionResult.SUCCESS);
                cir.cancel();
            }
            case PLACING -> {
                ShearsCutState.finishCut(pos);
                cir.setReturnValue(InteractionResult.SUCCESS);
                cir.cancel();
            }
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void joc$onUse(Level level, Player player, InteractionHand hand,
                           CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.SHEARS)) return;
        if (!level.isClientSide()) return;

        if (ShearsCutState.getMode() == ShearsCutState.Mode.PLACING) {
            BlockPos cursorPos = ShearsCutState.getCursorPos();
            if (cursorPos != null) {
                ShearsCutState.finishCut(cursorPos);
            } else {
                ShearsCutState.reset();
            }
            cir.setReturnValue(InteractionResultHolder.success(stack));
            cir.cancel();
        }
    }
}
