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

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;

import org.slf4j.Logger;

// Portable Throttle item: a handheld wireless redstone transmitter.
// Right-clicking a Redstone Link block binds the throttle to that link's frequency.
// Right-clicking in air opens a config screen to set frequency items.
// Left-clicking (or the client handler) opens a strength slider (0-15).
// The throttle sends keepalive signals to the server for persistent output while active.
public class PortableThrottleItem extends Item {

    private static final Logger LOGGER = AeronauticsJoyofcreation.LOGGER;

    // NBT keys for storing the two frequency-defining items
    private static final String TAG_FREQ_FIRST = "frequency_first";
    private static final String TAG_FREQ_SECOND = "frequency_second";

    public PortableThrottleItem(Properties properties) {
        super(properties);
    }

    // Called when right-clicking on a block with the throttle.
    // If the target is a Redstone Link, initiates a bind operation.
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState hitState = world.getBlockState(pos);

        if (player.mayBuild() && AllBlocks.REDSTONE_LINK.has(hitState)) {
            LOGGER.info("[THROTTLE_ITEM] onItemUseFirst: right-clicked redstone link at {}, side={}", pos, ctx.getClickedFace());
            if (world.isClientSide) {
                // Start bind on the client side; the actual bind packet is sent during the next client tick
                PortableThrottleClientHandler.startBind(pos);
                LOGGER.info("[THROTTLE_ITEM] onItemUseFirst: called startBind on client");
            }
            player.getCooldowns().addCooldown(this, 2);
            return InteractionResult.SUCCESS;
        }

        LOGGER.info("[THROTTLE_ITEM] onItemUseFirst: not a redstone link, PASS (state={})", hitState.getBlock().getDescriptionId());
        return InteractionResult.PASS;
    }

    // Called when right-clicking in air (or on a non-interactable block).
    // Opens the throttle configuration screen on the client.
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.pass(heldItem);

        LOGGER.info("[THROTTLE_ITEM] use: opening config screen, side={}", world.isClientSide() ? "CLIENT" : "SERVER");
        if (world.isClientSide) {
            PortableThrottleClientHandler.openScreen();
        }
        return InteractionResultHolder.success(heldItem);
    }

    // Reads the frequency pair stored on the item's CUSTOM_DATA component.
    // Returns null if no frequency is set or data is malformed.
    public static Couple<Frequency> getFrequency(ItemStack stack, HolderLookup.Provider registries) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            LOGGER.info("[THROTTLE_ITEM] getFrequency: no CUSTOM_DATA on stack");
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_FREQ_FIRST) || !tag.contains(TAG_FREQ_SECOND)) {
            LOGGER.info("[THROTTLE_ITEM] getFrequency: missing frequency tags (first={}, second={})",
                tag.contains(TAG_FREQ_FIRST), tag.contains(TAG_FREQ_SECOND));
            return null;
        }
        ItemStack first = ItemStack.parseOptional(registries, tag.getCompound(TAG_FREQ_FIRST));
        ItemStack second = ItemStack.parseOptional(registries, tag.getCompound(TAG_FREQ_SECOND));
        if (first.isEmpty() || second.isEmpty()) {
            LOGGER.info("[THROTTLE_ITEM] getFrequency: first or second parsed empty");
            return null;
        }
        Couple<Frequency> freq = Couple.create(Frequency.of(first), Frequency.of(second));
        LOGGER.info("[THROTTLE_ITEM] getFrequency: returning freq=({}|{})",
            first.getHoverName().getString(), second.getHoverName().getString());
        return freq;
    }

    // Writes the frequency pair to the item's CUSTOM_DATA component for persistence
    public static void setFrequency(ItemStack stack, Couple<Frequency> freq, HolderLookup.Provider registries) {
        LOGGER.info("[THROTTLE_ITEM] setFrequency: setting freq=({}|{})",
            freq.getFirst().getStack().getHoverName().getString(),
            freq.getSecond().getStack().getHoverName().getString());
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(TAG_FREQ_FIRST, freq.getFirst().getStack().save(registries, new CompoundTag()));
        tag.put(TAG_FREQ_SECOND, freq.getSecond().getStack().save(registries, new CompoundTag()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // Checks whether the item has a frequency pair stored
    public static boolean hasFrequency(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        boolean result = tag.contains(TAG_FREQ_FIRST) && tag.contains(TAG_FREQ_SECOND);
        LOGGER.info("[THROTTLE_ITEM] hasFrequency: {} (first={}, second={})",
            result, tag.contains(TAG_FREQ_FIRST), tag.contains(TAG_FREQ_SECOND));
        return result;
    }
}
