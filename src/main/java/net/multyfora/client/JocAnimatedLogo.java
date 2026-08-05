package net.multyfora.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JocAnimatedLogo {

    private static final String MOD_ID = "joc";
    private static final int FRAME_COUNT = 28;
    private static final int FRAME_DURATION_TICKS = 2;

    private static List<NativeImage> cachedFrames;

    private static String selectedModId;

    public static void setSelectedModId(String modId) {
        selectedModId = modId;
    }

    public static boolean isJocSelected() {
        return MOD_ID.equals(selectedModId);
    }

    public static void install(TextureManager tm, ResourceLocation targetLocation) {
        try {
            if (cachedFrames == null)
                cachedFrames = loadFrames();

            tm.register(targetLocation, new AnimatedGifTexture(cachedFrames, FRAME_DURATION_TICKS));
        } catch (IOException ignored) {
        }
    }

    private static List<NativeImage> loadFrames() throws IOException {
        List<NativeImage> frames = new ArrayList<>();
        for (int i = 0; i < FRAME_COUNT; i++) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MOD_ID,
                    "textures/gui/logo_frames/frame_" + i + ".png");
            try (InputStream in = Minecraft.getInstance().getResourceManager()
                    .getResourceOrThrow(loc).open()) {
                frames.add(NativeImage.read(in));
            }
        }
        return frames;
    }
}
