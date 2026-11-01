package net.multyfora.client.seeker;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;

interface SeekerAdvancedOption {
    int height();

    void init(int x, int y);

    void renderBg(GuiGraphics graphics, int x, int y);

    void renderLabels(GuiGraphics graphics, int x, int y);

    default void tick() {}

    default List<EditBox> fields() {
        return List.of();
    }

    default boolean isPending() {
        return false;
    }

    default void apply() {}
}
