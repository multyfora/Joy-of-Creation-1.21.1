package net.multyfora.client.coordnav;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.graphics.GraphicsUtils;
import net.multyfora.content.coordnav.CoordNavBlockEntity;
import net.multyfora.network.CoordNavPayloads;

import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2i;

/**
 * GUI screen for configuring the Coordinate Navigator's target coordinates.
 * Shows three input fields (X, Y, Z), a Set button to apply, and a Current Pos button
 * to fill in the block's own position.
 **/
public class CoordNavScreen extends AbstractContainerScreen<CoordNavMenu> {
    public static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(
            AeronauticsJoyofcreation.MODID,
            "textures/gui/coord_navigator/coord_navigator.png"
        )
    ;

    // Position of the block being configured
    private final BlockPos pos;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public CoordNavScreen(CoordNavMenu menu, Inventory inventory) {
        super(
            menu,
            inventory,
            Component.translatable("screen.joc.coord_navigator")
        );
        this.pos = menu.blockEntity.getBlockPos();
    }

    // Helper to fetch the block entity from the client level
    private CoordNavBlockEntity getBlockEntity() {
        Level level = Minecraft.getInstance().level;
        if(
            level != null
            && level.getBlockEntity(pos) instanceof CoordNavBlockEntity blockEntity
        ) {
            return blockEntity;
        }
        return null;
    }

    @Override
    protected void init() {
        // Title position
        this.titleLabelX = (  width  -  this.font.width(this.title)  ) / 2;
        this.titleLabelY = (int)( 0.25 * height );
        // Image size
        this.imageWidth = 176;
        this.imageHeight = 176;
        // remove "Inventory" label
        this.inventoryLabelX = -1000;

        CoordNavBlockEntity blockEntity = getBlockEntity();
        int center_x = width/2;
        int center_y = height/2;

        xField = createIntegerField(
            new Vector2i(center_x - 80, center_y - 40),
            new Vector2i(160, 20),
            Component.literal("X"),
            ( blockEntity != null ? (int)blockEntity.getTargetX() : pos.getX() )
        );
        yField = createIntegerField(
            new Vector2i(center_x - 80, center_y - 10),
            new Vector2i(160, 20),
            Component.literal("Y"),
            ( blockEntity != null ? (int)blockEntity.getTargetY() : pos.getY() )
        );
        zField = createIntegerField(
            new Vector2i(center_x - 80, center_y + 20),
            new Vector2i(160, 20),
            Component.literal("Z"),
            ( blockEntity != null ? (int)blockEntity.getTargetZ() : pos.getZ() )
        );

        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("screen.joc.coord_navigator.set"),
                    (btn) -> {
                        if(this.menu.blockEntity == null) {
                            return;
                        }

                        int x = Integer.parseInt( xField.getValue() );
                        int y = Integer.parseInt( yField.getValue() );
                        int z = Integer.parseInt( zField.getValue() );

                        this.menu.blockEntity.setTarget(x, y, z);
                        sendUpdate(x, y, z);
                    }
                )
                .bounds(center_x - 80, center_y + 50, 75, 20)
                .build()
        );
        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("screen.joc.coord_navigator.current_pos"),
                    btn -> useCurrentPos()
                )
                .bounds(center_x + 5, center_y + 50, 75, 20)
                .build()
        );
        addRenderableWidget(
            Button
                .builder(
                    Component.literal("⌖"),
                    btn -> accessWayPointScreen()
                )
                .bounds(
                    (width - imageWidth)/2 + 4,
                    (height - imageHeight)/2 + 16/2,
                    16,
                    16
                )
                .build()
        );
    }

    private EditBox createIntegerField(
        Vector2i position, Vector2i size,
        Component text, int initial_value
    ) {
        EditBox field;

        field = addRenderableWidget(
            new EditBox(font, position.x, position.y, size.x, size.y, text)
        );
        field.setValue( String.valueOf(initial_value) );
        field.setFilter(
            s -> s.matches("-?\\d*\\.?\\d*") // Allow negative numbers and decimals
        );

        return field;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int gui_x = (width - imageWidth) / 2;
        int gui_y = (height - imageHeight) / 2;
        guiGraphics.blit(GUI_TEXTURE, gui_x, gui_y+5, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        //positions
        CoordNavBlockEntity blockEntity = this.menu.blockEntity;
        if(blockEntity == null) {
            return;
        }

        FormattedText target   = FormattedText.of(
            GraphicsUtils.getCoordinateString( blockEntity.getTarget() )
        );
        FormattedText position = FormattedText.of(
            GraphicsUtils.getCoordinateString( blockEntity.getBlockPos() )
        );

        guiGraphics.drawString(
            this.font,
            target.getString(),
            ( width  - this.font.width(target) )/2 - 44,
            ( height - this.font.lineHeight    )/2 + 78,
            0xFFFFFFFF,
            false
        );
        guiGraphics.drawString(
            this.font,
            position.getString(),
            ( width  - this.font.width(position) )/2 + 44,
            ( height - this.font.lineHeight      )/2 + 78,
            0xFFFFFFFF,
            false
        );
    }

    // Sends the entered coordinates to the server via UpdateCoordPayload
    private void sendUpdate(int x, int y, int z) {
        try {
            PacketDistributor.sendToServer(
                new CoordNavPayloads.UpdateCoordPayload(pos, x, y, z)
            );
        } catch(NumberFormatException ignored) {}
    }

    // Fills the input fields with the block's own position
    private void useCurrentPos() {
        xField.setValue(  String.valueOf( pos.getX() )  );
        yField.setValue(  String.valueOf( pos.getY() )  );
        zField.setValue(  String.valueOf( pos.getZ() )  );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //TODO
    private void accessWayPointScreen() {
        AeronauticsJoyofcreation.LOGGER.info("Waypoints Menu not yet implemented.");
    }
}
