package net.multyfora.content.shears_cut;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.multyfora.network.ShearsCutPayloads;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShearsCutRemoteState {

    public static final long PREVIEW_TTL_MS = 10_000;
    public static final long FLASH_DURATION_MS = 800;

    public record RemotePreview(BlockPos point1, BlockPos point2, Direction orientation, long lastSeenAt) {}

    public record RemoteFlash(BlockPos point1, BlockPos point2, Direction orientation, long startMs) {}

    private static final Map<UUID, RemotePreview> PREVIEWS = new ConcurrentHashMap<>();
    private static final Map<UUID, RemoteFlash> FLASHES = new ConcurrentHashMap<>();

    private ShearsCutRemoteState() {}

    public static void handle(ShearsCutPayloads.ShearsPreviewPayload payload) {
        UUID actor = payload.actorId();
        switch (payload.phase()) {
            case PLACING -> PREVIEWS.put(actor, new RemotePreview(
                    payload.point1(), payload.point2(), payload.orientation(), System.currentTimeMillis()));
            case DONE -> {
                PREVIEWS.remove(actor);
                FLASHES.put(actor, new RemoteFlash(
                        payload.point1(), payload.point2(), payload.orientation(), System.currentTimeMillis()));
            }
            case CANCEL -> {
                PREVIEWS.remove(actor);
                FLASHES.remove(actor);
            }
        }
    }

    public static Map<UUID, RemotePreview> getPreviews() {
        return Map.copyOf(PREVIEWS);
    }

    public static Map<UUID, RemoteFlash> getFlashes() {
        return Map.copyOf(FLASHES);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        PREVIEWS.entrySet().removeIf(e -> now - e.getValue().lastSeenAt() > PREVIEW_TTL_MS);
        FLASHES.entrySet().removeIf(e -> now - e.getValue().startMs() > FLASH_DURATION_MS);
    }
}
