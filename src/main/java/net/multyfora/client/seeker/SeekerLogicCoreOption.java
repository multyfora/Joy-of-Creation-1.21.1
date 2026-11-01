package net.multyfora.client.seeker;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.multyfora.content.seeker.SeekerLinkBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.multyfora.client.seeker.AbstractSeekerMenu.*;

class SeekerLogicCoreOption implements SeekerAdvancedOption {
    private static final Component LABEL = Component.translatable("screen.joc.seeker.logic_core");
    private static final Component LINKS_HEADING = Component.translatable("screen.joc.seeker.links");

    private static final int HEIGHT = 18;
    private static final int LABEL_Y = 5;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int LINK_LABEL_Y = 5;
    private static final int LINK_LABEL_WIDTH = 9;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_U = 7;
    private static final int SLOT_V = 17;
    private static final int PANEL_BOTTOM_HEIGHT = 12;

    private final AbstractSeekerScreen<?> screen;
    private final List<TooltipArea> linkTooltips = new ArrayList<>();

    SeekerLogicCoreOption(AbstractSeekerScreen<?> screen) {
        this.screen = screen;
    }

    private boolean hasCore() {
        return screen.getMenu().blockEntity.hasLogicCore();
    }

    private static String linkLabel(Direction side) {
        return side.getName().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public void init(int x, int y) {
        Font font = Minecraft.getInstance().font;
        screen.addOptionWidget(new TooltipArea(x, y, font.width(LABEL), HEIGHT).withTooltip(List.of(
                Component.translatable("screen.joc.seeker.logic_core.hint_1"),
                Component.translatable("screen.joc.seeker.logic_core.hint_2"),
                Component.translatable("screen.joc.seeker.logic_core.hint_3")
        )));

        linkTooltips.clear();
        for (int i = 0; i < LINK_SIDES.length; i++) {
            Direction side = LINK_SIDES[i];
            TooltipArea tooltip = new TooltipArea(
                    screen.getGuiLeft() + linkCellX(i), screen.getGuiTop() + linkCellY(i), LINK_LABEL_WIDTH, SLOT_SIZE
            ).withTooltip(List.of(
                    Component.translatable("screen.joc.seeker.links." + side.getName()),
                    Component.translatable("screen.joc.seeker.links.hint")
            ));
            linkTooltips.add(screen.addOptionWidget(tooltip));
        }
        tick();
    }

    @Override
    public void tick() {
        boolean hasCore = hasCore();
        linkTooltips.forEach(tooltip -> tooltip.visible = hasCore);
    }

    @Override
    public void renderBg(GuiGraphics graphics, int x, int y) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        renderSlot(graphics, left + CORE_SLOT_X - 1, top + CORE_SLOT_Y - 1);
        if (!hasCore()) return;

        int panelX = left + DRAWER_X;
        int panelY = top + INVENTORY_Y;
        graphics.blit(
                AbstractSeekerScreen.ADVANCED_TEXTURE, panelX, panelY,
                0, 0, DRAWER_WIDTH, INVENTORY_HEIGHT - PANEL_BOTTOM_HEIGHT
        );
        graphics.blit(
                AbstractSeekerScreen.ADVANCED_TEXTURE, panelX, panelY + INVENTORY_HEIGHT - PANEL_BOTTOM_HEIGHT,
                0, MAIN_HEIGHT - PANEL_BOTTOM_HEIGHT, DRAWER_WIDTH, PANEL_BOTTOM_HEIGHT
        );

        for (int i = 0; i < LINK_SIDES.length; i++) {
            for (int index = 0; index < SeekerLinkBehaviour.FREQUENCIES_PER_SIDE; index++) {
                renderSlot(graphics, left + linkCellX(i) + LINK_SLOTS_X + index * SLOT_SIZE, top + linkCellY(i));
            }
        }
    }

    private static void renderSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(AllGuiTextures.PLAYER_INVENTORY.location, x, y, SLOT_U, SLOT_V, SLOT_SIZE, SLOT_SIZE);
    }

    @Override
    public void renderLabels(GuiGraphics graphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, LABEL, x, y + LABEL_Y, LABEL_COLOR, false);
        if (!hasCore()) return;

        graphics.drawString(
                font, LINKS_HEADING,
                DRAWER_X + (DRAWER_WIDTH - font.width(LINKS_HEADING)) / 2, INVENTORY_Y + AbstractSeekerScreen.TITLE_Y,
                AbstractSeekerScreen.TITLE_COLOR, false
        );
        for (int i = 0; i < LINK_SIDES.length; i++) {
            String label = linkLabel(LINK_SIDES[i]);
            graphics.drawString(
                    font, label,
                    linkCellX(i) + (LINK_LABEL_WIDTH - font.width(label)) / 2, linkCellY(i) + LINK_LABEL_Y,
                    LABEL_COLOR, false
            );
        }
    }
}
