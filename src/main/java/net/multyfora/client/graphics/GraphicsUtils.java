package net.multyfora.client.graphics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

import static net.multyfora.client.graphics.GraphicsFillers.INVENTORY_GRAPHICS_FILLER;

public class GraphicsUtils {
    public static boolean isInBounds(Vector2i startPosition, Vector2i endPosition, Vector2i position) {
        return     (startPosition.x <= position.x && position.x < endPosition.x)
                && (startPosition.y <= position.y && position.y < endPosition.y)
        ;
    }

    public static void renderSlot(GuiGraphics graphics, Font font, Vector2i startPosition, Vector2i endPosition, Vector2i mousePosition, ItemStack stack) {
        renderSlot(graphics, INVENTORY_GRAPHICS_FILLER.clone(), font, startPosition, endPosition, mousePosition, stack);
    }

    public static void renderSlot(GuiGraphics graphics, GraphicsFiller filler, Font font, Vector2i startPosition, Vector2i endPosition, Vector2i mousePosition, ItemStack stack) {
        filler.setHovering( isInBounds(startPosition, endPosition, mousePosition) );
        filler.fill(graphics, startPosition, endPosition);

        Vector2i size = new Vector2i(endPosition.x - startPosition.x, endPosition.y - startPosition.y);
        if( !stack.isEmpty() ) {
            int ix = startPosition.x + (size.x - 16) / 2;
            int iy = startPosition.y + (size.y - 16) / 2;
            graphics.renderItem(stack, ix, iy);
            graphics.renderItemDecorations(font, stack, ix, iy);
        }
    }

    public static void renderFrequencySlots(
        GuiGraphics graphics, Font font,
        Vector2i center, int size, int gap,
        Vector2i mousePosition,
        ItemStack firstItem, ItemStack secondItem
    ) {
        int totalW = 2*size + gap;
        int slotX1 = center.x - totalW/2;
        int slotX2 = slotX1 + size + gap;

        Vector2i firstStart    = new Vector2i(slotX1, center.y);
        Vector2i secondStart   = new Vector2i(slotX2, center.y);
        Vector2i firstEnd      = new Vector2i(slotX1+size, center.y+size);
        Vector2i secondEnd     = new Vector2i(slotX2+size, center.y+size);

        GraphicsUtils.renderSlot(graphics, GraphicsFillers.RED_FILLER,  font, firstStart, firstEnd,  mousePosition,   firstItem );
        GraphicsUtils.renderSlot(graphics, GraphicsFillers.BLUE_FILLER, font, secondStart, secondEnd, mousePosition, secondItem);
    }

    public static void renderInventory(
        GuiGraphics graphics, Font font,
        Vector2i origin,
        int size, int gap,
        Vector2i mousePosition
    ) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) {
            return;
        }
        Inventory inventory = mc.player.getInventory();

        Vector2i start, end;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + 9*row + col;
                int x = origin.x + col * size;
                int y = origin.y + row * size;
                start = new Vector2i(x, y);
                end =   new Vector2i(x+size, y+size);

                ItemStack stack = inventory.getItem(slotIdx);
                GraphicsUtils.renderSlot(graphics, font, start, end, mousePosition, stack);
            }
        }

        int hotbarY = origin.y + 3*size + gap;
        for (int col = 0; col < 9; col++) {
            int x = origin.x + col * size;
            start = new Vector2i(x, hotbarY);
            end   = new Vector2i(x+size, hotbarY+size);

            ItemStack stack = inventory.getItem(col);
            GraphicsUtils.renderSlot(graphics, font, start, end, mousePosition, stack);
        }
    }
}
