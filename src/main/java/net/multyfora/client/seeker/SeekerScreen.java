package net.multyfora.client.seeker;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.Sable;
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

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.graphics.GraphicsUtils;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.network.SeekerPayloads;

import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

public class SeekerScreen extends AbstractContainerScreen<SeekerMenu> {
    public static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(
            AeronauticsJoyofcreation.MODID,
            "textures/gui/seeker.png"
        )
    ;

    // Position of the block being configured
    private final BlockPos pos;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    private boolean use3D;

    private Button modeButton;

    public SeekerScreen(SeekerMenu menu, Inventory inventory) {
        super(
            menu,
            inventory,
            Component.translatable("screen.joc.seeker")
        );
        this.pos = menu.blockEntity.getBlockPos();
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

        addFields();
        addWidgets();
    }

    private void addFields() {
        SeekerBlockEntity blockEntity = getBlockEntity();
        use3D = blockEntity == null || blockEntity.isUse3D();

        int center_x = width/2;
        int center_y = height/2;

        if(blockEntity == null) {
            return;
        }
        Vec3 worldPosition = Sable.HELPER.projectOutOfSubLevel(
            blockEntity.getLevel(),
            blockEntity.getTargetPosition(true)
        );

        xField = createIntegerField(
            new Vector2i(center_x - 80, center_y - 40),
            new Vector2i(160, 20),
            Component.literal("X"),
            (int)worldPosition.x
        );
        yField = createIntegerField(
            new Vector2i(center_x - 80, center_y - 10),
            new Vector2i(160, 20),
            Component.literal("Y"),
            (int)worldPosition.y
        );
        zField = createIntegerField(
            new Vector2i(center_x - 80, center_y + 20),
            new Vector2i(160, 20),
            Component.literal("Z"),
            (int)worldPosition.z
        );

        yField.setEditable(use3D);
    }

    private void addWidgets() {
        int center_x = width/2;
        int center_y = height/2;

        addRenderableWidget(
            Button
                .builder(
                    Component.translatable("screen.joc.seeker.set"),
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
                    Component.translatable("screen.joc.seeker.current_pos"),
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
        modeButton = addRenderableWidget(
                Button
                        .builder(
                                Component.literal(use3D ? "3D" : "2D"),
                                btn -> toggleMode()
                        )
                        .bounds(center_x + 90, center_y - 40, 40, 20)
                        .build()
        );
        return;

    }
    private void toggleMode() {
        SeekerBlockEntity blockEntity = getBlockEntity();
        use3D = !use3D;

        if (blockEntity != null) {
            blockEntity.setUse3D(use3D);
        }

        yField.setEditable(use3D);
        if (!use3D) {
            yField.setValue(String.valueOf(pos.getY()));
        }
        modeButton.setMessage(Component.literal(use3D ? "3D" : "2D"));

        try {
            PacketDistributor.sendToServer(
                    new SeekerPayloads.ToggleModePayload(pos, use3D)
            );
        } catch (Exception ignored) {}
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
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        //positions
        SeekerBlockEntity blockEntity = this.menu.blockEntity;
        if(blockEntity == null) {
            return;
        }

        FormattedText target   = FormattedText.of(
            GraphicsUtils.getCoordinateString( blockEntity.getTarget() )
        );
        FormattedText position = FormattedText.of(
            GraphicsUtils.getCoordinateString(
                this.getCurrentPosition()
            )
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Fills the input fields with the block's own position
    private void useCurrentPos() {
        SeekerBlockEntity blockEntity = (SeekerBlockEntity)getBlockEntity();
        if(blockEntity == null) {
            return;
        }

        BlockPos worldPosition = getCurrentPosition();
        if(worldPosition == null) {
            return;
        }

        Vec3 position = Vec3.atLowerCornerOf(worldPosition);
        xField.setValue( String.valueOf(position.x) );
        yField.setValue( String.valueOf(position.y) );
        zField.setValue( String.valueOf(position.z) );
    }

    // Sends the entered coordinates to the server via UpdateSeekerPayload
    private void sendUpdate(int x, int y, int z) {
        try {
            PacketDistributor.sendToServer(
                new SeekerPayloads.UpdateSeekerPayload(pos, x, y, z)
            );
        } catch(NumberFormatException ignored) {}
    }

    //TODO
    private void accessWayPointScreen() {
        AeronauticsJoyofcreation.LOGGER.info("Waypoints Menu not yet implemented.");
    }

    // Helper to fetch the block entity from the client level
    private SeekerBlockEntity getBlockEntity() {
        Level level = Minecraft.getInstance().level;
        if(
            level != null
            && level.getBlockEntity(pos) instanceof SeekerBlockEntity blockEntity
        ) {
            return blockEntity;
        }
        return null;
    }

    private BlockPos getCurrentPosition() {
        if(this.menu.blockEntity == null) {
            return null;
        }

        return BlockPos.containing(
            Sable.HELPER.projectOutOfSubLevel(
                this.menu.blockEntity.getLevel(),
                this.menu.blockEntity.getBlockPos().getCenter()
            )
        );
    }
}
