package net.multyfora.client.portable_throttle;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.multyfora.client.FreqScreenMenu;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleConfigPacket;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class PortableThrottleScreen extends AbstractContainerScreen<FreqScreenMenu> {

    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 8;
    private static final int INV_SLOT = 18;
    private static final int INV_GAP = 2;

    private static final int CONTENT_W = 160;
    private static final int CONTENT_H = 150;

    private ItemStack firstItem = ItemStack.EMPTY;
    private ItemStack secondItem = ItemStack.EMPTY;
    private ItemStack cursorItem = ItemStack.EMPTY;

    public PortableThrottleScreen(FreqScreenMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = CONTENT_W;
        this.imageHeight = CONTENT_H;
    }

    @Override
    protected void init() {
        this.imageWidth = CONTENT_W;
        this.imageHeight = CONTENT_H;
        super.init();
        this.leftPos = (width - CONTENT_W) / 2;
        this.topPos = freqSlotsY() - 10;

        Minecraft mc = Minecraft.getInstance();
        ItemStack held = getHeldItem();
        if (held != null && mc.level != null) {
            Couple<Frequency> freq = PortableThrottleItem.getFrequency(held, mc.level.registryAccess());
            if (freq != null) {
                firstItem = freq.getFirst().getStack();
                secondItem = freq.getSecond().getStack();
            }
        }
    }

    private int freqSlotsCenterX() {
        return width / 2;
    }

    private int freqSlotsY() {
        return height / 2 - 40;
    }

    private int invTopY() {
        return freqSlotsY() + SLOT_SIZE + 35;
    }

    private int invLeft() {
        return width / 2 - 9 * INV_SLOT / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!cursorItem.isEmpty()) {
            graphics.renderItem(cursorItem, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, cursorItem, mouseX - 8, mouseY - 8);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.drawCenteredString(font,
                Component.translatable("item.joc.portable_throttle"),
                width / 2, height / 2 - 80, 0xFFFFFF);

        int cy = freqSlotsY();
        int cx = freqSlotsCenterX();
        int totalW = SLOT_SIZE * 2 + SLOT_GAP;
        int slotX1 = cx - totalW / 2;
        int slotX2 = slotX1 + SLOT_SIZE + SLOT_GAP;

        renderSlot(graphics, slotX1, cy, firstItem, mouseX, mouseY);
        renderSlot(graphics, slotX2, cy, secondItem, mouseX, mouseY);

        renderInventory(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void renderInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Inventory inv = mc.player.getInventory();
        int startX = invLeft();
        int startY = invTopY();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + row * 9 + col;
                int x = startX + col * INV_SLOT;
                int y = startY + row * INV_SLOT;
                boolean hovered = mouseX >= x && mouseX < x + INV_SLOT && mouseY >= y && mouseY < y + INV_SLOT;
                int bg = hovered ? 0xFF555555 : 0xFF333333;
                graphics.fill(x, y, x + INV_SLOT, y + INV_SLOT, 0xFF000000);
                graphics.fill(x + 1, y + 1, x + INV_SLOT - 1, y + INV_SLOT - 1, bg);
                ItemStack stack = inv.getItem(slotIdx);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x + 1, y + 1);
                    graphics.renderItemDecorations(font, stack, x + 1, y + 1);
                }
            }
        }

        int hotbarY = startY + 3 * INV_SLOT + INV_GAP;
        for (int col = 0; col < 9; col++) {
            int x = startX + col * INV_SLOT;
            boolean hovered = mouseX >= x && mouseX < x + INV_SLOT && mouseY >= hotbarY && mouseY < hotbarY + INV_SLOT;
            int bg = hovered ? 0xFF555555 : 0xFF333333;
            graphics.fill(x, hotbarY, x + INV_SLOT, hotbarY + INV_SLOT, 0xFF000000);
            graphics.fill(x + 1, hotbarY + 1, x + INV_SLOT - 1, hotbarY + INV_SLOT - 1, bg);
            ItemStack stack = inv.getItem(col);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + 1, hotbarY + 1);
                graphics.renderItemDecorations(font, stack, x + 1, hotbarY + 1);
            }
        }
    }

    private void renderSlot(GuiGraphics graphics, int x, int y, ItemStack stack, int mx, int my) {
        boolean hovered = mx >= x && mx <= x + SLOT_SIZE && my >= y && my <= y + SLOT_SIZE;
        int bg = hovered ? 0xFF555555 : 0xFF333333;
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, bg);

        if (!stack.isEmpty()) {
            int ix = x + (SLOT_SIZE - 16) / 2;
            int iy = y + (SLOT_SIZE - 16) / 2;
            graphics.renderItem(stack, ix, iy);
            graphics.renderItemDecorations(font, stack, ix, iy);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int cx = freqSlotsCenterX();
        int slotY = freqSlotsY();
        int totalW = SLOT_SIZE * 2 + SLOT_GAP;
        int slotX1 = cx - totalW / 2;
        int slotX2 = slotX1 + SLOT_SIZE + SLOT_GAP;

        if (isInSlot(mouseX, mouseY, slotX1, slotY, SLOT_SIZE)) {
            handleFreqSlotClick(0);
            return true;
        }
        if (isInSlot(mouseX, mouseY, slotX2, slotY, SLOT_SIZE)) {
            handleFreqSlotClick(1);
            return true;
        }

        if (handleInvClick(mouseX, mouseY)) return true;

        if (!cursorItem.isEmpty()) {
            cursorItem = ItemStack.EMPTY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleInvClick(double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        Inventory inv = mc.player.getInventory();
        int startX = invLeft();
        int startY = invTopY();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + row * 9 + col;
                int x = startX + col * INV_SLOT;
                int y = startY + row * INV_SLOT;
                if (mx >= x && mx < x + INV_SLOT && my >= y && my < y + INV_SLOT) {
                    ItemStack stack = inv.getItem(slotIdx);
                    if (!stack.isEmpty()) {
                        cursorItem = stack.copyWithCount(1);
                    }
                    return true;
                }
            }
        }

        int hotbarY = startY + 3 * INV_SLOT + INV_GAP;
        for (int col = 0; col < 9; col++) {
            int x = startX + col * INV_SLOT;
            if (mx >= x && mx < x + INV_SLOT && my >= hotbarY && my < hotbarY + INV_SLOT) {
                ItemStack stack = inv.getItem(col);
                if (!stack.isEmpty()) {
                    cursorItem = stack.copyWithCount(1);
                }
                return true;
            }
        }

        return false;
    }

    private void handleFreqSlotClick(int slotIndex) {
        if (!cursorItem.isEmpty()) {
            if (slotIndex == 0) {
                firstItem = cursorItem.copy();
            } else {
                secondItem = cursorItem.copy();
            }
            cursorItem = ItemStack.EMPTY;
            save();
        } else {
            if (slotIndex == 0) {
                firstItem = ItemStack.EMPTY;
            } else {
                secondItem = ItemStack.EMPTY;
            }
            save();
        }
    }

    private boolean isInSlot(double mx, double my, int sx, int sy, int size) {
        return mx >= sx && mx <= sx + size && my >= sy && my <= sy + size;
    }

    private void save() {
        if (firstItem.isEmpty() || secondItem.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        HolderLookup.Provider registries = level.registryAccess();
        ItemStack held = getHeldItem();
        if (held == null) return;
        Couple<Frequency> freq = Couple.create(Frequency.of(firstItem), Frequency.of(secondItem));
        PortableThrottleItem.setFrequency(held, freq, registries);
        PacketDistributor.sendToServer(new PortableThrottleConfigPacket(
                (net.minecraft.nbt.CompoundTag)firstItem.save(registries, new net.minecraft.nbt.CompoundTag()),
                (net.minecraft.nbt.CompoundTag)secondItem.save(registries, new net.minecraft.nbt.CompoundTag())));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public @org.jetbrains.annotations.Nullable Rect2i getFreqSlotArea(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 1) return null;
        int cx = freqSlotsCenterX();
        int totalW = SLOT_SIZE * 2 + SLOT_GAP;
        int x = cx - totalW / 2 + slotIndex * (SLOT_SIZE + SLOT_GAP);
        return new Rect2i(x, freqSlotsY(), SLOT_SIZE, SLOT_SIZE);
    }

    public void acceptFreqSlotIngredient(int slotIndex, ItemStack stack) {
        if (slotIndex < 0 || slotIndex > 1) return;
        if (slotIndex == 0) {
            firstItem = stack.copy();
        } else {
            secondItem = stack.copy();
        }
        save();
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(leftPos, topPos, imageWidth, imageHeight));
        return areas;
    }

    private static ItemStack getHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof PortableThrottleItem) return held;
        held = mc.player.getOffhandItem();
        return held.getItem() instanceof PortableThrottleItem ? held : null;
    }
}
