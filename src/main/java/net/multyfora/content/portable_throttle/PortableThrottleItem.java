package net.multyfora.content.portable_throttle;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.util.Mth;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.index.JocDataComponents;

public class PortableThrottleItem extends Item {

    private static final String TAG_FREQ_FIRST = "frequency_first";
    private static final String TAG_FREQ_SECOND = "frequency_second";

    public PortableThrottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState hitState = world.getBlockState(pos);

        if (player.mayBuild() && AllBlocks.REDSTONE_LINK.has(hitState)) {
            if (world.isClientSide) {
                PortableThrottleClientHandler.startBind(pos);
            }
            player.getCooldowns().addCooldown(this, 2);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.pass(heldItem);

        if (world.isClientSide) {
            PortableThrottleClientHandler.openScreen();
        }
        return InteractionResultHolder.success(heldItem);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    public static int getStrength(ItemStack stack) {
        return stack.getOrDefault(JocDataComponents.THROTTLE_STRENGTH, 0);
    }

    public static void setStrength(ItemStack stack, int strength) {
        strength = Mth.clamp(strength, 0, 15);
        if (strength == 0) {
            stack.remove(JocDataComponents.THROTTLE_STRENGTH);
        } else {
            stack.set(JocDataComponents.THROTTLE_STRENGTH, strength);
        }
    }

    public static Couple<Frequency> getFrequency(ItemStack stack, HolderLookup.Provider registries) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();

        ItemStack first = ItemStack.EMPTY;
        if( tag.contains(TAG_FREQ_FIRST) ) {
            first = ItemStack.parseOptional( registries, tag.getCompound(TAG_FREQ_FIRST) );
        }
        ItemStack second = ItemStack.EMPTY;
        if( tag.contains(TAG_FREQ_SECOND) ) {
            second = ItemStack.parseOptional( registries, tag.getCompound(TAG_FREQ_SECOND) );
        }

        return Couple.create( Frequency.of(first), Frequency.of(second) );
    }

    public static void setFrequency(ItemStack stack, Couple<Frequency> freq, HolderLookup.Provider registries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        ItemStack  firstItem = freq.getFirst() .getStack();
        ItemStack secondItem = freq.getSecond().getStack();

        if(firstItem != null && !firstItem.isEmpty()) {
            tag.put(
                TAG_FREQ_FIRST,
                firstItem.save( registries, new CompoundTag() )
            );
        } else {
            tag.remove(TAG_FREQ_FIRST);
        }
        if(secondItem != null && !secondItem.isEmpty()) {
            tag.put(
                TAG_FREQ_SECOND,
                secondItem.save( registries, new CompoundTag() )
            );
        } else {
            tag.remove(TAG_FREQ_SECOND);
        }

        stack.set(
            DataComponents.CUSTOM_DATA,
            CustomData.of(tag)
        );
    }

    public static boolean hasFrequency(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        boolean result = tag.contains(TAG_FREQ_FIRST) && tag.contains(TAG_FREQ_SECOND);
        return result;
    }
}
