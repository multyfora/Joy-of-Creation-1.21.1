package net.multyfora.client.graphics;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2i;

public interface GraphicsFiller {
     void fill(GuiGraphics graphics, Vector2i start, Vector2i end);
     void setHoverColor(int color);
     GraphicsFiller clone();
}
