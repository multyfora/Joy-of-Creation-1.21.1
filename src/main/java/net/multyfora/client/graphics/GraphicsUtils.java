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
        ItemStack firstItem, ItemStack secondItem,
        boolean isVertical, boolean allowHover
    ) {
        final ResourceLocation FREQUENCY_TEXTURES =
            ResourceLocation.fromNamespaceAndPath(
                AeronauticsJoyofcreation.MODID,
                "textures/gui/frequency_slots.png"
            )
        ;

        int center_delta = (size + gap)/2;
        Vector2i adjustedCenter = new Vector2i(center.x, center.y);
        if(!isVertical) {
            adjustedCenter.sub(
                new Vector2i(size/2, size/2)
            );
        }

        Vector2i firstStart = new Vector2i(
            adjustedCenter.x - (isVertical ? 0 : center_delta),
            adjustedCenter.y - (isVertical ? center_delta : 0)
        );
        Vector2i secondStart = new Vector2i(
            adjustedCenter.x + (isVertical ? 0 : center_delta),
            adjustedCenter.y + (isVertical ? center_delta : 0)
        );

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, FREQUENCY_TEXTURES);

        final int FREQUENCY_SLOT_SIZE = 128;
        final float SCALE = 1.0f/5.7f;
        graphics.pose().pushPose();
        graphics.pose().scale(SCALE, SCALE, 1.0f);

        // first (red)
        Vector2i adjustedFirstPosition = new Vector2i(
            (int)( (double)(firstStart.x) / SCALE ),
            (int)( (double)(firstStart.y) / SCALE )
        );
        graphics.blit(
            FREQUENCY_TEXTURES,
            adjustedFirstPosition.x, adjustedFirstPosition.y,
            0, 0,
            FREQUENCY_SLOT_SIZE, FREQUENCY_SLOT_SIZE
        );

        // second (blue)
        Vector2i adjustedSecondPosition = new Vector2i(
            (int)( (double)(secondStart.x) / SCALE ),
            (int)( (double)(secondStart.y) / SCALE )
        );
        graphics.blit(
            FREQUENCY_TEXTURES,
            adjustedSecondPosition.x, adjustedSecondPosition.y,
            FREQUENCY_SLOT_SIZE, 0,
            FREQUENCY_SLOT_SIZE, FREQUENCY_SLOT_SIZE
        );

        graphics.pose().popPose();

        // Items
        Vector2i firstItemPosition = new Vector2i(
            firstStart.x + size/2,
            firstStart.y + size/2
        );
        renderItemStack(
            graphics, font,
            firstItem, firstItemPosition,
            0
        );

        Vector2i secondItemPosition = new Vector2i(
            secondStart.x + size/2,
            secondStart.y + size/2
        );
        renderItemStack(
            graphics, font,
            secondItem, secondItemPosition,
            0
        );

        // Hover
        if(!allowHover) {
            return;
        }

        GraphicsFiller hoverFiller = new SimpleGraphicsFiller(
            0x33FFFFFF, 0x00FFFFFF, 0
        );

        final int OUTLINE_THICKNESS = 1;

        adjustedFirstPosition.div(1.0f/SCALE);
        adjustedFirstPosition.add(OUTLINE_THICKNESS+1, OUTLINE_THICKNESS+1);
        Vector2i adjustedFirstEnd = new Vector2i(
            adjustedFirstPosition.x + size,
            adjustedFirstPosition.y + size
        );
        adjustedFirstEnd.sub(OUTLINE_THICKNESS+1, OUTLINE_THICKNESS+1);

        adjustedSecondPosition.div(1.0f/SCALE);
        adjustedSecondPosition.add(OUTLINE_THICKNESS+1, OUTLINE_THICKNESS+1);
        Vector2i adjustedSecondEnd = new Vector2i(
            adjustedSecondPosition.x + size,
            adjustedSecondPosition.y + size
        );
        adjustedSecondEnd.sub(OUTLINE_THICKNESS+1, OUTLINE_THICKNESS+1);

        if( isInBounds(adjustedFirstPosition, adjustedFirstEnd, mousePosition) ) {
            hoverFiller.fill(graphics, adjustedFirstPosition, adjustedFirstEnd);
        }
        if( isInBounds(adjustedSecondPosition, adjustedSecondEnd, mousePosition) ) {
            hoverFiller.fill(graphics, adjustedSecondPosition, adjustedSecondEnd);
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
