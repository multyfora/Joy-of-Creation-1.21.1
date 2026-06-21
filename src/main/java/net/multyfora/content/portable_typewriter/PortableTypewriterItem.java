package net.multyfora.content.portable_typewriter;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

/**
 * Portable Typewriter item: a handheld device that binds keyboard keys to redstone frequencies.
 * When activated (right-click in air toggles ACTIVE mode), it monitors bound keys and sends
 * press/release events to the server. Shift-right-click opens the keyboard layout GUI.
 * Right-clicking a Redstone Link block while a key is selected binds that key to the link's frequency.
 **/
public class PortableTypewriterItem extends Item {

    // NBT keys for storing key bindings and the currently selected key
    private static final String TAG_KEY_BINDINGS = "key_bindings";
    private static final String TAG_SELECTED_KEY = "selected_key";

    public PortableTypewriterItem(Properties properties) {
        super(properties);
    }

    /**
     * Called when right-clicking on a block with the typewriter.
     * If the target is a Redstone Link and a key is selected, initiates a bind.
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
                // The client handler will send the bind packet on the next tick
                PortableTypewriterClientHandler.toggleBindMode(pos);
            }
            player.getCooldowns().addCooldown(this, 2);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Called when right-clicking in air.
     * With shift: opens the keyboard layout GUI.
     * Without shift: toggles ACTIVE mode (monitoring for key presses).
     **/
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.pass(heldItem);

        if (player.isShiftKeyDown()) {
            // Open the keyboard binding screen
            if (world.isClientSide) {
                PortableTypewriterClientHandler.openScreen();
            }
            return InteractionResultHolder.success(heldItem);
        }

        // Toggle active monitoring mode
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

    // Returns the currently selected (highlighted) key code from the GUI, or -1 if none
    public static int getSelectedKey(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return -1;
        CompoundTag tag = data.copyTag();
        if (tag.contains(TAG_SELECTED_KEY))
            return tag.getInt(TAG_SELECTED_KEY);
        return -1;
    }

    // Persists the selected key code on the item stack
    public static void setSelectedKey(ItemStack stack, int glfwKeyCode) {
        CompoundTag tag = getOrCreateBindingsTag(stack);
        tag.putInt(TAG_SELECTED_KEY, glfwKeyCode);
        saveTag(stack, tag);
    }

    // Reads the frequency pair bound to a given GLFW key code from the item's NBT
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
        if (first.isEmpty() || second.isEmpty()) return null;
        return Couple.create(Frequency.of(first), Frequency.of(second));
    }

    // Binds a GLFW key code to a frequency pair and saves it on the item
    public static void setKeyBinding(ItemStack stack, int glfwKeyCode, Couple<Frequency> frequencies, HolderLookup.Provider registries) {
        CompoundTag tag = getOrCreateBindingsTag(stack);
        CompoundTag bindings = tag.getCompound(TAG_KEY_BINDINGS);
        CompoundTag entry = new CompoundTag();
        entry.put("first", frequencies.getFirst().getStack().save(registries, new CompoundTag()));
        entry.put("second", frequencies.getSecond().getStack().save(registries, new CompoundTag()));
        bindings.put(String.valueOf(glfwKeyCode), entry);
        tag.put(TAG_KEY_BINDINGS, bindings);
        saveTag(stack, tag);
    }

    // Removes the binding for a given key code
    public static void clearKeyBinding(ItemStack stack, int glfwKeyCode) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_KEY_BINDINGS)) return;
        tag.getCompound(TAG_KEY_BINDINGS).remove(String.valueOf(glfwKeyCode));
        saveTag(stack, tag);
    }

    // Returns whether a given key code has a binding on this item
    public static boolean hasKeyBinding(ItemStack stack, int glfwKeyCode) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_KEY_BINDINGS)) return false;
        return tag.getCompound(TAG_KEY_BINDINGS).contains(String.valueOf(glfwKeyCode));
    }

    // Returns all GLFW key codes that have bindings on this item
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
