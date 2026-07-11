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
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.network.SeekerDistancePayloads;
import net.multyfora.network.SeekerPayloads;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

public class SeekerDistanceScreen extends AbstractContainerScreen<SeekerDistanceMenu> {
    public static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AeronauticsJoyofcreation.MODID,
                    "textures/gui/seeker_screen_new.png"
            );

    private static final int MAX_DISTANCE = 256;
    private static final int TRACK_HEIGHT = 5;
    private static final int HANDLE_RADIUS = 7;
    private static final int TRACK_BG_COLOR = 0xFF2D2D2D;
    private static final int TRACK_BORDER_COLOR = 0xFF444444;
    private static final int FILL_COLOR = 0xFF4B9EFF;
    private static final int FILL_BORDER_COLOR = 0xFF3A7EDE;
    private static final int HANDLE_COLOR = 0xFFE0E0E0;
    private static final int HANDLE_BORDER_COLOR = 0xFF888888;
    private static final int HANDLE_HIGHLIGHT_COLOR = 0xFFFFFFFF;
    private static final int HANDLE_SHADOW_COLOR = 0xFF555555;
    private static final int HANDLE_ACTIVE_COLOR = 0xFFFFAA00;

    private final BlockPos pos;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private boolean use3D;
    private boolean fieldsEdited;
    private IconButton modeButton;
    private ModulatingSetButton setButton;
    private int sliderMin;
    private int sliderMax;
    private boolean draggingMin;
    private boolean draggingMax;
    private int sliderX;
    private int sliderY;
    private int sliderWidth;

    public SeekerDistanceScreen(SeekerDistanceMenu menu, Inventory inventory) {
        super(menu, inventory, Component.translatable("screen.joc.seeker_distance"));
        this.pos = menu.blockEntity.getBlockPos();
    }

    @Override
    protected void init() {
        this.titleLabelX = (width - this.font.width(this.title)) / 2;
        this.titleLabelY = (int)(0.25 * height);
        this.imageWidth = 225;
        this.imageHeight = 162;
        this.inventoryLabelX = -1000;
        addFields();
        addWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshTargetFields();
    }

    private void refreshTargetFields() {
        if (fieldsEdited) return;
        SeekerBlockEntity blockEntity = getBlockEntity();
        if (blockEntity == null || xField == null || yField == null || zField == null) return;
        var targetPos = blockEntity.getTargetPosition(true);
        if (targetPos == null) return;
        Vec3 worldPosition = Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), targetPos);
        if (!xField.isFocused()) xField.setValue(String.valueOf((int) worldPosition.x));
        if (!yField.isFocused()) yField.setValue(String.valueOf((int) worldPosition.y));
        if (!zField.isFocused()) zField.setValue(String.valueOf((int) worldPosition.z));
    }

    private void addFields() {
        SeekerBlockEntity blockEntity = getBlockEntity();
        use3D = blockEntity == null || blockEntity.isUse3D();
        int gui_x = (width - imageWidth) / 2;
        int gui_y = (height - imageHeight) / 2;
        if (blockEntity == null) return;
        var targetPos = blockEntity.getTargetPosition(true);
        Vec3 worldPosition = targetPos != null
                ? Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), targetPos)
                : Vec3.ZERO;

        xField = createIntegerField(new Vector2i(gui_x + 60, gui_y + 38), new Vector2i(137, 18),
                Component.literal("X"), (int) worldPosition.x);
        xField.setBordered(false);
        yField = createIntegerField(new Vector2i(gui_x + 60, gui_y + 61), new Vector2i(137, 18),
                Component.literal("Y"), (int) worldPosition.y);
        yField.setBordered(false);
        zField = createIntegerField(new Vector2i(gui_x + 60, gui_y + 84), new Vector2i(137, 18),
                Component.literal("Z"), (int) worldPosition.z);
        zField.setBordered(false);
        yField.setEditable(use3D);
    }

    private void addWidgets() {
        int gui_x = (width - imageWidth) / 2;
        int gui_y = (height - imageHeight) / 2;

        SeekerBlockEntity blockEntity = getBlockEntity();
        sliderMin = blockEntity != null ? blockEntity.getMinDistance() : 0;
        sliderMax = blockEntity != null ? blockEntity.getMaxDistance() : 256;
        sliderX = gui_x + 25;
        sliderY = gui_y + 105;
        sliderWidth = 150;

        setButton = new ModulatingSetButton(
                gui_x + 207, gui_y + 146, 10, 10,
                btn -> {
                    if (this.menu.blockEntity == null) return;
                    int x = (int) Double.parseDouble(xField.getValue().isEmpty() ? "0" : xField.getValue());
                    int y = (int) Double.parseDouble(yField.getValue().isEmpty() ? "0" : yField.getValue());
                    int z = (int) Double.parseDouble(zField.getValue().isEmpty() ? "0" : zField.getValue());
                    int min = sliderMin;
                    int max = sliderMax;
                    this.menu.blockEntity.setTarget(x, y, z);
                    this.menu.blockEntity.setMinMaxDistance(min, max);
                    sendUpdate(x, y, z, min, max);
                });
        addRenderableWidget(setButton);

        modeButton = new IconButton(
                (width - imageWidth) / 2 + (255 - 52),
                (height - imageHeight) / 2 + 24,
                use3D ? AllIcons.I_FX_SURFACE_ON : AllIcons.I_MTD_USER_MODE
        );
        modeButton.withCallback(this::toggleMode);
        addRenderableWidget(modeButton);
    }

    private void toggleMode() {
        SeekerBlockEntity blockEntity = getBlockEntity();
        use3D = !use3D;
        if (blockEntity != null) blockEntity.setUse3D(use3D);
        yField.setEditable(use3D);
        if (!use3D) yField.setValue(String.valueOf(pos.getY()));
        modeButton.setIcon(use3D ? AllIcons.I_FX_SURFACE_ON : AllIcons.I_MTD_USER_MODE);
        try {
            PacketDistributor.sendToServer(new SeekerPayloads.ToggleModePayload(pos, use3D));
        } catch (Exception ignored) {}
    }

    private EditBox createIntegerField(Vector2i position, Vector2i size, Component text, int initialValue) {
        EditBox field = addRenderableWidget(new EditBox(font, position.x, position.y, size.x, size.y, text));
        field.setValue(String.valueOf(initialValue));
        field.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
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
        guiGraphics.blit(GUI_TEXTURE, gui_x, gui_y + 5, 0, 0, imageWidth, imageHeight);

        int cy = sliderY + 20;
        int trackStart = sliderX + HANDLE_RADIUS;
        int trackEnd = sliderX + sliderWidth - HANDLE_RADIUS;
        int trackTop = cy - TRACK_HEIGHT / 2;
        int minX = valueToX(sliderMin);
        int maxX = valueToX(sliderMax);

        // Track background with border
        guiGraphics.fill(trackStart - 1, trackTop - 1, trackEnd + 1, trackTop + TRACK_HEIGHT + 1, TRACK_BORDER_COLOR);
        guiGraphics.fill(trackStart, trackTop, trackEnd, trackTop + TRACK_HEIGHT, TRACK_BG_COLOR);

        // Active fill with border
        if (sliderMax > sliderMin) {
            guiGraphics.fill(minX, trackTop - 1, maxX, trackTop + TRACK_HEIGHT + 1, FILL_BORDER_COLOR);
            guiGraphics.fill(minX + 1, trackTop, maxX - 1, trackTop + TRACK_HEIGHT, FILL_COLOR);
        }

        drawHandle(guiGraphics, minX, cy,
                Math.abs(mouseX - minX) <= HANDLE_RADIUS && Math.abs(mouseY - cy) <= HANDLE_RADIUS || draggingMin);
        drawHandle(guiGraphics, maxX, cy,
                Math.abs(mouseX - maxX) <= HANDLE_RADIUS && Math.abs(mouseY - cy) <= HANDLE_RADIUS || draggingMax);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        SeekerBlockEntity blockEntity = this.menu.blockEntity;
        if (blockEntity == null) return;

        FormattedText target = FormattedText.of(GraphicsUtils.getCoordinateString(blockEntity.getTarget()));
        int gui_x = (width - imageWidth) / 2;
        int gui_y = (height - imageHeight) / 2;

        guiGraphics.drawString(this.font, "X", gui_x + 39, gui_y + 38, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, "Y", gui_x + 39, gui_y + 61, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, "Z", gui_x + 39, gui_y + 84, 0xFFFFFFFF, false);

        guiGraphics.drawString(this.font, "Min:" + sliderMin, gui_x + 185, gui_y + 108, 0xFFAAAAAA, false);
        guiGraphics.drawString(this.font, "Max:" + sliderMax, gui_x + 185, gui_y + 118, 0xFFAAAAAA, false);

        guiGraphics.drawString(this.font, "target: " + target.getString(),
                (width - this.font.width(target)) / 2 - 30, (height - this.font.lineHeight) / 2 + 73, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cy = sliderY + 20;
            double dY = Math.abs(mouseY - cy);
            if (dY <= HANDLE_RADIUS + 6) {
                int minHandleX = valueToX(sliderMin);
                int maxHandleX = valueToX(sliderMax);
                double dMin = Math.abs(mouseX - minHandleX);
                double dMax = Math.abs(mouseX - maxHandleX);
                double handleHitRadius = HANDLE_RADIUS * 1.5;

                if (dMax <= handleHitRadius && dMax <= dMin) {
                    draggingMax = true;
                    return true;
                }
                if (dMin <= handleHitRadius) {
                    draggingMin = true;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && (draggingMin || draggingMax)) {
            int value = xToValue((int) mouseX);
            value = Math.max(0, Math.min(MAX_DISTANCE, value));

            if (draggingMin) {
                sliderMin = Math.min(value, sliderMax);
            }
            if (draggingMax) {
                sliderMax = Math.max(value, sliderMin);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingMin || draggingMax) {
            draggingMin = false;
            draggingMax = false;
            SeekerBlockEntity blockEntity = getBlockEntity();
            if (blockEntity != null) {
                blockEntity.setMinMaxDistance(sliderMin, sliderMax);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void sendUpdate(int x, int y, int z, int min, int max) {
        try {
            PacketDistributor.sendToServer(new SeekerDistancePayloads.UpdateSeekerDistancePayload(pos, x, y, z, min, max));
        } catch (NumberFormatException ignored) {}
    }

    private SeekerBlockEntity getBlockEntity() {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof SeekerBlockEntity blockEntity) return blockEntity;
        return null;
    }

    private int trackX() { return sliderX + HANDLE_RADIUS; }
    private int trackWidth() { return sliderWidth - HANDLE_RADIUS * 2; }

    private int valueToX(int value) {
        return trackX() + (int) ((double) value / MAX_DISTANCE * trackWidth());
    }

    private int xToValue(int x) {
        double t = (double) (x - trackX()) / trackWidth();
        t = Math.max(0, Math.min(1, t));
        return (int) Math.round(t * MAX_DISTANCE);
    }

    private void drawHandle(GuiGraphics graphics, int cx, int cy, boolean active) {
        int r = HANDLE_RADIUS;
        int color = active ? HANDLE_ACTIVE_COLOR : HANDLE_COLOR;
        // Shadow (bottom-right)
        graphics.fill(cx - r + 1, cy - r + 2, cx + r + 1, cy + r + 1, HANDLE_SHADOW_COLOR);
        // Border
        graphics.fill(cx - r, cy - r, cx + r, cy + r, HANDLE_BORDER_COLOR);
        // Fill
        graphics.fill(cx - r + 1, cy - r + 1, cx + r - 1, cy + r - 1, color);
        // Highlight (top-left edge)
        graphics.fill(cx - r + 1, cy - r + 1, cx + r - 2, cy - r + 2, HANDLE_HIGHLIGHT_COLOR);
        graphics.fill(cx - r + 1, cy - r + 1, cx - r + 2, cy + r - 2, HANDLE_HIGHLIGHT_COLOR);
    }

    private static class ModulatingSetButton extends AbstractWidget {
        private final OnPress onPress;
        private static final float BASE_SCALE = 30f;
        private static final float ROTATION_SPEED = 45f;

        public ModulatingSetButton(int x, int y, int width, int height, OnPress onPress) {
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
                    SeekerPartialModels.SEEKER_MODULATING,
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            if (hovered) superBuffer.color(0x99FF99);
            else superBuffer.color(0xFFFFFF);
            superBuffer.light(LightTexture.FULL_BRIGHT).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
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
            void onPress(ModulatingSetButton button);
        }
    }
}
