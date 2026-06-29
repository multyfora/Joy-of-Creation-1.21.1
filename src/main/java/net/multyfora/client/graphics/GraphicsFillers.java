package net.multyfora.client.graphics;

public class GraphicsFillers {
    public static final GraphicsFiller INVENTORY_GRAPHICS_FILLER = new SimpleGraphicsFiller(
        0xFF333333, 0xFF000000, 4
    );
    public static final GraphicsFiller RED_FILLER  = new CheckerboardGraphicsFiller(
        0xFF000000, 0xFF7c2c3a, 0xFF95323a, 2
    );
    public static final GraphicsFiller BLUE_FILLER = new CheckerboardGraphicsFiller(
        0xFF000000, 0xFF4f58a9, 0xFF5270c4, 2
    );
}
