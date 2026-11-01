package net.multyfora.client.seeker;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.network.SeekerPayloads;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

class SeekerOffsetOption implements SeekerAdvancedOption {
    private static final Component LABEL = Component.translatable("screen.joc.seeker.offset");
    private static final String[] AXES = {"X", "Y", "Z"};

    private static final int ROW_WIDTH = 104;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_SPACING = 20;
    private static final int FIRST_ROW_Y = 21;
    private static final int ROW_V = 170;
    private static final int FIELD_X = 28;
    private static final int FIELD_Y = 5;
    private static final int FIELD_WIDTH = 72;
    private static final int FIELD_HEIGHT = 12;
    private static final int LABEL_COLOR = 0xFFFFFFFF;

    private final AbstractSeekerScreen<?> screen;
    private final EditBox[] fields = new EditBox[AXES.length];
    private final String[] values = {"0", "0", "0"};

    private IconButton applyButton;
    private IconButton resetButton;
    private boolean edited;
    private boolean syncing;

    SeekerOffsetOption(AbstractSeekerScreen<?> screen) {
        this.screen = screen;
    }

    static String format(Vec3 offset) {
        return format(offset.x) + ", " + format(offset.y) + ", " + format(offset.z);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static double parse(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int height() {
        return FIRST_ROW_Y + ROW_SPACING * AXES.length;
    }

    @Override
    public void init(int x, int y) {
        Font font = Minecraft.getInstance().font;
        if (!edited) readFromSeeker();

        screen.addOptionWidget(new TooltipArea(x, y, font.width(LABEL), 18).withTooltip(List.of(
                Component.translatable("screen.joc.seeker.offset.hint_1"),
                Component.translatable("screen.joc.seeker.offset.hint_2"),
                Component.translatable("screen.joc.seeker.offset.hint_3")
        )));

        resetButton = new IconButton(x + ROW_WIDTH - 38, y, AllIcons.I_CONFIG_RESET);
        resetButton.setToolTip(Component.translatable("screen.joc.seeker.offset.reset"));
        resetButton.withCallback(this::reset);
        screen.addOptionWidget(resetButton);

        applyButton = new IconButton(x + ROW_WIDTH - 18, y, AllIcons.I_CONFIRM);
        applyButton.setToolTip(Component.translatable("screen.joc.seeker.offset.apply"));
        applyButton.withCallback(this::apply);
        screen.addOptionWidget(applyButton);

        for (int i = 0; i < AXES.length; i++) {
            int axis = i;
            EditBox field = new EditBox(
                    font, x + FIELD_X, y + FIRST_ROW_Y + ROW_SPACING * i + FIELD_Y,
                    FIELD_WIDTH, FIELD_HEIGHT, Component.literal(AXES[i])
            );
            field.setBordered(false);
            field.setMaxLength(12);
            field.setValue(values[i]);
            field.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
            field.setResponder(s -> {
                values[axis] = s;
                if (!syncing) edited = true;
            });
            fields[i] = screen.addOptionWidget(field);
        }
        updateButtons();
    }

    @Override
    public void renderBg(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < AXES.length; i++) {
            graphics.blit(
                    AbstractSeekerScreen.ADVANCED_TEXTURE,
                    x, y + FIRST_ROW_Y + ROW_SPACING * i,
                    0, ROW_V, ROW_WIDTH, ROW_HEIGHT
            );
        }
    }

    @Override
    public void renderLabels(GuiGraphics graphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, LABEL, x, y + 5, LABEL_COLOR, false);
        for (int i = 0; i < AXES.length; i++) {
            graphics.drawString(
                    font, AXES[i],
                    x + 9 - font.width(AXES[i]) / 2, y + FIRST_ROW_Y + ROW_SPACING * i + FIELD_Y,
                    LABEL_COLOR, false
            );
        }
    }

    @Override
    public void tick() {
        if (!edited) {
            readFromSeeker();
            for (int i = 0; i < AXES.length; i++) {
                if (fields[i] != null && !fields[i].getValue().equals(values[i])) {
                    syncing = true;
                    fields[i].setValue(values[i]);
                    syncing = false;
                }
            }
        }
        updateButtons();
    }

    @Override
    public List<EditBox> fields() {
        return Arrays.asList(fields);
    }

    @Override
    public boolean isPending() {
        return edited;
    }

    @Override
    public void apply() {
        SeekerBlockEntity blockEntity = screen.getSeeker();
        if (blockEntity == null) return;

        double x = parse(values[0]);
        double y = parse(values[1]);
        double z = parse(values[2]);
        blockEntity.setOffset(x, y, z);
        PacketDistributor.sendToServer(new SeekerPayloads.SetOffsetPayload(blockEntity.getBlockPos(), x, y, z));
        edited = false;
    }

    private void reset() {
        syncing = true;
        for (EditBox field : fields) {
            field.setValue("0");
        }
        syncing = false;
        apply();
    }

    private void readFromSeeker() {
        SeekerBlockEntity blockEntity = screen.getSeeker();
        if (blockEntity == null) return;

        Vec3 offset = blockEntity.getOffset();
        values[0] = format(offset.x);
        values[1] = format(offset.y);
        values[2] = format(offset.z);
    }

    private void updateButtons() {
        SeekerBlockEntity blockEntity = screen.getSeeker();
        if (applyButton == null || resetButton == null) return;

        applyButton.active = edited;
        applyButton.green = edited;
        resetButton.active = edited || (blockEntity != null && blockEntity.hasOffset());
    }
}
