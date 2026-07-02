package net.multyfora.client.portable_throttle;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.graphics.GraphicsUtils;
import net.neoforged.neoforge.network.PacketDistributor;

import net.multyfora.content.portable_throttle.PortableThrottleItem;
import net.multyfora.network.PortableThrottleSignalPacket;
import org.joml.Vector2i;

import static net.multyfora.AeronauticsJoyofcreation.LOGGER;

/**
 * Vertical slider screen for adjusting the Portable Throttle's output signal strength (0-15).
 * Features an animated bar with a cursor that follows the mouse. Strength changes are
 * sent to the server immediately. The screen has no background overlay for a minimal UI.
 **/
public class PortableThrottleStrengthScreen extends Screen {
    public static final ResourceLocation GUI_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(
            AeronauticsJoyofcreation.MODID,
            "textures/gui/portable_throttle_lever.png"
        )
    ;

    // Layout constants
    private static final int
        BAR_W = 16,
        BAR_H = 100
    ;

    private static final int
        FREQUENCY_SIZE = 22,
        FREQUENCY_GAP = 8
    ;

    // Current value (0.0-1.0), animated value, and last frame's animated value for smooth interpolation
    private float value;
    private float animatedValue;
    private float lastAnimatedValue;
    // Integer strength (0-15)
    private int strength;
    // Display string showing the frequency the throttle is bound to
    private Component freqInfo = Component.empty();

    protected PortableThrottleStrengthScreen() {
        super(
            Component.translatable("item.joc.portable_throttle")
        );
    }

    @Override
    protected void init() {
        strength = PortableThrottleClientHandler.getLastStrength();
        value = strength / 15.0f;
        animatedValue = value;
        lastAnimatedValue = value;
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
        if (newStrength != strength) {
            strength = newStrength;
            forceSignal(strength);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            setFromMouseY(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
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

        graphics.drawCenteredString(font, title, cx, by - 25, 0xFFFFFF);
        // Show the bound frequency info below the bar
        graphics.drawCenteredString(font, freqInfo, cx, by + BAR_H + 18, 0x888888);

        // Draw bar background with border
        final float SCALE = 0.5f;
        final int SLIDER_WIDTH = 36;
        final int SLIDER_HEIGHT = 232;

        graphics.pose().pushPose();
        graphics.pose().scale(SCALE, SCALE, 1.0f);

        graphics.blit(
            GUI_TEXTURE,
            width - SLIDER_WIDTH/2, height - SLIDER_HEIGHT/2,
            0, 0,
            SLIDER_WIDTH, SLIDER_HEIGHT
        );

        // Draw the filled portion of the bar with color interpolated for animation
        float renderValue = (1-partialTick) * lastAnimatedValue + animatedValue * partialTick;
        final int CUT_OFF = SLIDER_HEIGHT - (int)(renderValue * (float)SLIDER_HEIGHT);
        graphics.blit(
            GUI_TEXTURE,
            width - SLIDER_WIDTH/2, height - SLIDER_HEIGHT/2 + CUT_OFF,
            SLIDER_WIDTH, CUT_OFF,
            SLIDER_WIDTH, SLIDER_HEIGHT - CUT_OFF
        );

        // Draw the cursor (horizontal bar showing current strength)
        final int HANDLE_WIDTH = 2*SLIDER_WIDTH;
        final int HANDLE_HEIGHT = 16;

        final int HANDLE_POSITION = height - HANDLE_HEIGHT/2 - SLIDER_HEIGHT/2 + CUT_OFF;
        graphics.blit(
            GUI_TEXTURE,
            width - HANDLE_WIDTH/2, HANDLE_POSITION,
            2*SLIDER_WIDTH, 0,
            HANDLE_WIDTH, HANDLE_HEIGHT
        );

        graphics.pose().popPose();

        // Strength number label next to cursor
        String text = String.valueOf(strength);
        int color_strength = 0x00FE0000 / 15;
        graphics.drawString(
            font, text,
            cx + BAR_W/2 + 16, HANDLE_POSITION/2,
            0xFF000000 + (strength*color_strength & 0x00FF0000), true
        );

        // Frequency Information
        Couple<Frequency> frequencies = null; {
            Minecraft client = Minecraft.getInstance();
            ItemStack held = getHeldItem();
            if(held != null && client.level != null) {
                frequencies = PortableThrottleItem.getFrequency(
                    held,
                    client.level.registryAccess()
                );
            }
        }

        GraphicsUtils.renderFrequencySlots(
            graphics, font,
            new Vector2i(
                width/2 - 2*FREQUENCY_SIZE,
                height/2 - FREQUENCY_SIZE/2
            ), FREQUENCY_SIZE, FREQUENCY_GAP,
            new Vector2i(mouseX, mouseY),
            frequencies == null ? null : frequencies.getFirst().getStack(),
            frequencies == null ? null : frequencies.getSecond().getStack(),
            true, false
        );
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
        PortableThrottleClientHandler.setStrength(strength);
        super.onClose();
    }

    private int barX() {
        return width / 2 - BAR_W / 2;
    }

    // Sends a signal strength packet directly to the server
    private static void forceSignal(int s) {
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
