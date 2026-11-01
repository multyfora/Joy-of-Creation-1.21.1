package net.multyfora.client.seeker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.ryanhcode.sable.Sable;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.graphics.GraphicsUtils;
import net.multyfora.client.integration.xaeros.WaypointData;
import net.multyfora.client.integration.xaeros.WaypointPickerScreen;
import net.multyfora.client.integration.xaeros.XaerosCompat;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.network.SeekerPayloads;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSeekerScreen<M extends AbstractSeekerMenu> extends AbstractSimiContainerScreen<M> {
    public static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AeronauticsJoyofcreation.MODID,
                    "textures/gui/seeker_screen_new.png"
            );
    static final ResourceLocation ADVANCED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AeronauticsJoyofcreation.MODID,
                    "textures/gui/seeker_advanced.png"
            );

    private static final int MAIN_WIDTH = AbstractSeekerMenu.MAIN_WIDTH;
    private static final int MAIN_HEIGHT = AbstractSeekerMenu.MAIN_HEIGHT;
    private static final int DRAWER_WIDTH = AbstractSeekerMenu.DRAWER_WIDTH;
    private static final int DRAWER_X = AbstractSeekerMenu.DRAWER_X;
    private static final int DRAWER_PADDING = 10;
    private static final int DRAWER_OPTIONS_Y = 19;
    private static final int DRAWER_OPTION_SPACING = 8;

    static final int TITLE_COLOR = 0xFF592424;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    static final int TITLE_Y = 3;
    private static final int BUTTON_Y = 19;
    private static final int TARGET_LABEL_CENTER_X = 98;
    private static final int TARGET_LABEL_Y = 143;
    private static final int FIELD_LABEL_CENTER_X = 41;
    private static final int FIELD_X = 60;
    private static final int FIRST_FIELD_Y = 46;
    private static final int FIELD_SPACING = 23;
    private static final int FIELD_WIDTH = 130;
    private static final int FIELD_HEIGHT = 12;

    protected final BlockPos pos;

    private final List<SeekerAdvancedOption> advancedOptions;
    private boolean advancedOpen;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    private boolean use3D;
    private boolean fieldsEdited;
    private boolean syncingFields;
    private WaypointData pendingWaypoint;

    private IconButton waypointButton;
    private IconButton modeButton;
    private IconButton advancedButton;
    private SetButton setButton;

    protected AbstractSeekerScreen(M menu, Inventory inventory, Component title, SeekerBlockEntity blockEntity) {
        super(menu, inventory, title);
        this.pos = blockEntity.getBlockPos();
        this.advancedOptions = createAdvancedOptions();
    }

    private List<SeekerAdvancedOption> createAdvancedOptions() {
        return List.of(new SeekerOffsetOption(this), new SeekerLogicCoreOption(this));
    }

    protected abstract PartialModel setButtonModel();

    protected abstract float setButtonScale();

    protected abstract void sendTarget(int x, int y, int z);

    @Override
    protected void init() {
        if (advancedOpen) {
            setWindowSize(DRAWER_X + DRAWER_WIDTH, AbstractSeekerMenu.OPEN_HEIGHT);
        } else {
            setWindowSize(MAIN_WIDTH, MAIN_HEIGHT);
        }
        super.init();

        addFields();
        addWidgets();
        if (advancedOpen) {
            int y = topPos + DRAWER_OPTIONS_Y;
            for (SeekerAdvancedOption option : advancedOptions) {
                option.init(leftPos + DRAWER_X + DRAWER_PADDING, y);
                y += option.height() + DRAWER_OPTION_SPACING;
            }
        }

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
        if (advancedOpen) {
            advancedOptions.forEach(SeekerAdvancedOption::tick);
        }

        updateAdvancedButton();
        setButton.pending = isTargetPending() || advancedOptions.stream().anyMatch(SeekerAdvancedOption::isPending);
    }

    private void refreshTargetFields() {
        if (fieldsEdited) return;
        SeekerBlockEntity blockEntity = getSeeker();
        if (blockEntity == null) return;

        var targetPos = blockEntity.getTargetPosition(true);
        if (targetPos == null) return;

        Vec3 worldPosition = Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), targetPos);
        syncingFields = true;
        syncField(xField, Mth.floor(worldPosition.x));
        if (use3D) syncField(yField, Mth.floor(worldPosition.y));
        syncField(zField, Mth.floor(worldPosition.z));
        syncingFields = false;
    }

    private static void syncField(EditBox field, int value) {
        String text = String.valueOf(value);
        if (!field.isFocused() && !field.getValue().equals(text)) {
            field.setValue(text);
        }
    }

    private void addFields() {
        SeekerBlockEntity blockEntity = getSeeker();
        use3D = blockEntity == null || blockEntity.isUse3D();

        var targetPos = blockEntity != null ? blockEntity.getTargetPosition(true) : null;
        Vec3 worldPosition = targetPos != null
                ? Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), targetPos)
                : Vec3.ZERO;

        xField = createCoordinateField(0, "X", xField, Mth.floor(worldPosition.x));
        yField = createCoordinateField(1, "Y", yField, Mth.floor(worldPosition.y));
        zField = createCoordinateField(2, "Z", zField, Mth.floor(worldPosition.z));
        yField.setEditable(use3D);
    }

    private EditBox createCoordinateField(int row, String label, EditBox previous, int initialValue) {
        EditBox field = addRenderableWidget(new EditBox(
                font, leftPos + FIELD_X, topPos + FIRST_FIELD_Y + FIELD_SPACING * row,
                FIELD_WIDTH, FIELD_HEIGHT, Component.literal(label)
        ));
        field.setBordered(false);
        field.setValue(previous != null ? previous.getValue() : String.valueOf(initialValue));
        field.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
        field.setResponder(s -> {
            if (!syncingFields) fieldsEdited = true;
        });
        return field;
    }

    private void addWidgets() {
        setButton = new SetButton(
                leftPos + 198, topPos + 135, 24, 24,
                setButtonModel(), setButtonScale(), this::applyAll
        );
        addRenderableWidget(setButton);

        waypointButton = new IconButton(leftPos + 8, topPos + BUTTON_Y, AllIcons.I_TARGET);
        waypointButton.setToolTip(Component.translatable("screen.joc.seeker.waypoints"));
        waypointButton.withCallback(this::accessWayPointScreen);
        waypointButton.visible = XaerosCompat.isLoaded();
        addRenderableWidget(waypointButton);

        modeButton = new IconButton(leftPos + 179, topPos + BUTTON_Y, AllIcons.I_MTD_USER_MODE);
        modeButton.withCallback(this::toggleMode);
        updateModeButton();
        addRenderableWidget(modeButton);

        advancedButton = new IconButton(
                leftPos + 199, topPos + BUTTON_Y,
                advancedOpen ? AllIcons.I_CONFIG_BACK : AllIcons.I_CONFIG_OPEN
        );
        advancedButton.withCallback(this::toggleAdvanced);
        updateAdvancedButton();
        addRenderableWidget(advancedButton);
    }

    <W extends GuiEventListener & Renderable & NarratableEntry> W addOptionWidget(W widget) {
        return addRenderableWidget(widget);
    }

    private void toggleAdvanced() {
        advancedOpen = !advancedOpen;
        menu.setAdvancedOpen(advancedOpen);
        rebuildWidgets();
    }

    private void updateAdvancedButton() {
        advancedButton.getToolTip().clear();
        advancedButton.getToolTip().add(Component.translatable(
                advancedOpen ? "screen.joc.seeker.advanced.close" : "screen.joc.seeker.advanced.open"
        ));

        SeekerBlockEntity blockEntity = getSeeker();
        advancedButton.green = blockEntity != null && (blockEntity.hasOffset() || blockEntity.hasLogicCore());
        if (blockEntity != null && blockEntity.hasOffset()) {
            advancedButton.getToolTip().add(Component.translatable(
                    "screen.joc.seeker.offset.current", SeekerOffsetOption.format(blockEntity.getOffset())
            ));
        }
    }

    private void updateModeButton() {
        modeButton.setIcon(use3D ? AllIcons.I_FX_SURFACE_ON : AllIcons.I_MTD_USER_MODE);
        modeButton.setToolTip(Component.translatable(
                use3D ? "screen.joc.seeker.mode.3d" : "screen.joc.seeker.mode.2d"
        ));
    }

    private void toggleMode() {
        SeekerBlockEntity blockEntity = getSeeker();
        use3D = !use3D;

        if (blockEntity != null) {
            blockEntity.setUse3D(use3D);
        }

        yField.setEditable(use3D);
        if (!use3D && blockEntity != null) {
            Vec3 self = Sable.HELPER.projectOutOfSubLevel(blockEntity.getLevel(), pos.getCenter());
            syncingFields = true;
            yField.setValue(String.valueOf(Mth.floor(self.y)));
            syncingFields = false;
        }
        updateModeButton();

        try {
            PacketDistributor.sendToServer(
                    new SeekerPayloads.ToggleModePayload(pos, use3D)
            );
        } catch (Exception ignored) {}
    }

    private void applyAll() {
        boolean optionsPending = false;
        for (SeekerAdvancedOption option : advancedOptions) {
            if (option.isPending()) {
                option.apply();
                optionsPending = true;
            }
        }
        if (!optionsPending || isTargetPending()) {
            applyTarget();
        }
    }

    private void applyTarget() {
        SeekerBlockEntity blockEntity = getSeeker();
        if (blockEntity == null) return;

        int x = parseCoordinate(xField.getValue());
        int y = parseCoordinate(yField.getValue());
        int z = parseCoordinate(zField.getValue());

        beforeTargetApplied(blockEntity);
        blockEntity.setTarget(x, y, z);
        sendTarget(x, y, z);
        fieldsEdited = false;
    }

    protected void beforeTargetApplied(SeekerBlockEntity blockEntity) {}

    protected boolean isTargetPending() {
        return fieldsEdited;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getFocused() instanceof EditBox focused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                submit(focused);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                focusNextField(focused, hasShiftDown());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submit(EditBox focused) {
        for (SeekerAdvancedOption option : advancedOptions) {
            if (option.fields().contains(focused)) {
                option.apply();
                setFocused(null);
                return;
            }
        }
        applyTarget();
        setFocused(null);
    }

    private void focusNextField(EditBox focused, boolean backwards) {
        List<EditBox> order = new ArrayList<>(List.of(xField));
        if (use3D) order.add(yField);
        order.add(zField);
        if (advancedOpen) {
            advancedOptions.forEach(option -> order.addAll(option.fields()));
        }

        int next = Math.floorMod(order.indexOf(focused) + (backwards ? -1 : 1), order.size());
        setFocused(order.get(next));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, MAIN_WIDTH, MAIN_HEIGHT);

        if (advancedOpen) {
            guiGraphics.blit(ADVANCED_TEXTURE, leftPos + DRAWER_X, topPos, 0, 0, DRAWER_WIDTH, MAIN_HEIGHT);
            renderPlayerInventory(guiGraphics, leftPos + AbstractSeekerMenu.INVENTORY_X, topPos + AbstractSeekerMenu.INVENTORY_Y);
            int y = topPos + DRAWER_OPTIONS_Y;
            for (SeekerAdvancedOption option : advancedOptions) {
                option.renderBg(guiGraphics, leftPos + DRAWER_X + DRAWER_PADDING, y);
                y += option.height() + DRAWER_OPTION_SPACING;
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, (MAIN_WIDTH - font.width(title)) / 2, TITLE_Y, TITLE_COLOR, false);

        drawFieldLabel(guiGraphics, "X", 0);
        drawFieldLabel(guiGraphics, "Y", 1);
        drawFieldLabel(guiGraphics, "Z", 2);

        SeekerBlockEntity blockEntity = getSeeker();
        if (blockEntity != null) {
            Component target = Component.translatable(
                    "screen.joc.seeker.target",
                    GraphicsUtils.getCoordinateString(blockEntity.getTarget())
            );
            guiGraphics.drawString(
                    font, target,
                    TARGET_LABEL_CENTER_X - font.width(target) / 2, TARGET_LABEL_Y,
                    TEXT_COLOR, false
            );
        }

        if (advancedOpen) {
            Component heading = Component.translatable("screen.joc.seeker.advanced");
            guiGraphics.drawString(
                    font, heading,
                    DRAWER_X + (DRAWER_WIDTH - font.width(heading)) / 2, TITLE_Y,
                    TITLE_COLOR, false
            );

            int y = DRAWER_OPTIONS_Y;
            for (SeekerAdvancedOption option : advancedOptions) {
                option.renderLabels(guiGraphics, DRAWER_X + DRAWER_PADDING, y);
                y += option.height() + DRAWER_OPTION_SPACING;
            }
        }
    }

    private void drawFieldLabel(GuiGraphics guiGraphics, String label, int row) {
        guiGraphics.drawString(
                font, label,
                FIELD_LABEL_CENTER_X - font.width(label) / 2, FIRST_FIELD_Y + FIELD_SPACING * row,
                TEXT_COLOR, false
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int parseCoordinate(String value) {
        try {
            return Mth.floor(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return 0;
        }
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

    SeekerBlockEntity getSeeker() {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof SeekerBlockEntity blockEntity) {
            return blockEntity;
        }
        return null;
    }

    private static class SetButton extends AbstractSimiWidget {
        private static final float ROTATION_SPEED = 45f;
        private static final int PENDING_COLOR = 0x99FF99;
        private static final int IDLE_COLOR = 0xFFFFFF;

        private final PartialModel model;
        private final float baseScale;
        private final Runnable onPress;

        boolean pending;

        SetButton(int x, int y, int width, int height, PartialModel model, float baseScale, Runnable onPress) {
            super(x, y, width, height, Component.translatable("screen.joc.seeker.set"));
            this.model = model;
            this.baseScale = baseScale;
            this.onPress = onPress;
            toolTip.add(getMessage());
        }

        @Override
        public void doRender(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();
            float scale = hovered ? baseScale * 1.1f : baseScale;

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
                    model,
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
            );

            superBuffer
                    .color(hovered || pending ? PENDING_COLOR : IDLE_COLOR)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

            buffer.endBatch();

            ms.popPose();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            onPress.run();
            setFocused(false);
        }
    }
}
