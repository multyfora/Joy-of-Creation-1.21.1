package net.multyfora.client.portable_throttle;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleSignalPacket;

import org.slf4j.Logger;

// Vertical slider screen for adjusting the Portable Throttle's output signal strength (0-15).
// Features an animated bar with a cursor that follows the mouse. Strength changes are
// sent to the server immediately. The screen has no background overlay for a minimal UI.
public class PortableThrottleStrengthScreen extends Screen {

    private static final Logger LOGGER = AeronauticsJoyofcreation.LOGGER;

    // Layout constants
    private static final int BAR_W = 16;
    private static final int BAR_H = 100;
    private static final int CURSOR_W = 28;
    private static final int CURSOR_H = 3;

    // Current value (0.0-1.0), animated value, and last frame's animated value for smooth interpolation
    private float value;
    private float animatedValue;
    private float lastAnimatedValue;
    // Integer strength (0-15)
    private int strength;
    // Display string showing the frequency the throttle is bound to
    private Component freqInfo = Component.empty();

    protected PortableThrottleStrengthScreen() {
        super(Component.translatable("item.joc.portable_throttle"));
        LOGGER.info("[THROTTLE_SCREEN] Constructor called");
    }

    @Override
    protected void init() {
        LOGGER.info("[THROTTLE_SCREEN] init: screen size={}x{}", width, height);
        strength = PortableThrottleClientHandler.getLastStrength();
        value = strength / 15.0f;
        animatedValue = value;
        lastAnimatedValue = value;
        LOGGER.info("[THROTTLE_SCREEN] init: loaded lastStrength={} value={}", strength, value);

        // Display the currently bound frequency
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = getHeldItem();
        if (held != null && mc.level != null) {
            Couple<Frequency> freq = PortableThrottleItem.getFrequency(held, mc.level.registryAccess());
            if (freq != null) {
                String first = freq.getFirst().getStack().getHoverName().getString();
                String second = freq.getSecond().getStack().getHoverName().getString();
                freqInfo = Component.literal(first + " + " + second);
                LOGGER.info("[THROTTLE_SCREEN] init: freq=({}|{})", first, second);
            } else {
                LOGGER.info("[THROTTLE_SCREEN] init: freq is null");
            }
        } else {
            LOGGER.info("[THROTTLE_SCREEN] init: held={} level={}", held, mc.level);
        }
    }

    private int barY() {
        return height / 2 - BAR_H / 2;
    }

    // Converts mouse Y position to a strength value and sends it to the server
    private void setFromMouseY(double mouseY) {
        int by = barY();
        double pixelFromBottom = by + BAR_H - mouseY;
        pixelFromBottom = Math.max(0, Math.min(BAR_H, pixelFromBottom));
        value = (float) (pixelFromBottom / BAR_H);
        int newStrength = (int) Math.round(pixelFromBottom * 15.0 / BAR_H);
        LOGGER.info("[THROTTLE_SCREEN] setFromMouseY: mouseY={} by={} pixelFromBottom={} value={} newStrength={} oldStrength={}", mouseY, by, pixelFromBottom, value, newStrength, strength);
        if (newStrength != strength) {
            strength = newStrength;
            LOGGER.info("[THROTTLE_SCREEN] setFromMouseY: STRENGTH CHANGED -> {}, sending packet", strength);
            forceSignal(strength);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        LOGGER.info("[THROTTLE_SCREEN] mouseClicked: mouseX={} mouseY={} button={}", mouseX, mouseY, button);
        if (button == 0) {
            setFromMouseY(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            LOGGER.info("[THROTTLE_SCREEN] mouseDragged: mouseX={} mouseY={} button={} dragX={} dragY={}", mouseX, mouseY, button, dragX, dragY);
            setFromMouseY(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // Animate the bar value with exponential smoothing
    @Override
    public void tick() {
        super.tick();
        lastAnimatedValue = animatedValue;
        animatedValue = animatedValue * 0.15f + value * 0.85f;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int bx = barX();
        int by = barY();

        graphics.drawCenteredString(font, title, cx, by - 20, 0xFFFFFF);
        // Show the bound frequency info below the bar
        graphics.drawCenteredString(font, freqInfo, cx, by + BAR_H + 18, 0x888888);

        // Draw bar background with border
        graphics.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 0xFF888888);
        graphics.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF111111);

        // Draw the filled portion of the bar with color interpolated for animation
        float renderValue = lastAnimatedValue * (1 - partialTick) + animatedValue * partialTick;
        int fillH = Math.max(1, (int) (BAR_H * renderValue));
        if (strength > 0) {
            // Green for high strength, blue for low
            int fillColor = strength >= 13 ? 0xFF44FF44 : 0xFF4488FF;
            graphics.fill(bx + 2, by + BAR_H - fillH, bx + BAR_W - 2, by + BAR_H - 1, fillColor);
        }

        // Draw the cursor (horizontal bar showing current strength)
        int cursorY = by + BAR_H - (int) (BAR_H * renderValue);
        int cursorX = cx - CURSOR_W / 2;
        graphics.fill(cursorX - 1, cursorY - CURSOR_H / 2 - 1, cursorX + CURSOR_W + 1, cursorY + CURSOR_H / 2 + 1, 0xFFAAAAAA);
        graphics.fill(cursorX, cursorY - CURSOR_H / 2, cursorX + CURSOR_W, cursorY + CURSOR_H / 2, 0xFFFFFFFF);

        // Strength number label next to cursor
        String text = String.valueOf(strength);
        graphics.drawString(font, text, cx + BAR_W / 2 + 8, cursorY - 4, strength > 0 ? 0xFFFFFF : 0x888888, false);

        if (strength > 0) {
            graphics.drawCenteredString(font,
                Component.translatable("screen.joc.portable_throttle.bound_strength", strength),
                cx, by + BAR_H + 48, 0x22AA44);
        }
    }

    // No background overlay so the game world is visible behind the slider
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void onClose() {
        LOGGER.info("[THROTTLE_SCREEN] onClose: strength={} calling setStrength", strength);
        PortableThrottleClientHandler.setStrength(strength);
        super.onClose();
    }

    private int barX() {
        return width / 2 - BAR_W / 2;
    }

    // Sends a signal strength packet directly to the server
    private static void forceSignal(int s) {
        LOGGER.info("[THROTTLE_SCREEN] forceSignal: sending strength={}", s);
        PacketDistributor.sendToServer(new PortableThrottleSignalPacket(s));
    }

    private static ItemStack getHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof PortableThrottleItem) return held;
        held = mc.player.getOffhandItem();
        return held.getItem() instanceof PortableThrottleItem ? held : null;
    }
}
