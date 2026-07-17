package net.multyfora.client.portable_throttle;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.multyfora.client.FreqScreenMenu;
import net.multyfora.client.graphics.GraphicsUtils;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleConfigPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

import static net.multyfora.client.graphics.GraphicsUtils.isInBounds;

public class PortableThrottleLinkScreen extends AbstractContainerScreen<FreqScreenMenu> {

    private static final int
        FREQ_SLOT_SIZE = 22,
        FREQ_SLOT_GAP = 8,
        INV_SLOT_SIZE = 24,
        INV_LINE_THICKNESS = 2,
        INV_SLOT_GAP = 2
    ;

    private static final int CONTENT_WIDTH = 160;
    private static final int CONTENT_HEIGHT = 150;

    private ItemStack firstItem = ItemStack.EMPTY;
    private ItemStack secondItem = ItemStack.EMPTY;
    private ItemStack cursorItem = ItemStack.EMPTY;

    public PortableThrottleLinkScreen(FreqScreenMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = CONTENT_WIDTH;
        this.imageHeight = CONTENT_HEIGHT;
    }

    @Override
    protected void init() {
        this.imageWidth = CONTENT_WIDTH;
        this.imageHeight = CONTENT_HEIGHT;
        super.init();
        this.leftPos = (width - CONTENT_WIDTH) / 2;
        this.topPos = getFreqSlotsY() - 10;

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

    private int getFreqSlotsCenterX() {
        return width/2;
    }

    private int getFreqSlotsY() {
        return height/2 - 40;
    }

    private int getInvTopY() {
        return getFreqSlotsY() + FREQ_SLOT_SIZE + 15;
    }

    private int getInvLeft() {
        return width/2 - 9*INV_SLOT_SIZE/2 + 2*INV_LINE_THICKNESS;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!cursorItem.isEmpty()) {
            graphics.renderItem(cursorItem, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, cursorItem, mouseX - 8, mouseY - 8);
        }

        ItemStack hovered = getHoveredInventoryStack(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.drawCenteredString(
            font,
            Component.translatable("item.joc.portable_throttle"),
            width / 2, height / 2 - 80, 0xFFFFFF
        );

        Vector2i mousePosition = new Vector2i(mouseX, mouseY);

        Vector2i center = new Vector2i(
            getFreqSlotsCenterX(),
            getFreqSlotsY()
        );
        GraphicsUtils.renderFrequencySlots(
            graphics, font,
            center, FREQ_SLOT_SIZE, FREQ_SLOT_GAP,
            mousePosition,
            firstItem, secondItem,
            false, true
        );

        Vector2i origin = new Vector2i(
            getInvLeft(),
            getInvTopY()
        );
        graphics.drawString(font, Component.translatable("joc.gui.inventory"), origin.x, getInvTopY() - 12, 0x888888);
        GraphicsUtils.renderInventory(
            graphics, font,
            origin,
            INV_SLOT_SIZE, INV_SLOT_GAP, INV_LINE_THICKNESS,
            mousePosition
        );
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);


        Vector2i center = new Vector2i(
            getFreqSlotsCenterX(),
            getFreqSlotsY()
        );
        int center_delta = (FREQ_SLOT_SIZE + FREQ_SLOT_GAP)/2;
        Vector2i adjustedCenter = new Vector2i(center.x, center.y);

        Vector2i firstStart = new Vector2i(
            adjustedCenter.x - center_delta - FREQ_SLOT_SIZE/2,
            adjustedCenter.y - FREQ_SLOT_SIZE/2
        );
        Vector2i secondStart = new Vector2i(
            adjustedCenter.x + center_delta - FREQ_SLOT_SIZE/2,
            adjustedCenter.y - FREQ_SLOT_SIZE/2
        );

        final int OUTLINE_THICKNESS = 1;

        firstStart.add(OUTLINE_THICKNESS, OUTLINE_THICKNESS);
        Vector2i firstEnd = new Vector2i(
            firstStart.x + FREQ_SLOT_SIZE,
            firstStart.y + FREQ_SLOT_SIZE
        );
        firstEnd.sub(OUTLINE_THICKNESS, OUTLINE_THICKNESS);

        secondStart.add(OUTLINE_THICKNESS, OUTLINE_THICKNESS);
        Vector2i SecondEnd = new Vector2i(
            secondStart.x + FREQ_SLOT_SIZE,
            secondStart.y + FREQ_SLOT_SIZE
        );
        SecondEnd.sub(OUTLINE_THICKNESS, OUTLINE_THICKNESS);

        Vector2i mousePosition = new Vector2i( (int)mouseX, (int)mouseY );
        if ( isInBounds(firstStart, firstEnd, mousePosition) ) {
            handleFreqSlotClick(0);
            return true;
        }
        if ( isInBounds(secondStart, SecondEnd, mousePosition) ) {
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
        //TODO: this does not perfectly line-up with GraphicsUtils::renderInventory
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        Inventory inv = mc.player.getInventory();
        int startX = getInvLeft();
        int startY = getInvTopY();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + row * 9 + col;
                int x = startX + col * (INV_SLOT_SIZE-INV_LINE_THICKNESS);
                int y = startY + row * (INV_SLOT_SIZE-INV_LINE_THICKNESS);
                if (mx >= x && mx < x + INV_SLOT_SIZE && my >= y && my < y + INV_SLOT_SIZE) {
                    ItemStack stack = inv.getItem(slotIdx);
                    if (!stack.isEmpty()) {
                        cursorItem = stack.copyWithCount(1);
                    }
                    return true;
                }
            }
        }

        int hotbarY = startY + 3 * INV_SLOT_SIZE + INV_SLOT_GAP;
        for (int col = 0; col < 9; col++) {
            int x = startX + col * (INV_SLOT_SIZE-INV_LINE_THICKNESS);
            if (mx >= x && mx < x + INV_SLOT_SIZE && my >= hotbarY && my < hotbarY + INV_SLOT_SIZE) {
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
        } else {
            if (slotIndex == 0) {
                firstItem = ItemStack.EMPTY;
            } else {
                secondItem = ItemStack.EMPTY;
            }
        }

        save();
    }

    private void save() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if(level == null) {
            return;
        }

        HolderLookup.Provider registries = level.registryAccess();
        ItemStack held = getHeldItem();
        if(held == null) {
            return;
        }

        Couple<Frequency> freq = Couple.create(
            Frequency.of(firstItem),
            Frequency.of(secondItem)
        );
        PortableThrottleItem.setFrequency(held, freq, registries);

        CompoundTag firstTag = new CompoundTag();
        if( !firstItem.isEmpty() ) {
            firstTag = (CompoundTag) firstItem.save( registries, new CompoundTag() );
        }
        CompoundTag secondTag = new CompoundTag();
        if( !secondItem.isEmpty() ) {
            secondTag = (CompoundTag)secondItem.save( registries, new CompoundTag() );
        }
        PacketDistributor.sendToServer(
            new PortableThrottleConfigPacket(firstTag, secondTag)
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public @Nullable Rect2i getFreqSlotArea(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 1) return null;
        int cx = getFreqSlotsCenterX();
        int totalW = FREQ_SLOT_SIZE * 2 + FREQ_SLOT_GAP;
        int x = cx - totalW / 2 + slotIndex * (FREQ_SLOT_SIZE + FREQ_SLOT_GAP);
        return new Rect2i(x, getFreqSlotsY() - FREQ_SLOT_SIZE / 2, FREQ_SLOT_SIZE, FREQ_SLOT_SIZE);
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

    private ItemStack getHoveredInventoryStack(double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        Inventory inv = mc.player.getInventory();
        int startX = getInvLeft();
        int startY = getInvTopY();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + row * 9 + col;
                int x = startX + col * (INV_SLOT_SIZE - INV_LINE_THICKNESS);
                int y = startY + row * (INV_SLOT_SIZE - INV_LINE_THICKNESS);
                if (mx >= x && mx < x + INV_SLOT_SIZE && my >= y && my < y + INV_SLOT_SIZE) {
                    ItemStack stack = inv.getItem(slotIdx);
                    return stack.isEmpty() ? null : stack;
                }
            }
        }

        int hotbarY = startY + 3 * INV_SLOT_SIZE + INV_SLOT_GAP;
        for (int col = 0; col < 9; col++) {
            int x = startX + col * (INV_SLOT_SIZE - INV_LINE_THICKNESS);
            if (mx >= x && mx < x + INV_SLOT_SIZE && my >= hotbarY && my < hotbarY + INV_SLOT_SIZE) {
                ItemStack stack = inv.getItem(col);
                return stack.isEmpty() ? null : stack;
            }
        }

        return null;
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
