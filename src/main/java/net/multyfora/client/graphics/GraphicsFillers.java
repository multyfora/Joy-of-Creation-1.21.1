package net.multyfora.client.graphics;

public class GraphicsFillers {
    public static final GraphicsFiller INVENTORY_GRAPHICS_FILLER; static {
        final int FOREGROUND_COLOR = 0xFF000000;
        final int BACKGROUND_COLOR = 0xFF333333;
        INVENTORY_GRAPHICS_FILLER = new SimpleGraphicsFiller(BACKGROUND_COLOR, FOREGROUND_COLOR);
    }
    public static final GraphicsFiller RED_FILLER  = new CheckerboardGraphicsFiller(0xFF000000, 0xFF7c2c3a, 0xFF95323a);
    public static final GraphicsFiller BLUE_FILLER = new CheckerboardGraphicsFiller(0xFF000000, 0xFF4f58a9, 0xFF5270c4);
}
