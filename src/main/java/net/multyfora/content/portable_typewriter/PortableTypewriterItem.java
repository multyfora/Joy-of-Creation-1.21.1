package net.multyfora.content.portable_typewriter;

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

import net.multyfora.client.portable_typewriter.PortableTypewriterClientHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class PortableTypewriterItem extends Item {

    private static final String TAG_KEY_BINDINGS = "key_bindings";
    private static final String TAG_SELECTED_KEY = "selected_key";

    public PortableTypewriterItem(Properties properties) {
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
                PortableTypewriterClientHandler.toggleBindMode(pos);
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

        if (player.isShiftKeyDown()) {
            if (world.isClientSide) {
                PortableTypewriterClientHandler.openScreen();
            }
            return InteractionResultHolder.success(heldItem);
        }

        if (world.isClientSide) {
            PortableTypewriterClientHandler.toggle();
        }
        player.getCooldowns().addCooldown(this, 2);
        return InteractionResultHolder.pass(heldItem);
    }

    private static CompoundTag getOrCreateBindingsTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        if (!tag.contains(TAG_KEY_BINDINGS))
            tag.put(TAG_KEY_BINDINGS, new CompoundTag());
        return tag;
    }

    private static void saveTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getSelectedKey(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return -1;
        CompoundTag tag = data.copyTag();
        if (tag.contains(TAG_SELECTED_KEY))
            return tag.getInt(TAG_SELECTED_KEY);
        return -1;
    }

    public static void setSelectedKey(ItemStack stack, int glfwKeyCode) {
        CompoundTag tag = getOrCreateBindingsTag(stack);
        tag.putInt(TAG_SELECTED_KEY, glfwKeyCode);
        saveTag(stack, tag);
    }

    public static Couple<Frequency> getKeyBinding(ItemStack stack, int glfwKeyCode, HolderLookup.Provider registries) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_KEY_BINDINGS)) return null;
        CompoundTag bindings = tag.getCompound(TAG_KEY_BINDINGS);
        String key = String.valueOf(glfwKeyCode);
        if (!bindings.contains(key)) return null;
        CompoundTag entry = bindings.getCompound(key);
        ItemStack first = ItemStack.parseOptional(registries, entry.getCompound("first"));
        ItemStack second = ItemStack.parseOptional(registries, entry.getCompound("second"));
        if (first.isEmpty() && second.isEmpty()) return null;
        return Couple.create(Frequency.of(first), Frequency.of(second));
    }

    public static void setKeyBinding(ItemStack stack, int glfwKeyCode, Couple<Frequency> frequencies, HolderLookup.Provider registries) {
        CompoundTag tag = getOrCreateBindingsTag(stack);
        CompoundTag bindings = tag.getCompound(TAG_KEY_BINDINGS);
        CompoundTag entry = new CompoundTag();
        ItemStack first = frequencies.getFirst().getStack();
        ItemStack second = frequencies.getSecond().getStack();
        if (first != null && !first.isEmpty()) {
            entry.put("first", first.save(registries, new CompoundTag()));
        }
        if (second != null && !second.isEmpty()) {
            entry.put("second", second.save(registries, new CompoundTag()));
        }
        bindings.put(String.valueOf(glfwKeyCode), entry);
        tag.put(TAG_KEY_BINDINGS, bindings);
        saveTag(stack, tag);
    }

    public static void clearKeyBinding(ItemStack stack, int glfwKeyCode) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_KEY_BINDINGS)) return;
        tag.getCompound(TAG_KEY_BINDINGS).remove(String.valueOf(glfwKeyCode));
        saveTag(stack, tag);
    }

    public static boolean hasKeyBinding(ItemStack stack, int glfwKeyCode) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_KEY_BINDINGS)) return false;
        return tag.getCompound(TAG_KEY_BINDINGS).contains(String.valueOf(glfwKeyCode));
    }

    public static Collection<Integer> getAllBoundKeys(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Collections.emptyList();
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_KEY_BINDINGS)) return Collections.emptyList();
        CompoundTag bindings = tag.getCompound(TAG_KEY_BINDINGS);
        Collection<Integer> keys = new ArrayList<>();
        for (String k : bindings.getAllKeys()) {
            try { keys.add(Integer.parseInt(k)); } catch (NumberFormatException ignored) {}
        }
        return keys;
    }
}
