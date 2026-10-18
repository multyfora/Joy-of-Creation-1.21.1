package net.multyfora.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.Tickable;

import java.util.List;

public class AnimatedGifTexture extends DynamicTexture implements Tickable {

    private final List<NativeImage> frames;
    private final int frameDurationTicks;
    private int currentFrame = 0;
    private int ticksUntilNext;

    public AnimatedGifTexture(List<NativeImage> frames, int frameDurationTicks) {
        super(frames == null || frames.isEmpty() ? new NativeImage(1, 1, false) : frames.get(0));
        this.frames = frames == null ? List.of() : List.copyOf(frames);
        this.frameDurationTicks = Math.max(1, frameDurationTicks);
        this.ticksUntilNext = this.frameDurationTicks;
    }

    @Override
    public void tick() {
        if (frames.size() <= 1)
            return;
        if (--ticksUntilNext <= 0) {
            ticksUntilNext = frameDurationTicks;
            currentFrame = (currentFrame + 1) % frames.size();
            uploadFrame(frames.get(currentFrame));
        }
    }

    private void uploadFrame(NativeImage frame) {
        this.bind();
        frame.upload(0, 0, 0, 0, 0, frame.getWidth(), frame.getHeight(), false, false, false, false);
    }

    @Override
    public void close() {
        this.releaseId();
        for (int i = 1; i < frames.size(); i++) {
            try { frames.get(i).close(); } catch (Exception ignored) {}
        }
    }
}