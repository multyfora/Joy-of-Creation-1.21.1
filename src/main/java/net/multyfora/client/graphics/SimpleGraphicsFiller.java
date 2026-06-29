package net.multyfora.client.graphics;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2i;

public class SimpleGraphicsFiller implements GraphicsFiller {
    private final int background_color;
    private final int foreground_color;
    private int thickness;

    private boolean isHovering = false;
    private final int hovering_background_color;

    public SimpleGraphicsFiller(int background_color, int foreground_color, int thickness) {
        this.background_color = background_color;
        this.foreground_color = foreground_color;
        this.setThickness(thickness);
        this.hovering_background_color = GraphicsFiller.getHoveredColor(background_color);
    }

    @Override
    public void fill(GuiGraphics graphics, Vector2i start, Vector2i end) {
        graphics.fill(start.x, start.y, end.x, end.y, foreground_color);
        graphics.fill(
            start.x + thickness/2, start.y + thickness/2,
              end.x - thickness/2,  end.y - thickness/2,
            (isHovering ? hovering_background_color : background_color)
        );
    }

    @Override
    public void setHovering(boolean hovering) {
        this.isHovering = hovering;
    }

    @Override
    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    @Override
    public int getThickness() {
        return this.thickness;
    }

    @Override
    public GraphicsFiller clone() {
        return new SimpleGraphicsFiller(background_color, foreground_color, thickness);
    }
}
