package net.multyfora.client.integration.xaeros;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class WaypointPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 210;
    private static final int ENTRY_HEIGHT = 28;

    private final Screen parent;
    private final BlockPos seekerPos;
    private final Consumer<WaypointData> callback;
    private final List<WaypointData> waypoints;
    private int scrollOffset;


    //FUCK yeah making screens from code and without textures is awesome
    public WaypointPickerScreen(Screen parent, BlockPos seekerPos, List<WaypointData> waypoints,
                                Consumer<WaypointData> callback) {
        super(Component.translatable("screen.joc.seeker.waypoint_picker"));
        this.parent = parent;
        this.seekerPos = seekerPos;
        this.waypoints = waypoints;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int pY = (height - PANEL_HEIGHT) / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> onClose()
        ).bounds(width / 2 - 40, pY + PANEL_HEIGHT - 26, 80, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int pX = (width - PANEL_WIDTH) / 2;
        int pY = (height - PANEL_HEIGHT) / 2;

        graphics.fill(pX - 1, pY - 1, pX + PANEL_WIDTH + 1, pY + PANEL_HEIGHT + 1, 0xFF383838);
        graphics.fill(pX, pY, pX + PANEL_WIDTH, pY + PANEL_HEIGHT, 0xFF141414);
        graphics.fill(pX, pY + 22, pX + PANEL_WIDTH, pY + 23, 0xFF2A2A2A); // Clean separator line
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int pX = (width - PANEL_WIDTH) / 2;
        int pY = (height - PANEL_HEIGHT) / 2;

        graphics.drawCenteredString(font, title, width / 2, pY + 8, 0xFFFFFFFF);

        if (waypoints.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.joc.seeker.no_waypoints"),
                    width / 2, height / 2, 0xFFFF5555);
            return;
        }

        int listTop = pY + 25;
        int listBottom = pY + PANEL_HEIGHT - 32;
        int listLeft = pX + 5;
        int listRight = pX + PANEL_WIDTH - 5;

        int maxVisible = (listBottom - listTop) / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, waypoints.size() - maxVisible);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        graphics.enableScissor(listLeft, listTop, listRight, listBottom);

        for (int i = 0; i < maxVisible && (scrollOffset + i) < waypoints.size(); i++) {
            int idx = scrollOffset + i;
            WaypointData wp = waypoints.get(idx);
            int y = listTop + i * ENTRY_HEIGHT;

            boolean hovered = mouseX >= listLeft && mouseX <= listRight
                    && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            int bgColor = hovered ? 0xFF2A2A2A : 0xFF1A1A1A;
            graphics.fill(listLeft, y + 1, listRight - 10, y + ENTRY_HEIGHT - 1, bgColor);

            int wpColor = wp.color() | 0xFF000000;
            graphics.fill(listLeft, y + 1, listLeft + 3, y + ENTRY_HEIGHT - 1, wpColor);

            double dist = wp.distanceTo(seekerPos.getX(), seekerPos.getY(), seekerPos.getZ());
            String distStr = dist >= 1000 ? String.format("%.0fm", dist) : String.format("%.1fm", dist);
            String initials = wp.initials() != null && !wp.initials().isEmpty() ? " [" + wp.initials() + "]" : "";

            int textX = listLeft + 8;

            graphics.drawString(font, wp.name() + initials, textX, y + 4, 0xFFFFFFFF, false);

            String coords = wp.x() + ", " + wp.y() + ", " + wp.z();
            graphics.drawString(font, coords, textX, y + 15, 0xFFAAAAAA, false);

            int distWidth = font.width(distStr);
            graphics.drawString(font, distStr, listRight - 18 - distWidth, y + 10, 0xFF55FFFF, false);
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int scrollBarHeight = Math.max(15, (int) ((float) maxVisible / waypoints.size() * (listBottom - listTop)));
            int scrollBarY = listTop + (int) ((float) scrollOffset / maxScroll * (listBottom - listTop - scrollBarHeight));
            int scrollBarX = listRight - 6;

            graphics.fill(scrollBarX, listTop, scrollBarX + 2, listBottom, 0xFF222222); // Track
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 2, scrollBarY + scrollBarHeight, 0xFF777777); // Thumb
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int pX = (width - PANEL_WIDTH) / 2;
            int pY = (height - PANEL_HEIGHT) / 2;
            int listTop = pY + 25;
            int listBottom = pY + PANEL_HEIGHT - 32;
            int listLeft = pX + 5;
            int listRight = pX + PANEL_WIDTH - 5;

            if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
                int relativeY = (int) (mouseY - listTop);
                int clickedIndex = scrollOffset + (relativeY / ENTRY_HEIGHT);

                if (clickedIndex < waypoints.size()) {
                    callback.accept(waypoints.get(clickedIndex));
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int pY = (height - PANEL_HEIGHT) / 2;
        int maxVisible = (pY + PANEL_HEIGHT - 32 - (pY + 25)) / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, waypoints.size() - maxVisible);

        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}