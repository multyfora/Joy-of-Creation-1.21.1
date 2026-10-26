package net.multyfora.client.portable_typewriter;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class TypewriterLayout {
    public static final float ROW_UNITS = 15.0F;

    public record Key(String label, int glfwCode, float widthUnits) {}

    public record PlacedKey(Key key, int row, float x) {
        public float centerX() {
            return x + key.widthUnits() / 2.0F;
        }
    }

    public static final List<List<Key>> ROWS = List.of(
        List.of(
            key("Esc", GLFW.GLFW_KEY_ESCAPE, 1.5F),
            key("1", GLFW.GLFW_KEY_1), key("2", GLFW.GLFW_KEY_2), key("3", GLFW.GLFW_KEY_3),
            key("4", GLFW.GLFW_KEY_4), key("5", GLFW.GLFW_KEY_5), key("6", GLFW.GLFW_KEY_6),
            key("7", GLFW.GLFW_KEY_7), key("8", GLFW.GLFW_KEY_8), key("9", GLFW.GLFW_KEY_9),
            key("0", GLFW.GLFW_KEY_0), key("-", GLFW.GLFW_KEY_MINUS), key("=", GLFW.GLFW_KEY_EQUAL),
            key("Bck", GLFW.GLFW_KEY_BACKSPACE, 1.5F)
        ),
        List.of(
            key("Tab", GLFW.GLFW_KEY_TAB, 1.5F),
            key("Q", GLFW.GLFW_KEY_Q), key("W", GLFW.GLFW_KEY_W), key("E", GLFW.GLFW_KEY_E),
            key("R", GLFW.GLFW_KEY_R), key("T", GLFW.GLFW_KEY_T), key("Y", GLFW.GLFW_KEY_Y),
            key("U", GLFW.GLFW_KEY_U), key("I", GLFW.GLFW_KEY_I), key("O", GLFW.GLFW_KEY_O),
            key("P", GLFW.GLFW_KEY_P), key("[", GLFW.GLFW_KEY_LEFT_BRACKET), key("]", GLFW.GLFW_KEY_RIGHT_BRACKET),
            key("\\", GLFW.GLFW_KEY_BACKSLASH, 1.5F)
        ),
        List.of(
            key("Caps", GLFW.GLFW_KEY_CAPS_LOCK, 1.75F),
            key("A", GLFW.GLFW_KEY_A), key("S", GLFW.GLFW_KEY_S), key("D", GLFW.GLFW_KEY_D),
            key("F", GLFW.GLFW_KEY_F), key("G", GLFW.GLFW_KEY_G), key("H", GLFW.GLFW_KEY_H),
            key("J", GLFW.GLFW_KEY_J), key("K", GLFW.GLFW_KEY_K), key("L", GLFW.GLFW_KEY_L),
            key(";", GLFW.GLFW_KEY_SEMICOLON), key("'", GLFW.GLFW_KEY_APOSTROPHE),
            key("Ent", GLFW.GLFW_KEY_ENTER, 2.25F)
        ),
        List.of(
            key("Shift", GLFW.GLFW_KEY_LEFT_SHIFT, 2.25F),
            key("Z", GLFW.GLFW_KEY_Z), key("X", GLFW.GLFW_KEY_X), key("C", GLFW.GLFW_KEY_C),
            key("V", GLFW.GLFW_KEY_V), key("B", GLFW.GLFW_KEY_B), key("N", GLFW.GLFW_KEY_N),
            key("M", GLFW.GLFW_KEY_M), key(",", GLFW.GLFW_KEY_COMMA), key(".", GLFW.GLFW_KEY_PERIOD),
            key("/", GLFW.GLFW_KEY_SLASH),
            key("Shift", GLFW.GLFW_KEY_RIGHT_SHIFT, 2.75F)
        ),
        List.of(
            key("Ctrl", GLFW.GLFW_KEY_LEFT_CONTROL, 1.5F),
            key("Alt", GLFW.GLFW_KEY_LEFT_ALT, 1.5F),
            key("Space", GLFW.GLFW_KEY_SPACE, 9.0F),
            key("Alt", GLFW.GLFW_KEY_RIGHT_ALT, 1.5F),
            key("Ctrl", GLFW.GLFW_KEY_RIGHT_CONTROL, 1.5F)
        )
    );

    public static final List<PlacedKey> PLACED_KEYS = place();

    private TypewriterLayout() {}

    private static Key key(String label, int glfwCode) {
        return new Key(label, glfwCode, 1.0F);
    }

    private static Key key(String label, int glfwCode, float widthUnits) {
        return new Key(label, glfwCode, widthUnits);
    }

    private static List<PlacedKey> place() {
        List<PlacedKey> placed = new ArrayList<>();
        for (int row = 0; row < ROWS.size(); row++) {
            float x = 0.0F;
            for (Key key : ROWS.get(row)) {
                placed.add(new PlacedKey(key, row, x));
                x += key.widthUnits();
            }
        }
        return List.copyOf(placed);
    }
}
