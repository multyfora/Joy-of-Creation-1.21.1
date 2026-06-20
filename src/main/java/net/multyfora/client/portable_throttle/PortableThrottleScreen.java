package net.multyfora.client.portable_throttle;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleConfigPacket;
import net.neoforged.neoforge.network.PacketDistributor;

import org.slf4j.Logger;

// Configuration screen for the Portable Throttle: shows two item slots representing
// the two items that define the redstone link frequency. Left-click a slot to set
// the held item, right-click to clear. Saved settings are sent to the server.
public class PortableThrottleScreen extends Screen {

    private static final Logger LOGGER = AeronauticsJoyofcreation.LOGGER;

    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 8;

    // Items currently displayed in each frequency slot
    private ItemStack firstItem = ItemStack.EMPTY;
    private ItemStack secondItem = ItemStack.EMPTY;
    // Whether unsaved changes exist
    private boolean dirty;

    protected PortableThrottleScreen() {
        super(Component.translatable("item.joc.portable_throttle"));
        LOGGER.info("[THROTTLE_CONFIG_SCREEN] Constructor called");
    }

    @Override
    protected void init() {
        LOGGER.info("[THROTTLE_CONFIG_SCREEN] init");
        dirty = false;
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = getHeldItem();
        if (held != null && mc.level != null) {
            Couple<Frequency> freq = PortableThrottleItem.getFrequency(held, mc.level.registryAccess());
            if (freq != null) {
                firstItem = freq.getFirst().getStack();
                secondItem = freq.getSecond().getStack();
                LOGGER.info("[THROTTLE_CONFIG_SCREEN] init: loaded freq=({}|{})",
                    firstItem.getHoverName().getString(),
                    secondItem.getHoverName().getString());
            } else {
                LOGGER.info("[THROTTLE_CONFIG_SCREEN] init: freq is null");
            }
        } else {
            LOGGER.info("[THROTTLE_CONFIG_SCREEN] init: held={} level={}", held, mc.level);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font,
                Component.translatable("item.joc.portable_throttle"),
                width / 2, height / 2 - 60, 0xFFFFFF);

        // Draw the two frequency slots side by side
        int cx = width / 2;
        int slotY = height / 2 - 20;
        int totalW = SLOT_SIZE * 2 + SLOT_GAP;
        int slotX1 = cx - totalW / 2;
        int slotX2 = slotX1 + SLOT_SIZE + SLOT_GAP;

        renderSlot(graphics, slotX1, slotY, firstItem, "1", mouseX, mouseY);
        renderSlot(graphics, slotX2, slotY, secondItem, "2", mouseX, mouseY);

        Component hint = Component.translatable("screen.joc.portable_throttle.hint");
        graphics.drawCenteredString(font, hint, cx, slotY + SLOT_SIZE + 20, 0x888888);

        if (dirty) {
            Component saved = Component.translatable("screen.joc.portable_throttle.saved");
            graphics.drawCenteredString(font, saved, cx, slotY + SLOT_SIZE + 35, 0x22AA44);
        }
    }

    private void renderSlot(GuiGraphics graphics, int x, int y, ItemStack stack, String label, int mx, int my) {
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

        graphics.drawCenteredString(font, label, x + SLOT_SIZE / 2, y + SLOT_SIZE + 2, 0x888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        LOGGER.info("[THROTTLE_CONFIG_SCREEN] mouseClicked: mouseX={} mouseY={} button={}", mouseX, mouseY, button);
        int cx = width / 2;
        int slotY = height / 2 - 20;
        int totalW = SLOT_SIZE * 2 + SLOT_GAP;
        int slotX1 = cx - totalW / 2;
        int slotX2 = slotX1 + SLOT_SIZE + SLOT_GAP;

        if (button == 0) {
            // Left-click: set the slot to the currently held item (copy with count 1)
            if (isInSlot(mouseX, mouseY, slotX1, slotY)) {
                LOGGER.info("[THROTTLE_CONFIG_SCREEN] mouseClicked: left-click on slot 1");
                handleSlotClick(0);
                return true;
            }
            if (isInSlot(mouseX, mouseY, slotX2, slotY)) {
                LOGGER.info("[THROTTLE_CONFIG_SCREEN] mouseClicked: left-click on slot 2");
                handleSlotClick(1);
                return true;
            }
        }
        if (button == 1) {
            // Right-click: clear the slot
            if (isInSlot(mouseX, mouseY, slotX1, slotY)) {
                LOGGER.info("[THROTTLE_CONFIG_SCREEN] mouseClicked: right-click on slot 1 (clear)");
                firstItem = ItemStack.EMPTY;
                dirty = true;
                save();
                return true;
            }
            if (isInSlot(mouseX, mouseY, slotX2, slotY)) {
                LOGGER.info("[THROTTLE_CONFIG_SCREEN] mouseClicked: right-click on slot 2 (clear)");
                secondItem = ItemStack.EMPTY;
                dirty = true;
                save();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInSlot(double mx, double my, int sx, int sy) {
        return mx >= sx && mx <= sx + SLOT_SIZE && my >= sy && my <= sy + SLOT_SIZE;
    }

    private void handleSlotClick(int slotIndex) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack cursor = mc.player.getMainHandItem();
        if (cursor.isEmpty()) {
            LOGGER.info("[THROTTLE_CONFIG_SCREEN] handleSlotClick: cursor empty, cannot set");
            return;
        }
        ItemStack copy = cursor.copyWithCount(1);
        LOGGER.info("[THROTTLE_CONFIG_SCREEN] handleSlotClick: slot={} item={}", slotIndex, copy.getHoverName().getString());
        if (slotIndex == 0) {
            firstItem = copy;
        } else {
            secondItem = copy;
        }
        dirty = true;
        save();
    }

    // Saves the frequency settings to the held item and sends a config packet to the server
    private void save() {
        if (firstItem.isEmpty() || secondItem.isEmpty()) {
            LOGGER.info("[THROTTLE_CONFIG_SCREEN] save: first or second empty, not saving");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        HolderLookup.Provider registries = level.registryAccess();
        ItemStack held = getHeldItem();
        if (held == null) return;
        Couple<Frequency> freq = Couple.create(Frequency.of(firstItem), Frequency.of(secondItem));
        LOGGER.info("[THROTTLE_CONFIG_SCREEN] save: setting freq=( {} | {} ) on held item and sending packet",
            firstItem.getHoverName().getString(), secondItem.getHoverName().getString());
        PortableThrottleItem.setFrequency(held, freq, registries);
        PacketDistributor.sendToServer(new PortableThrottleConfigPacket(
                (net.minecraft.nbt.CompoundTag)firstItem.save(registries, new net.minecraft.nbt.CompoundTag()),
                (net.minecraft.nbt.CompoundTag)secondItem.save(registries, new net.minecraft.nbt.CompoundTag())));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Gets the held throttle item from mainhand or offhand
    private static ItemStack getHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof PortableThrottleItem) return held;
        held = mc.player.getOffhandItem();
        return held.getItem() instanceof PortableThrottleItem ? held : null;
    }
}
