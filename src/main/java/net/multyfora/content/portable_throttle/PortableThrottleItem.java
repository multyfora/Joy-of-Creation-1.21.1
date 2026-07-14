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

import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;

/**
 * Portable Throttle item: a handheld wireless redstone transmitter.
 * Right-clicking a Redstone Link block binds the throttle to that link's frequency.
 * Right-clicking in air opens a config screen to set frequency items.
 * Left-clicking (or the client handler) opens a strength slider (0-15).
 * The throttle sends keepalive signals to the server for persistent output while active.
 **/
public class PortableThrottleItem extends Item {

    // NBT keys for storing the two frequency-defining items
    private static final String TAG_FREQ_FIRST = "frequency_first";
    private static final String TAG_FREQ_SECOND = "frequency_second";

    public PortableThrottleItem(Properties properties) {
        super(properties);
    }

    /**
     * Called when right-clicking on a block with the throttle.
     * If the target is a Redstone Link, initiates a bind operation.
     **/
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState hitState = world.getBlockState(pos);

        if (player.mayBuild() && AllBlocks.REDSTONE_LINK.has(hitState)) {
            if (world.isClientSide) {
                // Start bind on the client side; the actual bind packet is sent during the next client tick
                PortableThrottleClientHandler.startBind(pos);
            }
            player.getCooldowns().addCooldown(this, 2);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Called when right-clicking in air (or on a non-interactable block).
     * Opens the throttle configuration screen on the client.
     **/
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

    /**
     * Reads the frequency pair stored on the item's CUSTOM_DATA component.
     * Returns null if no frequency is set or data is malformed.
     **/
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

    // Writes the frequency pair to the item's CUSTOM_DATA component for persistence
    public static void setFrequency(ItemStack stack, Couple<Frequency> freq, HolderLookup.Provider registries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        ItemStack  firstItem = freq.getFirst() .getStack();
        ItemStack secondItem = freq.getSecond().getStack();

        if(firstItem != null && firstItem != ItemStack.EMPTY) {
            tag.put(
                TAG_FREQ_FIRST,
                firstItem.save( registries, new CompoundTag() )
            );
        }
        if(secondItem != null && secondItem != ItemStack.EMPTY) {
            tag.put(
                TAG_FREQ_SECOND,
                secondItem.save( registries, new CompoundTag() )
            );
        }

        stack.set(
            DataComponents.CUSTOM_DATA,
            CustomData.of(tag)
        );
    }

    // Checks whether the item has a frequency pair stored
    public static boolean hasFrequency(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        boolean result = tag.contains(TAG_FREQ_FIRST) && tag.contains(TAG_FREQ_SECOND);
        return result;
    }
}
