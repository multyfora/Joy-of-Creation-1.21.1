package net.multyfora.client.graphics;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2i;

public interface GraphicsFiller {
     int HOVER_AVERAGING_COLOR = 0xFF777777;

     void fill(GuiGraphics graphics, Vector2i start, Vector2i end);
     void setHovering(boolean hovering);
     GraphicsFiller clone();

     static int getHoveredColor(int color) {
          int a = (color >> 24) & 0xFF;
          int r = (color >> 16) & 0xFF;
          int g = (color >>  8) & 0xFF;
          int b = (color      ) & 0xFF;

          int ha = (HOVER_AVERAGING_COLOR >> 24) & 0xFF;
          int hr = (HOVER_AVERAGING_COLOR >> 16) & 0xFF;
          int hg = (HOVER_AVERAGING_COLOR >>  8) & 0xFF;
          int hb = (HOVER_AVERAGING_COLOR      ) & 0xFF;

          int aa = (a + ha) / 2;
          int ar = (r + hr) / 2;
          int ag = (g + hg) / 2;
          int ab = (b + hb) / 2;

          //average of input color and averaging-color
          return (aa << 24) | (ar << 16) | (ag << 8) | ab;
     }
}
