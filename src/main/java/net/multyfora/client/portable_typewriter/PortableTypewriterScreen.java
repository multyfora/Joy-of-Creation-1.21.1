package net.multyfora.client.portable_typewriter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

import net.multyfora.content.portable_typewriter.PortableTypewriterItem;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI screen showing a US keyboard layout for binding keys to redstone frequencies.
 * Each key is rendered as a widget; bound keys are highlighted green, the selected key
 * is highlighted blue. Clicking a key selects it for binding to a redstone link.
 **/
public class PortableTypewriterScreen extends Screen {

    // Layout constants for the keyboard grid
    static final int KEY_W = 18;
    static final int KEY_H = 18;
    static final int GAP = 2;
    static final int ROW_GAP = 4;
    static final int UNIT = KEY_W + GAP;
    static final int TOTAL_UNITS = 15;
    static final int TOTAL_W = TOTAL_UNITS * UNIT - GAP;

    private final List<KeyWidget> keyWidgets = new ArrayList<>();
    private int selectedKeyCode = -1;

    protected PortableTypewriterScreen() {
        super(Component.translatable("item.joc.portable_typewriter"));
    }

    @Override
    protected void init() {
        keyWidgets.clear();
        selectedKeyCode = -1;

        // Load the previously selected key from the item stack
        ItemStack held = getHeldItem();
        if (held != null)
            selectedKeyCode = PortableTypewriterItem.getSelectedKey(held);

        // Position the keyboard centered on screen
        int cx = width / 2;
        int startX = cx - TOTAL_W / 2;
        int startY = height / 2 - 100;

        // Build key widgets from the keyboard layout
        KeyRow[] rows = getKeyboardLayout();
        int y = startY;
        for (KeyRow row : rows) {
            float x = startX + row.offset * UNIT;
            for (KeyDef key : row.keys) {
                int w = (int) (key.widthUnits * UNIT - GAP);
                keyWidgets.add(new KeyWidget((int) x, y, w, KEY_H, key));
                x += key.widthUnits * UNIT;
            }
            y += KEY_H + ROW_GAP;
        }

        updateKeyWidgetStates();
    }

    // Updates each widget's bound/selected state from the item stack
    private void updateKeyWidgetStates() {
        ItemStack held = getHeldItem();
        if (held == null) return;
        var registries = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : null;
        for (KeyWidget kw : keyWidgets) {
            kw.bound = registries != null
                    && PortableTypewriterItem.hasKeyBinding(held, kw.def.glfwCode);
            kw.selected = kw.def.glfwCode == selectedKeyCode;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font,
                Component.translatable("item.joc.portable_typewriter"),
                width / 2, height / 2 - 125, 0xFFFFFF);

        // Render all key widgets
        for (KeyWidget kw : keyWidgets)
            kw.render(graphics, mouseX, mouseY);

        // Hint text depending on whether a key is selected
        Component hint = Component.translatable(selectedKeyCode >= 0
                ? "screen.joc.portable_typewriter.selected_hint"
                : "screen.joc.portable_typewriter.click_hint");
        graphics.drawCenteredString(font, hint, width / 2, height / 2 + 70, 0x888888);

        // Count and display how many keys are bound
        int boundCount = 0;
        for (KeyWidget kw : keyWidgets)
            if (kw.bound) boundCount++;
        Component status = Component.translatable("screen.joc.portable_typewriter.bound_count", boundCount);
        graphics.drawCenteredString(font, status, width / 2, height / 2 + 85, 0x666666);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (KeyWidget kw : keyWidgets) {
                if (kw.isHovered(mouseX, mouseY)) {
                    selectedKeyCode = kw.def.glfwCode;
                    PortableTypewriterClientHandler.setSelectedKey(selectedKeyCode);
                    updateKeyWidgetStates();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ItemStack getHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof PortableTypewriterItem) return held;
        held = mc.player.getOffhandItem();
        return held.getItem() instanceof PortableTypewriterItem ? held : null;
    }

    // Keyboard layout definition: 5 rows of keys with standard US keyboard arrangement
    private static KeyRow[] getKeyboardLayout() {
        return new KeyRow[] {
            // Row 1: number row + escape + backspace
            new KeyRow(0,
                new KeyDef("Esc", GLFW.GLFW_KEY_ESCAPE, 1.5f),
                new KeyDef("1", GLFW.GLFW_KEY_1, 1),
                new KeyDef("2", GLFW.GLFW_KEY_2, 1),
                new KeyDef("3", GLFW.GLFW_KEY_3, 1),
                new KeyDef("4", GLFW.GLFW_KEY_4, 1),
                new KeyDef("5", GLFW.GLFW_KEY_5, 1),
                new KeyDef("6", GLFW.GLFW_KEY_6, 1),
                new KeyDef("7", GLFW.GLFW_KEY_7, 1),
                new KeyDef("8", GLFW.GLFW_KEY_8, 1),
                new KeyDef("9", GLFW.GLFW_KEY_9, 1),
                new KeyDef("0", GLFW.GLFW_KEY_0, 1),
                new KeyDef("-", GLFW.GLFW_KEY_MINUS, 1),
                new KeyDef("=", GLFW.GLFW_KEY_EQUAL, 1),
                new KeyDef("Bck", GLFW.GLFW_KEY_BACKSPACE, 1.5f)
            ),
            // Row 2: QWERTY row + tab
            new KeyRow(0.5f,
                new KeyDef("Tab", GLFW.GLFW_KEY_TAB, 1.5f),
                new KeyDef("Q", GLFW.GLFW_KEY_Q, 1),
                new KeyDef("W", GLFW.GLFW_KEY_W, 1),
                new KeyDef("E", GLFW.GLFW_KEY_E, 1),
                new KeyDef("R", GLFW.GLFW_KEY_R, 1),
                new KeyDef("T", GLFW.GLFW_KEY_T, 1),
                new KeyDef("Y", GLFW.GLFW_KEY_Y, 1),
                new KeyDef("U", GLFW.GLFW_KEY_U, 1),
                new KeyDef("I", GLFW.GLFW_KEY_I, 1),
                new KeyDef("O", GLFW.GLFW_KEY_O, 1),
                new KeyDef("P", GLFW.GLFW_KEY_P, 1),
                new KeyDef("[", GLFW.GLFW_KEY_LEFT_BRACKET, 1),
                new KeyDef("]", GLFW.GLFW_KEY_RIGHT_BRACKET, 1),
                new KeyDef("\\", GLFW.GLFW_KEY_BACKSLASH, 1.5f)
            ),
            // Row 3: home row + caps lock + enter
            new KeyRow(0.75f,
                new KeyDef("Caps", GLFW.GLFW_KEY_CAPS_LOCK, 1.75f),
                new KeyDef("A", GLFW.GLFW_KEY_A, 1),
                new KeyDef("S", GLFW.GLFW_KEY_S, 1),
                new KeyDef("D", GLFW.GLFW_KEY_D, 1),
                new KeyDef("F", GLFW.GLFW_KEY_F, 1),
                new KeyDef("G", GLFW.GLFW_KEY_G, 1),
                new KeyDef("H", GLFW.GLFW_KEY_H, 1),
                new KeyDef("J", GLFW.GLFW_KEY_J, 1),
                new KeyDef("K", GLFW.GLFW_KEY_K, 1),
                new KeyDef("L", GLFW.GLFW_KEY_L, 1),
                new KeyDef(";", GLFW.GLFW_KEY_SEMICOLON, 1),
                new KeyDef("'", GLFW.GLFW_KEY_APOSTROPHE, 1),
                new KeyDef("Ent", GLFW.GLFW_KEY_ENTER, 2.25f)
            ),
            // Row 4: shift row
            new KeyRow(1.25f,
                new KeyDef("Shift", GLFW.GLFW_KEY_LEFT_SHIFT, 2.25f),
                new KeyDef("Z", GLFW.GLFW_KEY_Z, 1),
                new KeyDef("X", GLFW.GLFW_KEY_X, 1),
                new KeyDef("C", GLFW.GLFW_KEY_C, 1),
                new KeyDef("V", GLFW.GLFW_KEY_V, 1),
                new KeyDef("B", GLFW.GLFW_KEY_B, 1),
                new KeyDef("N", GLFW.GLFW_KEY_N, 1),
                new KeyDef("M", GLFW.GLFW_KEY_M, 1),
                new KeyDef(",", GLFW.GLFW_KEY_COMMA, 1),
                new KeyDef(".", GLFW.GLFW_KEY_PERIOD, 1),
                new KeyDef("/", GLFW.GLFW_KEY_SLASH, 1),
                new KeyDef("Shift", GLFW.GLFW_KEY_RIGHT_SHIFT, 2.75f)
            ),
            // Row 5: bottom row (ctrl, alt, space)
            new KeyRow(1.75f,
                new KeyDef("Ctrl", GLFW.GLFW_KEY_LEFT_CONTROL, 1.25f),
                new KeyDef("Alt", GLFW.GLFW_KEY_LEFT_ALT, 1.25f),
                new KeyDef("Space", GLFW.GLFW_KEY_SPACE, 6.25f),
                new KeyDef("Alt", GLFW.GLFW_KEY_RIGHT_ALT, 1.25f),
                new KeyDef("Ctrl", GLFW.GLFW_KEY_RIGHT_CONTROL, 1.25f)
            )
        };
    }

    // Visual representation of a single key on the keyboard
    private static class KeyWidget {
        final int x, y, w, h;
        final KeyDef def;
        boolean bound;    // Whether this key has a frequency bound
        boolean selected; // Whether this key is currently selected

        KeyWidget(int x, int y, int w, int h, KeyDef def) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.def = def;
        }

        boolean isHovered(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void render(GuiGraphics graphics, int mouseX, int mouseY) {
            boolean hovered = isHovered(mouseX, mouseY);

            int fillColor;
            if (selected) {
                fillColor = 0xFF4488FF; // Blue for selected
            } else if (bound) {
                fillColor = 0xFF22AA44; // Green for bound
            } else {
                fillColor = 0xFF333333; // Dark gray for unbound
            }
            if (hovered) fillColor = lighten(fillColor);

            // Draw key outline and fill
            graphics.fill(x, y, x + w, y + h, 0xFF000000);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fillColor);

            int textColor = selected || bound ? 0xFFFFFF : 0xAAAAAA;
            String label = def.label;
            int tw = Minecraft.getInstance().font.width(label);
            int tx = x + (w - tw) / 2;
            int ty = y + (h - 8) / 2;
            graphics.drawString(Minecraft.getInstance().font, label, tx, ty, textColor);
        }

        // Lightens a color by adding 40 to each RGB component for hover effect
        private static int lighten(int color) {
            int r = Math.min(255, (color >> 16 & 0xFF) + 40);
            int g = Math.min(255, (color >> 8 & 0xFF) + 40);
            int b = Math.min(255, (color & 0xFF) + 40);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    // Defines a key's label, GLFW code, and width in "units" (multiples of KEY_W + GAP)
    record KeyDef(String label, int glfwCode, float widthUnits) {}

    // Defines a row of keys with a horizontal offset (for staggered keyboard layout)
    static class KeyRow {
        final float offset;
        final KeyDef[] keys;

        KeyRow(float offset, KeyDef... keys) {
            this.offset = offset;
            this.keys = keys;
        }
    }
}
