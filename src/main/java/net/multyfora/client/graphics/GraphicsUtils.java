package net.multyfora.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.multyfora.AeronauticsJoyofcreation;
import org.joml.Vector2i;

import static net.multyfora.client.graphics.GraphicsFillers.INVENTORY_GRAPHICS_FILLER;

public class GraphicsUtils {
    public static boolean isInBounds(Vector2i startPosition, Vector2i endPosition, Vector2i position) {
        return     (startPosition.x <= position.x && position.x < endPosition.x)
                && (startPosition.y <= position.y && position.y < endPosition.y)
        ;
    }

    public static void renderSlot(
        GuiGraphics graphics, Font font, int thickness,
        Vector2i startPosition, Vector2i endPosition, Vector2i mousePosition,
        ItemStack stack
    ) {
        GraphicsFiller filler = INVENTORY_GRAPHICS_FILLER.clone();
        filler.setThickness(thickness);
        renderSlot(graphics, filler, font, startPosition, endPosition, mousePosition, stack);
    }

    public static void renderSlot(
        GuiGraphics graphics, GraphicsFiller filler, Font font,
        Vector2i startPosition, Vector2i endPosition, Vector2i mousePosition,
        ItemStack stack
    ) {
        Vector2i hoverStart = new Vector2i(
            startPosition.x + filler.getThickness()/2 + 1,
            startPosition.y + filler.getThickness()/2 + 1
        );
        Vector2i hoverEnd = new Vector2i(
            endPosition.x + filler.getThickness()/2 - 1,
            endPosition.y + filler.getThickness()/2 - 1
        );
        filler.setHovering( isInBounds(hoverStart, hoverEnd, mousePosition) );
        filler.fill(graphics, startPosition, endPosition);

        Vector2i size = new Vector2i(endPosition.x - startPosition.x, endPosition.y - startPosition.y);
        renderItemStack(
            graphics, font,
            stack, startPosition, size.x
        );
    }

    private static void renderItemStack(
            GuiGraphics graphics, Font font,
            ItemStack stack, Vector2i position, int size
    ) {
        if( !stack.isEmpty() ) {
            int ix = position.x + (size - 16) / 2;
            int iy = position.y + (size - 16) / 2;
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
        final ResourceLocation FREQUENCY_TEXTURES =
            ResourceLocation.fromNamespaceAndPath(
                AeronauticsJoyofcreation.MODID,
                "textures/gui/frequency_slots.png"
            )
        ;

        int totalW = 2*size + gap;
        int slotX1 = center.x - totalW/2;
        int slotX2 = slotX1 + size + gap;
        Vector2i firstStart    = new Vector2i(slotX1, center.y);
        Vector2i secondStart   = new Vector2i(slotX2, center.y);
        Vector2i firstEnd      = new Vector2i(slotX1+size, center.y+size);
        Vector2i secondEnd     = new Vector2i(slotX2+size, center.y+size);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, FREQUENCY_TEXTURES);

        final int FREQUENCY_SLOT_SIZE = 128;
        final float SCALE = 1.0f/5.7f;
        graphics.pose().pushPose();
        graphics.pose().scale(SCALE, SCALE, 1.0f);

        // first (red)
        Vector2i adjustedPosition = new Vector2i(
            (int)( (double)(firstStart.x) / SCALE ),
            (int)( (double)(firstStart.y) / SCALE )
        );
        graphics.blit(
            FREQUENCY_TEXTURES,
            adjustedPosition.x, adjustedPosition.y,
            0, 0,
            FREQUENCY_SLOT_SIZE, FREQUENCY_SLOT_SIZE
        );

        // second (blue)
        adjustedPosition = new Vector2i(
            (int)( (double)(secondStart.x) / SCALE ),
            (int)( (double)(secondStart.y) / SCALE )
        );
        graphics.blit(
            FREQUENCY_TEXTURES,
            adjustedPosition.x, adjustedPosition.y,
            FREQUENCY_SLOT_SIZE, 0,
            FREQUENCY_SLOT_SIZE, FREQUENCY_SLOT_SIZE
        );

        graphics.pose().popPose();

        // Items
        Vector2i itemSize = new Vector2i(firstEnd.x - firstStart.x, firstEnd.x - firstStart.x);
        renderItemStack(
            graphics, font,
            firstItem, firstStart,
            Math.min(itemSize.x, itemSize.y)
        );
        renderItemStack(
            graphics, font,
            secondItem, secondStart,
            Math.min(itemSize.x, itemSize.y)
        );

        // Hover
        GraphicsFiller hoverFiller = new SimpleGraphicsFiller(
            0x33FFFFFF, 0x00FFFFFF, 0
        );

        final Vector2i SLOT_THICKNESS = new Vector2i(1, 1);
        Vector2i adjustedFirstStart  =  firstStart.add(SLOT_THICKNESS);
        Vector2i adjustedFirstEnd    =    firstEnd.sub(SLOT_THICKNESS);
        Vector2i adjustedSecondStart = secondStart.add(SLOT_THICKNESS);
        Vector2i adjustedSecondEnd   =   secondEnd.sub(SLOT_THICKNESS);

        if( isInBounds(adjustedFirstStart, adjustedFirstEnd, mousePosition) ) {
            hoverFiller.fill(graphics, adjustedFirstStart, adjustedFirstEnd);
        }
        if( isInBounds(adjustedSecondStart, adjustedSecondEnd, mousePosition) ) {
            hoverFiller.fill(graphics, adjustedSecondStart, adjustedSecondEnd);
        }
    }

    public static void renderInventory(
        GuiGraphics graphics, Font font,
        Vector2i origin,
        int size, int gap, int thickness,
        Vector2i mousePosition
    ) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) {
            return;
        }
        Inventory inventory = mc.player.getInventory();

        // Inventory
        Vector2i start, end;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + 9*row + col;
                int x = origin.x + col * (size-thickness/2);
                int y = origin.y + row * (size-thickness/2);
                start = new Vector2i(x, y);
                end =   new Vector2i(
                    x+size,
                    y+size
                );

                ItemStack stack = inventory.getItem(slotIdx);
                GraphicsUtils.renderSlot(graphics, font, thickness, start, end, mousePosition, stack);
            }
        }

        // Hotbar
        int hotbarY = origin.y + 3*size + gap;
        for (int col = 0; col < 9; col++) {
            int x = origin.x + col * (size-thickness/2);
            start = new Vector2i(x, hotbarY);
            end   = new Vector2i(
                x + size,
                hotbarY + size
            );

            ItemStack stack = inventory.getItem(col);
            GraphicsUtils.renderSlot(graphics, font, thickness, start, end, mousePosition, stack);
        }
    }

    public static String getCoordinateString(BlockPos position) {
        if(position == null) {
            return "Not Set";
        }
        return
            position.getX() +
            ", " +
            position.getY() +
            ", " +
            position.getZ()
        ;
    }

}
