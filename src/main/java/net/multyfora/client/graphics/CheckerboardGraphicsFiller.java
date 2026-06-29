package net.multyfora.client.graphics;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2i;

public class CheckerboardGraphicsFiller implements GraphicsFiller {
    final int OUTLINE_COLOR;
    final int FIRST_COLOR, SECOND_COLOR;
    int thickness;

    boolean isHovering = false;
    final int HOVERED_FIRST_COLOR, HOVERED_SECOND_COLOR;

    public CheckerboardGraphicsFiller(int outline_color, int first_color, int second_color, int thickness) {
        this.OUTLINE_COLOR = outline_color;
        this.FIRST_COLOR   = first_color;
        this.SECOND_COLOR  = second_color;
        this.HOVERED_FIRST_COLOR  = GraphicsFiller.getHoveredColor(first_color);
        this.HOVERED_SECOND_COLOR = GraphicsFiller.getHoveredColor(second_color);
        this.thickness = thickness;
    }

    @Override
    public void fill(GuiGraphics graphics, Vector2i start, Vector2i end) {
        graphics.fill(start.x, start.y, end.x, end.y, OUTLINE_COLOR);

        Vector2i middle = new Vector2i(start.x/2 + end.x/2, start.y/2 + end.y/2);
        int first_color = (isHovering ? HOVERED_FIRST_COLOR : FIRST_COLOR);
        int second_color = (isHovering ? HOVERED_SECOND_COLOR : SECOND_COLOR);

        //top left
        graphics.fill(start.x+1, start.y+1, middle.x, middle.y, first_color);
        //top right
        graphics.fill(middle.x, start.y+1, end.x-1, middle.y, second_color);
        //bottom left
        graphics.fill(start.x+1, middle.y, middle.x, end.y-1, second_color);
        //bottom right
        graphics.fill(middle.x, middle.y, end.x-1, end.y-1, first_color);
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
        return new CheckerboardGraphicsFiller(OUTLINE_COLOR, FIRST_COLOR, SECOND_COLOR, thickness);
    }
}
