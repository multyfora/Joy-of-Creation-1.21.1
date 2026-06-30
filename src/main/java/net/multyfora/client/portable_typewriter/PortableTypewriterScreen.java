package net.multyfora.client.portable_typewriter;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.multyfora.client.FreqScreenMenu;
import net.multyfora.client.graphics.GraphicsUtils;
import net.multyfora.content.portable_typewriter.PortableTypewriterItem;
import net.multyfora.network.PortableTypewriterSetFreqPacket;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PortableTypewriterScreen extends AbstractContainerScreen<FreqScreenMenu> {

    static final int
        KEY_WIDTH = 18,
        KEY_HEIGHT = 18,
        KEY_GAP = 2,
        ROW_GAP = 4,
        KEY_UNIT = KEY_WIDTH + KEY_GAP,
        TOTAL_UNITS = 15,
        TOTAL_WIDTH = TOTAL_UNITS * KEY_UNIT - KEY_GAP
    ;

    private static final int
        FREQ_SLOT_SIZE = 22,
        FREQ_SLOT_GAP = 8,
        INV_SLOT_SIZE = 22,
        INV_SLOT_GAP = 2,
        INV_SLOT_THICKNESS = 2
    ;

    private static final int CONTENT_HEIGHT = 235;

    private final List<KeyWidget> keyWidgets = new ArrayList<>();
    private int selectedKeyCode = -1;

    private ItemStack firstFreqItem = ItemStack.EMPTY;
    private ItemStack secondFreqItem = ItemStack.EMPTY;
    private ItemStack cursorItem = ItemStack.EMPTY;

    public PortableTypewriterScreen(FreqScreenMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = TOTAL_WIDTH;
        this.imageHeight = CONTENT_HEIGHT;
    }

    @Override
    protected void init() {
        this.imageWidth = TOTAL_WIDTH;
        this.imageHeight = CONTENT_HEIGHT;
        super.init();
        this.leftPos = (width - TOTAL_WIDTH) / 2;
        this.topPos = keyboardTop();

        keyWidgets.clear();

        ItemStack held = getHeldItem();
        selectedKeyCode = -1;
        if (held != null)
            selectedKeyCode = PortableTypewriterItem.getSelectedKey(held);

        int cx = width / 2;
        int startX = cx - TOTAL_WIDTH / 2;
        int startY = keyboardTop();

        KeyRow[] rows = getKeyboardLayout();
        int y = startY;
        for (KeyRow row : rows) {
            float x = startX + row.offset * KEY_UNIT;
            for (KeyDef key : row.keys) {
                int w = (int) (key.widthUnits * KEY_UNIT - KEY_GAP);
                keyWidgets.add(new KeyWidget((int) x, y, w, KEY_HEIGHT, key));
                x += key.widthUnits * KEY_UNIT;
            }
            y += KEY_HEIGHT + ROW_GAP;
        }

        loadFreqForSelectedKey();
        updateKeyWidgetStates();
    }

    private int keyboardTop() {
        return height / 2 - 105;
    }

    private int keyboardBottom() {
        return keyboardTop() + 5 * KEY_HEIGHT + 4 * ROW_GAP;
    }

    private int freqSlotsTop() {
        return keyboardBottom() + 14;
    }

    private int freqSlotsCenterX() {
        return width / 2;
    }

    private int invTopY() {
        return freqSlotsTop() + FREQ_SLOT_SIZE + 2*INV_SLOT_THICKNESS - 11;
    }

    private int invLeft() {
        return width/2-9 * INV_SLOT_SIZE/2 + 2*INV_SLOT_THICKNESS;
    }

    private void loadFreqForSelectedKey() {
        firstFreqItem = ItemStack.EMPTY;
        secondFreqItem = ItemStack.EMPTY;
        if (selectedKeyCode < 0) return;
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = getHeldItem();
        if (held == null || mc.level == null) return;
        var registries = mc.level.registryAccess();
        Couple<Frequency> freq = PortableTypewriterItem.getKeyBinding(held, selectedKeyCode, registries);
        if (freq != null) {
            firstFreqItem = freq.getFirst().getStack();
            secondFreqItem = freq.getSecond().getStack();
        }
    }

    private void updateKeyWidgetStates() {
        ItemStack held = getHeldItem();
        if (held == null) return;
        var registries = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : null;
        for (KeyWidget kw : keyWidgets) {
            kw.bound = registries != null
                    && PortableTypewriterItem.hasKeyBinding(held, kw.def.glfwCode);
            kw.selected = kw.def.glfwCode == selectedKeyCode;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!cursorItem.isEmpty()) {
            graphics.renderItem(cursorItem, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, cursorItem, mouseX - 8, mouseY - 8);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.drawCenteredString(
            font,
            Component.translatable("item.joc.portable_typewriter"),
            width/2, height/2 - 120,
            0xFFFFFF
        );

        for(KeyWidget kw : keyWidgets) {
            kw.render(graphics, mouseX, mouseY);
        }

        Vector2i mousePosition = new Vector2i(mouseX, mouseY);
        if(0 <= selectedKeyCode) {
            Vector2i center = new Vector2i(
                freqSlotsCenterX(),
                freqSlotsTop()
            );
            GraphicsUtils.renderFrequencySlots(
                graphics, font,
                center, FREQ_SLOT_SIZE, FREQ_SLOT_GAP,
                mousePosition,
                firstFreqItem, secondFreqItem,
                false, true
            );
        }

        Vector2i origin = new Vector2i(
            invLeft(),
            invTopY()
        );
        GraphicsUtils.renderInventory(
            graphics, font,
            origin,
            FREQ_SLOT_SIZE, FREQ_SLOT_GAP, INV_SLOT_THICKNESS,
            mousePosition
        );
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (KeyWidget kw : keyWidgets) {
                if (kw.isHovered(mouseX, mouseY)) {
                    selectedKeyCode = kw.def.glfwCode;
                    PortableTypewriterClientHandler.setSelectedKey(selectedKeyCode);
                    loadFreqForSelectedKey();
                    updateKeyWidgetStates();
                    return true;
                }
            }

            if (selectedKeyCode >= 0) {
                int cx = freqSlotsCenterX();
                int fy = freqSlotsTop();
                int totalW = FREQ_SLOT_SIZE * 2 + 8;
                int slotX1 = cx - totalW / 2;
                int slotX2 = slotX1 + FREQ_SLOT_SIZE + 8;

                if (isInSlot(mouseX, mouseY, slotX1, fy, FREQ_SLOT_SIZE)) {
                    handleFreqSlotClick(0);
                    return true;
                }
                if (isInSlot(mouseX, mouseY, slotX2, fy, FREQ_SLOT_SIZE)) {
                    handleFreqSlotClick(1);
                    return true;
                }
            }

            if (handleInvClick(mouseX, mouseY)) return true;

            if (!cursorItem.isEmpty()) {
                cursorItem = ItemStack.EMPTY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleInvClick(double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        Inventory inv = mc.player.getInventory();
        int startX = invLeft();
        int startY = invTopY();;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + row * 9 + col;
                int x = startX + col * (INV_SLOT_SIZE-INV_SLOT_THICKNESS);
                int y = startY + row * (INV_SLOT_SIZE-INV_SLOT_THICKNESS);
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
            int x = startX + col * (INV_SLOT_SIZE-INV_SLOT_THICKNESS);
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
        if (selectedKeyCode < 0) return;

        if (!cursorItem.isEmpty()) {
            if (slotIndex == 0) {
                firstFreqItem = cursorItem.copy();
            } else {
                secondFreqItem = cursorItem.copy();
            }
            cursorItem = ItemStack.EMPTY;
        } else {
            if (slotIndex == 0) {
                firstFreqItem = ItemStack.EMPTY;
            } else {
                secondFreqItem = ItemStack.EMPTY;
            }
        }

        saveFreqForSelectedKey();
        updateKeyWidgetStates();
    }

    private void saveFreqForSelectedKey() {
        if (selectedKeyCode < 0) return;
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = getHeldItem();
        if (held == null || mc.level == null) return;
        var registries = mc.level.registryAccess();

        if (firstFreqItem.isEmpty() || secondFreqItem.isEmpty()) {
            PortableTypewriterItem.clearKeyBinding(held, selectedKeyCode);
        } else {
            Couple<Frequency> freq = Couple.create(Frequency.of(firstFreqItem), Frequency.of(secondFreqItem));
            PortableTypewriterItem.setKeyBinding(held, selectedKeyCode, freq, registries);
        }

        CompoundTag firstTag = firstFreqItem.isEmpty() ? new CompoundTag() : (CompoundTag) firstFreqItem.save(registries, new CompoundTag());
        CompoundTag secondTag = secondFreqItem.isEmpty() ? new CompoundTag() : (CompoundTag) secondFreqItem.save(registries, new CompoundTag());
        PacketDistributor.sendToServer(new PortableTypewriterSetFreqPacket(selectedKeyCode, firstTag, secondTag));
    }

    private boolean isInSlot(double mx, double my, int sx, int sy, int size) {
        return mx >= sx && mx <= sx + size && my >= sy && my <= sy + size;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public @org.jetbrains.annotations.Nullable Rect2i getFreqSlotArea(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 1) return null;
        if (selectedKeyCode < 0) return null;
        int cx = freqSlotsCenterX();
        int fy = freqSlotsTop();
        int totalW = FREQ_SLOT_SIZE * 2 + 8;
        int x = cx - totalW / 2 + slotIndex * (FREQ_SLOT_SIZE + 8);
        return new Rect2i(x, fy, FREQ_SLOT_SIZE, FREQ_SLOT_SIZE);
    }

    public void acceptFreqSlotIngredient(int slotIndex, ItemStack stack) {
        if (slotIndex < 0 || slotIndex > 1) return;
        if (selectedKeyCode < 0) return;
        if (slotIndex == 0) {
            firstFreqItem = stack.copy();
        } else {
            secondFreqItem = stack.copy();
        }
        saveFreqForSelectedKey();
        updateKeyWidgetStates();
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
        if (held.getItem() instanceof PortableTypewriterItem) return held;
        held = mc.player.getOffhandItem();
        return held.getItem() instanceof PortableTypewriterItem ? held : null;
    }

    private static KeyRow[] getKeyboardLayout() {
        return new KeyRow[] {
            new KeyRow(0,
                new KeyDef("Esc", GLFW.GLFW_KEY_ESCAPE, 1.5f),
                new KeyDef("1", GLFW.GLFW_KEY_1, 1),
                new KeyDef("2", GLFW.GLFW_KEY_2, 1),
                new KeyDef("3", GLFW.GLFW_KEY_3, 1),
                new KeyDef("4", GLFW.GLFW_KEY_4, 1),
                new KeyDef("5", GLFW.GLFW_KEY_5, 1),
                new KeyDef("6", GLFW.GLFW_KEY_6, 1),
                new KeyDef("7", GLFW.GLFW_KEY_7, 1),
                new KeyDef("8", GLFW.GLFW_KEY_8, 1),
                new KeyDef("9", GLFW.GLFW_KEY_9, 1),
                new KeyDef("0", GLFW.GLFW_KEY_0, 1),
                new KeyDef("-", GLFW.GLFW_KEY_MINUS, 1),
                new KeyDef("=", GLFW.GLFW_KEY_EQUAL, 1),
                new KeyDef("Bck", GLFW.GLFW_KEY_BACKSPACE, 1.5f)
            ),
            new KeyRow(0.5f,
                new KeyDef("Tab", GLFW.GLFW_KEY_TAB, 1.5f),
                new KeyDef("Q", GLFW.GLFW_KEY_Q, 1),
                new KeyDef("W", GLFW.GLFW_KEY_W, 1),
                new KeyDef("E", GLFW.GLFW_KEY_E, 1),
                new KeyDef("R", GLFW.GLFW_KEY_R, 1),
                new KeyDef("T", GLFW.GLFW_KEY_T, 1),
                new KeyDef("Y", GLFW.GLFW_KEY_Y, 1),
                new KeyDef("U", GLFW.GLFW_KEY_U, 1),
                new KeyDef("I", GLFW.GLFW_KEY_I, 1),
                new KeyDef("O", GLFW.GLFW_KEY_O, 1),
                new KeyDef("P", GLFW.GLFW_KEY_P, 1),
                new KeyDef("[", GLFW.GLFW_KEY_LEFT_BRACKET, 1),
                new KeyDef("]", GLFW.GLFW_KEY_RIGHT_BRACKET, 1),
                new KeyDef("\\", GLFW.GLFW_KEY_BACKSLASH, 1.5f)
            ),
            new KeyRow(0.75f,
                new KeyDef("Caps", GLFW.GLFW_KEY_CAPS_LOCK, 1.75f),
                new KeyDef("A", GLFW.GLFW_KEY_A, 1),
                new KeyDef("S", GLFW.GLFW_KEY_S, 1),
                new KeyDef("D", GLFW.GLFW_KEY_D, 1),
                new KeyDef("F", GLFW.GLFW_KEY_F, 1),
                new KeyDef("G", GLFW.GLFW_KEY_G, 1),
                new KeyDef("H", GLFW.GLFW_KEY_H, 1),
                new KeyDef("J", GLFW.GLFW_KEY_J, 1),
                new KeyDef("K", GLFW.GLFW_KEY_K, 1),
                new KeyDef("L", GLFW.GLFW_KEY_L, 1),
                new KeyDef(";", GLFW.GLFW_KEY_SEMICOLON, 1),
                new KeyDef("'", GLFW.GLFW_KEY_APOSTROPHE, 1),
                new KeyDef("Ent", GLFW.GLFW_KEY_ENTER, 2.25f)
            ),
            new KeyRow(1.25f,
                new KeyDef("Shift", GLFW.GLFW_KEY_LEFT_SHIFT, 2.25f),
                new KeyDef("Z", GLFW.GLFW_KEY_Z, 1),
                new KeyDef("X", GLFW.GLFW_KEY_X, 1),
                new KeyDef("C", GLFW.GLFW_KEY_C, 1),
                new KeyDef("V", GLFW.GLFW_KEY_V, 1),
                new KeyDef("B", GLFW.GLFW_KEY_B, 1),
                new KeyDef("N", GLFW.GLFW_KEY_N, 1),
                new KeyDef("M", GLFW.GLFW_KEY_M, 1),
                new KeyDef(",", GLFW.GLFW_KEY_COMMA, 1),
                new KeyDef(".", GLFW.GLFW_KEY_PERIOD, 1),
                new KeyDef("/", GLFW.GLFW_KEY_SLASH, 1),
                new KeyDef("Shift", GLFW.GLFW_KEY_RIGHT_SHIFT, 2.75f)
            ),
            new KeyRow(1.75f,
                new KeyDef("Ctrl", GLFW.GLFW_KEY_LEFT_CONTROL, 1.25f),
                new KeyDef("Alt", GLFW.GLFW_KEY_LEFT_ALT, 1.25f),
                new KeyDef("Space", GLFW.GLFW_KEY_SPACE, 6.25f),
                new KeyDef("Alt", GLFW.GLFW_KEY_RIGHT_ALT, 1.25f),
                new KeyDef("Ctrl", GLFW.GLFW_KEY_RIGHT_CONTROL, 1.25f)
            )
        };
    }

    private static class KeyWidget {
        final int x, y, w, h;
        final KeyDef def;
        boolean bound;
        boolean selected;

        KeyWidget(int x, int y, int w, int h, KeyDef def) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.def = def;
        }

        boolean isHovered(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void render(GuiGraphics graphics, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY);

            int fillColor;
            if (selected) {
                fillColor = 0xFF4488FF;
            } else if (bound) {
                fillColor = 0xFF22AA44;
            } else {
                fillColor = 0xFF333333;
            }
            if (hovered) fillColor = lighten(fillColor);

            graphics.fill(x, y, x + w, y + h, 0xFF000000);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fillColor);

            int textColor = selected || bound ? 0xFFFFFF : 0xAAAAAA;
            String label = def.label;
            int tw = Minecraft.getInstance().font.width(label);
            int tx = x + (w - tw) / 2;
            int ty = y + (h - 8) / 2;
            graphics.drawString(Minecraft.getInstance().font, label, tx, ty, textColor);
        }

        private static int lighten(int color) {
            int r = Math.min(255, (color >> 16 & 0xFF) + 40);
            int g = Math.min(255, (color >> 8 & 0xFF) + 40);
            int b = Math.min(255, (color & 0xFF) + 40);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    record KeyDef(String label, int glfwCode, float widthUnits) {}

    static class KeyRow {
        final float offset;
        final KeyDef[] keys;

        KeyRow(float offset, KeyDef... keys) {
            this.offset = offset;
            this.keys = keys;
        }
    }
}
