package net.multyfora.client.coordnav;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import net.multyfora.content.coordnav.CoordNavBlockEntity;
import net.multyfora.network.CoordNavPayloads;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * GUI screen for configuring the Coordinate Navigator's target coordinates.
 * Shows three input fields (X, Y, Z), a Set button to apply, and a Current Pos button
 * to fill in the block's own position.
 **/
public class CoordNavScreen extends Screen {

    // Position of the block being configured
    private final BlockPos pos;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    private static final Component TITLE = Component.translatable("screen.joc.coord_navigator");
    private static final Component SET_LABEL = Component.translatable("screen.joc.coord_navigator.set");
    private static final Component CURRENT_LABEL = Component.translatable("screen.joc.coord_navigator.current_pos");

    public CoordNavScreen(BlockPos pos) {
        super(TITLE);
        this.pos = pos;
    }

    // Helper to fetch the block entity from the client level
    private CoordNavBlockEntity be() {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof CoordNavBlockEntity be) {
            return be;
        }
        return null;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        CoordNavBlockEntity be = be();
        double initX = be != null ? be.getTargetX() : pos.getX();
        double initY = be != null ? be.getTargetY() : pos.getY();
        double initZ = be != null ? be.getTargetZ() : pos.getZ();

        xField = addRenderableWidget(new EditBox(font, cx - 80, cy - 40, 160, 20, Component.literal("X")));
        xField.setValue(String.valueOf((int) initX));
        xField.setFilter(s -> s.matches("-?\\d*\\.?\\d*")); // Allow negative numbers and decimals

        yField = addRenderableWidget(new EditBox(font, cx - 80, cy - 10, 160, 20, Component.literal("Y")));
        yField.setValue(String.valueOf((int) initY));
        yField.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));

        zField = addRenderableWidget(new EditBox(font, cx - 80, cy + 20, 160, 20, Component.literal("Z")));
        zField.setValue(String.valueOf((int) initZ));
        zField.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));

        addRenderableWidget(
                Button.builder(SET_LABEL, btn -> sendUpdate())
                        .bounds(cx - 80, cy + 50, 75, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(CURRENT_LABEL, btn -> useCurrentPos())
                        .bounds(cx + 5, cy + 50, 75, 20)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int cy = height / 2;

        graphics.drawCenteredString(font, TITLE, cx, cy - 70, 0xFFFFFF);

        // Show current target info
        CoordNavBlockEntity be = be();
        if (be != null) {
            String target = (be.hasTarget()
                    ? "Target: " + (int) be.getTargetX() + ", " + (int) be.getTargetY() + ", " + (int) be.getTargetZ()
                    : "No target set");
            graphics.drawCenteredString(font, Component.literal(target), cx, cy - 100, 0xAAAAAA);
        }

        graphics.drawCenteredString(font, Component.literal("Block: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), cx, cy + 80, 0x888888);
    }

    // Sends the entered coordinates to the server via UpdateCoordPayload
    private void sendUpdate() {
        try {
            double x = Double.parseDouble(xField.getValue());
            double y = Double.parseDouble(yField.getValue());
            double z = Double.parseDouble(zField.getValue());
            PacketDistributor.sendToServer(new CoordNavPayloads.UpdateCoordPayload(pos, x, y, z));
        } catch (NumberFormatException ignored) {}
    }

    // Fills the input fields with the block's own position
    private void useCurrentPos() {
        xField.setValue(String.valueOf(pos.getX()));
        yField.setValue(String.valueOf(pos.getY()));
        zField.setValue(String.valueOf(pos.getZ()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
