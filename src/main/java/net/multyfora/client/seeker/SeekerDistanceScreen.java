package net.multyfora.client.seeker;

import com.simibubi.create.foundation.gui.widget.TooltipArea;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.SeekerPartialModels;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.network.SeekerDistancePayloads;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SeekerDistanceScreen extends AbstractSeekerScreen<SeekerDistanceMenu> {
    private static final ResourceLocation SLIDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AeronauticsJoyofcreation.MODID,
                    "textures/gui/seeker_distance_slider.png"
            );
    private static final Component MIN_LABEL = Component.translatable("screen.joc.seeker.distance.min");
    private static final Component MAX_LABEL = Component.translatable("screen.joc.seeker.distance.max");

    private static final int MAX_DISTANCE = 256;

    private static final int ROW_Y = 108;
    private static final int ROW_HEIGHT = 18;
    private static final int TRACK_X = 32;
    private static final int TRACK_WIDTH = 161;
    private static final int BOX_WIDTH = 21;
    private static final int BOX_V = 20;
    private static final int MIN_BOX_X = 8;
    private static final int MAX_BOX_X = 196;
    private static final int LABEL_Y = ROW_Y - 10;
    private static final int VALUE_Y = ROW_Y + 5;

    private static final int BAND_X = TRACK_X + 8;
    private static final int BAND_Y = ROW_Y + 6;
    private static final int BAND_WIDTH = 144;
    private static final int BAND_HEIGHT = 5;
    private static final int BAND_V = 40;
    private static final float BAND_OFF_SHADE = 0.2F;
    private static final float BAND_DIM_SHADE = 0.3F;

    private static final int HANDLE_WIDTH = 7;
    private static final int HANDLE_HEIGHT = 16;
    private static final int HANDLE_Y = ROW_Y + 1;
    private static final int HANDLE_V = 46;
    private static final int MIN_HANDLE_U = 0;
    private static final int MAX_HANDLE_U = 16;
    private static final int HANDLE_ACTIVE_U_OFFSET = 8;

    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int ACTIVE_LABEL_COLOR = 0xFFFFEB8C;

    private int sliderMin;
    private int sliderMax;
    private boolean sliderLoaded;
    private boolean draggingMin;
    private boolean draggingMax;
    private int dragOffset;

    public SeekerDistanceScreen(SeekerDistanceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.blockEntity);
    }

    @Override
    protected void init() {
        if (!sliderLoaded) {
            sliderMin = menu.blockEntity.getMinDistance();
            sliderMax = menu.blockEntity.getMaxDistance();
            sliderLoaded = true;
        }
        super.init();

        addValueTooltip(MIN_BOX_X, MIN_LABEL, "screen.joc.seeker.distance.min");
        addValueTooltip(MAX_BOX_X, MAX_LABEL, "screen.joc.seeker.distance.max");
    }

    private void addValueTooltip(int x, Component label, String key) {
        addRenderableWidget(new TooltipArea(
                leftPos + x, topPos + LABEL_Y, BOX_WIDTH, ROW_Y + ROW_HEIGHT - LABEL_Y
        ).withTooltip(List.of(
                label,
                Component.translatable(key + ".hint_1"),
                Component.translatable(key + ".hint_2")
        )));
    }

    @Override
    protected PartialModel setButtonModel() {
        return SeekerPartialModels.SEEKER_MODULATING;
    }

    @Override
    protected float setButtonScale() {
        return 20f;
    }

    @Override
    protected void beforeTargetApplied(SeekerBlockEntity blockEntity) {
        blockEntity.setMinMaxDistance(sliderMin, sliderMax);
    }

    @Override
    protected void sendTarget(int x, int y, int z) {
        PacketDistributor.sendToServer(
                new SeekerDistancePayloads.UpdateSeekerDistancePayload(pos, x, y, z, sliderMin, sliderMax)
        );
    }

    @Override
    protected boolean isTargetPending() {
        SeekerBlockEntity blockEntity = getSeeker();
        return super.isTargetPending() || (blockEntity != null
                && (sliderMin != blockEntity.getMinDistance() || sliderMax != blockEntity.getMaxDistance()));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);

        int rowY = topPos + ROW_Y;
        guiGraphics.blit(SLIDER_TEXTURE, leftPos + TRACK_X, rowY, 0, 0, TRACK_WIDTH, ROW_HEIGHT);
        guiGraphics.blit(SLIDER_TEXTURE, leftPos + MIN_BOX_X, rowY, 0, BOX_V, BOX_WIDTH, ROW_HEIGHT);
        guiGraphics.blit(SLIDER_TEXTURE, leftPos + MAX_BOX_X, rowY, 0, BOX_V, BOX_WIDTH, ROW_HEIGHT);
        renderBand(guiGraphics);

        drawHandle(guiGraphics, valueToX(sliderMin) - HANDLE_WIDTH, MIN_HANDLE_U, isMinActive(mouseX, mouseY));
        drawHandle(guiGraphics, valueToX(sliderMax), MAX_HANDLE_U, isMaxActive(mouseX, mouseY));
    }

    private void renderBand(GuiGraphics guiGraphics) {
        int minX = valueToX(sliderMin);
        int maxX = valueToX(sliderMax);
        int bandX = leftPos + BAND_X;

        for (int column = 0; column < BAND_WIDTH; column++) {
            float shade = shadeForPower(powerAt(bandX + column, minX, maxX));
            guiGraphics.setColor(shade, shade, shade, 1.0F);
            guiGraphics.blit(SLIDER_TEXTURE, bandX + column, topPos + BAND_Y, column, BAND_V, 1, BAND_HEIGHT);
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int powerAt(int x, int minX, int maxX) {
        if (x < minX) return 15;
        if (x >= maxX) return 0;
        return Math.round(15.0F * (maxX - x) / (maxX - minX));
    }

    private static float shadeForPower(int power) {
        if (power == 0) return BAND_OFF_SHADE;
        return BAND_DIM_SHADE + (1.0F - BAND_DIM_SHADE) * power / 15.0F;
    }

    private void drawHandle(GuiGraphics guiGraphics, int x, int u, boolean active) {
        guiGraphics.blit(
                SLIDER_TEXTURE, x, topPos + HANDLE_Y,
                active ? u + HANDLE_ACTIVE_U_OFFSET : u, HANDLE_V, HANDLE_WIDTH, HANDLE_HEIGHT
        );
    }

    private boolean isMinActive(int mouseX, int mouseY) {
        return draggingMin || (!draggingMax && isOverMinHandle(mouseX, mouseY));
    }

    private boolean isMaxActive(int mouseX, int mouseY) {
        return draggingMax || (!draggingMin && isOverMaxHandle(mouseX, mouseY));
    }

    private boolean isOverMinHandle(double mouseX, double mouseY) {
        int x = valueToX(sliderMin);
        return isOverRow(mouseY) && mouseX >= x - HANDLE_WIDTH && mouseX < x;
    }

    private boolean isOverMaxHandle(double mouseX, double mouseY) {
        int x = valueToX(sliderMax);
        return isOverRow(mouseY) && mouseX >= x && mouseX < x + HANDLE_WIDTH;
    }

    private boolean isOverRow(double mouseY) {
        return mouseY >= topPos + ROW_Y && mouseY < topPos + ROW_Y + ROW_HEIGHT;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        drawValue(guiGraphics, MIN_BOX_X, MIN_LABEL, sliderMin, isMinActive(mouseX, mouseY));
        drawValue(guiGraphics, MAX_BOX_X, MAX_LABEL, sliderMax, isMaxActive(mouseX, mouseY));
    }

    private void drawValue(GuiGraphics guiGraphics, int x, Component label, int value, boolean active) {
        String text = String.valueOf(value);
        guiGraphics.drawString(
                font, label,
                x + (BOX_WIDTH - font.width(label)) / 2, LABEL_Y,
                LABEL_COLOR, false
        );
        guiGraphics.drawString(
                font, text,
                x + (BOX_WIDTH - font.width(text)) / 2, VALUE_Y,
                active ? ACTIVE_LABEL_COLOR : LABEL_COLOR, false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverMinHandle(mouseX, mouseY)) {
                draggingMin = true;
                dragOffset = (int) mouseX - valueToX(sliderMin);
                return true;
            }
            if (isOverMaxHandle(mouseX, mouseY)) {
                draggingMax = true;
                dragOffset = (int) mouseX - valueToX(sliderMax);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && (draggingMin || draggingMax)) {
            int value = xToValue((int) mouseX - dragOffset);

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
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int valueToX(int value) {
        return leftPos + BAND_X + Math.round((float) value / MAX_DISTANCE * BAND_WIDTH);
    }

    private int xToValue(int x) {
        double t = (double) (x - leftPos - BAND_X) / BAND_WIDTH;
        t = Math.max(0, Math.min(1, t));
        return (int) Math.round(t * MAX_DISTANCE);
    }
}
