package net.multyfora.client.seeker;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.ryanhcode.sable.Sable;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.SeekerPartialModels;
import net.multyfora.client.graphics.GraphicsUtils;
import net.multyfora.client.integration.xaeros.WaypointData;
import net.multyfora.client.integration.xaeros.WaypointPickerScreen;
import net.multyfora.client.integration.xaeros.XaerosCompat;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.network.SeekerPayloads;

import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

public class SeekerScreen extends AbstractContainerScreen<SeekerMenu> {
    public static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AeronauticsJoyofcreation.MODID,
                    "textures/gui/seeker_screen_new.png"
            )
            ;

    // Position of the block being configured
    private final BlockPos pos;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    private boolean use3D;
    private boolean fieldsEdited;
    private WaypointData pendingWaypoint;

    private IconButton waypointButton;
    private IconButton modeButton;
    private SpyglassSetButton setButton;

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
        this.imageWidth = 225;
        this.imageHeight = 162;
        // remove "Inventory" label
        this.inventoryLabelX = -1000;

        addFields();
        addWidgets();
        if (pendingWaypoint != null) {
            xField.setValue(String.valueOf(pendingWaypoint.x()));
            yField.setValue(String.valueOf(pendingWaypoint.y()));
            zField.setValue(String.valueOf(pendingWaypoint.z()));
            pendingWaypoint = null;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshTargetFields();
    }

    /**
     * Keeps the X/Y/Z fields synced to the block entity's current tracked target every tick,
     * rather than the block's own position. Skips updating whichever field the user currently
     * has focused, so typing isn't interrupted.
     **/
    private void refreshTargetFields() {
        if (fieldsEdited) return;
        SeekerBlockEntity blockEntity = getBlockEntity();
        if (blockEntity == null || xField == null || yField == null || zField == null) {
            return;
        }

        var targetPos = blockEntity.getTargetPosition(true);
        if (targetPos == null) {
            return;
        }

        Vec3 worldPosition = Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), targetPos);

        if (!xField.isFocused()) {
            xField.setValue(String.valueOf((int) worldPosition.x));
        }
        if (!yField.isFocused()) {
            yField.setValue(String.valueOf((int) worldPosition.y));
        }
        if (!zField.isFocused()) {
            zField.setValue(String.valueOf((int) worldPosition.z));
        }
    }

    private void addFields() {
        SeekerBlockEntity blockEntity = getBlockEntity();
        use3D = blockEntity == null || blockEntity.isUse3D();

        int gui_x = (width - imageWidth) / 2;
        int gui_y = (height - imageHeight) / 2;
        if(blockEntity == null) {
            return;
        }

        var targetPos = blockEntity.getTargetPosition(true);
        Vec3 worldPosition = targetPos != null
                ? Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), targetPos)
                : net.minecraft.world.phys.Vec3.ZERO;

        xField = createIntegerField(
                new Vector2i(gui_x + 60, gui_y + 51),
                new Vector2i(137, 18),
                Component.literal("X"),
                (int) worldPosition.x
        );

        xField.setBordered(false);
        yField = createIntegerField(
                new Vector2i(gui_x + 60, gui_y + 74),
                new Vector2i(137, 18),
                Component.literal("Y"),
                (int) worldPosition.y
        );
        yField.setBordered(false);
        zField = createIntegerField(
                new Vector2i(gui_x + 60, gui_y + 97),
                new Vector2i(137, 18),
                Component.literal("Z"),
                (int) worldPosition.z
        );
        zField.setBordered(false);

        yField.setEditable(use3D);
    }

    private void addWidgets() {
        int center_x = width/2;
        int center_y = height/2;

        int gui_x = (width - imageWidth) / 2;
        int gui_y = (height - imageHeight) / 2;

        setButton = new SpyglassSetButton(
                gui_x + 207, gui_y + 146, 10, 10,
                btn -> {
                    if (this.menu.blockEntity == null) {
                        return;
                    }

                    int x = (int) Double.parseDouble(xField.getValue().isEmpty() ? "0" : xField.getValue());
                    int y = (int) Double.parseDouble(yField.getValue().isEmpty() ? "0" : yField.getValue());
                    int z = (int) Double.parseDouble(zField.getValue().isEmpty() ? "0" : zField.getValue());

                    this.menu.blockEntity.setTarget(x, y, z);
                    sendUpdate(x, y, z);
                }
        );
        addRenderableWidget(setButton);

        waypointButton = new IconButton(
                (width - imageWidth) / 2 + 8,
                (height - imageHeight) / 2 + 24,
                AllIcons.I_TARGET
        );
        waypointButton.withCallback(this::accessWayPointScreen);
        addRenderableWidget(waypointButton);

        modeButton = new IconButton(
                (width - imageWidth) / 2 + (255 - 52),
                (height - imageHeight) / 2 + 24,
                use3D ? AllIcons.I_FX_SURFACE_ON : AllIcons.I_MTD_USER_MODE
        );
        modeButton.withCallback(this::toggleMode);
        addRenderableWidget(modeButton);
        return;

    }

    /**
     * A GUI button rendered as the real 3D SEEKER_SPYGLASS Flywheel partial model, drawn
     * directly via SuperByteBuffer into GUI space, continuously rotating for visual flair.
     **/
    private static class SpyglassSetButton extends AbstractWidget {
        private final OnPress onPress;

        private static final float BASE_SCALE = 30f;
        private static final float ROTATION_SPEED = 45f;

        public SpyglassSetButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.translatable("screen.joc.seeker.set"));
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();
            float scale = hovered ? BASE_SCALE * 1.1f : BASE_SCALE;

            float time = (Util.getMillis() % 360000L) / 1000f;
            float yaw = (time * ROTATION_SPEED) % 360f;

            PoseStack ms = graphics.pose();
            ms.pushPose();

            ms.translate(getX() + width / 2.0, getY() + height / 2.0, 100.0);
            ms.scale(scale, -scale, scale);
            ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25f));
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            ms.translate(-0.5, -0.5, -0.5);

            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

            SuperByteBuffer superBuffer = CachedBuffers.partial(
                    SeekerPartialModels.SEEKER_SPYGLASS,
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
            );

            if (hovered) {
                superBuffer.color(0x99FF99); // light green tint
            } else {
                superBuffer.color(0xFFFFFF); // no tint
            }

            superBuffer
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

            buffer.endBatch();

            ms.popPose();
        }

        public void onClick(double mouseX, double mouseY) {
            onPress.onPress(this);
            setFocused(false);
        }

        @Override
        public void updateWidgetNarration(@NotNull NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        public interface OnPress {
            void onPress(SpyglassSetButton button);
        }
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
        modeButton.setIcon(use3D ? AllIcons.I_FX_SURFACE_ON : AllIcons.I_MTD_USER_MODE);

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
                s -> s.matches("-?\\d*\\.?\\d*") // Allow negative numbers and decimals / note: fuck regex / note 2: i love regex
        );
        field.setResponder(s -> fieldsEdited = true);

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
        int gui_x = (width - imageWidth) / 2;
        int gui_y = (height - imageHeight) / 2;

        guiGraphics.drawString(
                this.font,
                "X",
                gui_x+ 39,
                gui_y + 51,
                0xFFFFFFFF,
                false
        );
        guiGraphics.drawString(
                this.font,
                "Y",
                gui_x+ 39,
                gui_y + 74,
                0xFFFFFFFF,
                false
        );
        guiGraphics.drawString(
                this.font,
                "Z",
                gui_x+ 39,
                gui_y + 97,
                0xFFFFFFFF,
                false
        );


        guiGraphics.drawString(
                this.font,
                "target: " + target.getString(),
                ( width  - this.font.width(target) )/2 - 30,
                ( height - this.font.lineHeight    )/2 + 73,
                0xFFFFFFFF,
                false
        );
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Sends the entered coordinates to the server via UpdateSeekerPayload
    private void sendUpdate(int x, int y, int z) {
        try {
            PacketDistributor.sendToServer(
                    new SeekerPayloads.UpdateSeekerPayload(pos, x, y, z)
            );
        } catch(NumberFormatException ignored) {}
    }

    private void accessWayPointScreen() {
        Level level = Minecraft.getInstance().level;
        if (level == null || !XaerosCompat.isLoaded()) return;

        XaerosCompat.invalidateCache();
        var waypoints = XaerosCompat.getCurrentWaypoints(level);
        Minecraft.getInstance().setScreen(
                new WaypointPickerScreen(this, pos, waypoints, wpd -> {
                    pendingWaypoint = wpd;
                })
        );
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