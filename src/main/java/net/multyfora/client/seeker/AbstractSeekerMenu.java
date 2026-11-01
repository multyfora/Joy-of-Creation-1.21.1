package net.multyfora.client.seeker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.content.seeker.SeekerLinkBehaviour;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractSeekerMenu extends AbstractContainerMenu {
    static final int MAIN_WIDTH = 227;
    static final int MAIN_HEIGHT = 162;
    static final int DRAWER_WIDTH = 124;
    static final int DRAWER_X = MAIN_WIDTH + 2;
    static final int INVENTORY_X = (MAIN_WIDTH - 176) / 2;
    static final int INVENTORY_Y = MAIN_HEIGHT + 4;
    static final int INVENTORY_HEIGHT = 99;
    static final int OPEN_HEIGHT = INVENTORY_Y + INVENTORY_HEIGHT;

    static final int CORE_SLOT_X = DRAWER_X + 97;
    static final int CORE_SLOT_Y = 109;

    static final Direction[] LINK_SIDES = {
            Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST,
            Direction.UP, Direction.DOWN
    };
    static final int LINK_FIRST_ROW_Y = 20;
    static final int LINK_ROW_SPACING = 27;
    static final int LINK_COLUMN_SPACING = 56;
    static final int LINK_PADDING = 10;
    static final int LINK_SLOTS_X = 11;

    private static final int SLOT_SIZE = 18;
    private static final int INVENTORY_SLOTS_X = INVENTORY_X + 8;
    private static final int INVENTORY_SLOTS_Y = INVENTORY_Y + 18;
    private static final int HOTBAR_Y = INVENTORY_SLOTS_Y + 58;

    private static final int CORE_SLOT = 36;
    private static final int FIRST_FREQUENCY_SLOT = CORE_SLOT + 1;

    public final SeekerBlockEntity blockEntity;
    private final Level level;
    private boolean advancedOpen;

    protected AbstractSeekerMenu(MenuType<?> type, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(type, containerId);
        this.blockEntity = (SeekerBlockEntity) blockEntity;
        this.level = inventory.player.level();

        addPlayerSlots(inventory);

        addSlot(new SlotItemHandler(this.blockEntity.getLinks().getCore(), 0, CORE_SLOT_X, CORE_SLOT_Y) {
            @Override
            public boolean isActive() {
                return advancedOpen;
            }
        });

        ItemStackHandler frequencies = this.blockEntity.getLinks().getFrequencies();
        for (int i = 0; i < LINK_SIDES.length; i++) {
            for (int index = 0; index < SeekerLinkBehaviour.FREQUENCIES_PER_SIDE; index++) {
                addSlot(new FrequencySlot(
                        frequencies, SeekerLinkBehaviour.frequencySlot(LINK_SIDES[i], index),
                        linkCellX(i) + LINK_SLOTS_X + 1 + index * SLOT_SIZE, linkCellY(i) + 1
                ));
            }
        }
    }

    protected static BlockEntity resolveBlockEntity(Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = inventory.player.level();
        return level.getBlockEntity(pos);
    }

    static int linkCellX(int cell) {
        return DRAWER_X + LINK_PADDING + cell % 2 * LINK_COLUMN_SPACING;
    }

    static int linkCellY(int cell) {
        return INVENTORY_Y + LINK_FIRST_ROW_Y + cell / 2 * LINK_ROW_SPACING;
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new DrawerSlot(
                        inventory, column + row * 9 + 9,
                        INVENTORY_SLOTS_X + column * SLOT_SIZE, INVENTORY_SLOTS_Y + row * SLOT_SIZE
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new DrawerSlot(inventory, column, INVENTORY_SLOTS_X + column * SLOT_SIZE, HOTBAR_Y));
        }
    }

    void setAdvancedOpen(boolean advancedOpen) {
        this.advancedOpen = advancedOpen;
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId < FIRST_FREQUENCY_SLOT) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (clickType == ClickType.PICKUP) {
            getSlot(slotId).set(getCarried().copyWithCount(1));
        } else if (clickType == ClickType.QUICK_MOVE) {
            getSlot(slotId).set(ItemStack.EMPTY);
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = getSlot(index);
        if (index >= FIRST_FREQUENCY_SLOT || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean success = index == CORE_SLOT
                ? moveItemStackTo(stack, 0, CORE_SLOT, true)
                : moveItemStackTo(stack, CORE_SLOT, FIRST_FREQUENCY_SLOT, false);
        if (!success) return ItemStack.EMPTY;

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !blockEntity.isRemoved() && blockEntity.hasModule();
    }

    public Level getLevel() {
        return level;
    }

    private class DrawerSlot extends Slot {
        DrawerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return advancedOpen;
        }
    }

    private class FrequencySlot extends SlotItemHandler {
        FrequencySlot(ItemStackHandler handler, int slot, int x, int y) {
            super(handler, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return advancedOpen && blockEntity.hasLogicCore();
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }
    }
}
