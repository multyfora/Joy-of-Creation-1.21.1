package net.multyfora.content.shears_cut;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.multyfora.network.ShearsCutPayloads;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Client-side state for cut previews initiated by other players. */
public final class ShearsCutRemoteState {

    public static final long PREVIEW_TTL_MS = 10_000;
    public static final long FLASH_DURATION_MS = 800;

    public record RemotePreview(BlockPos point1, BlockPos point2, Direction orientation, long lastSeenAt) {}

    public record RemoteFlash(BlockPos point1, BlockPos point2, Direction orientation, long startMs) {}

    private static final Map<UUID, RemotePreview> PREVIEWS = new HashMap<>();
    private static final Map<UUID, RemoteFlash> FLASHES = new HashMap<>();

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
        return PREVIEWS;
    }

    public static Map<UUID, RemoteFlash> getFlashes() {
        return FLASHES;
    }

    /** Removes stale previews/flashes so a dropped cutter (or a lost CANCEL) never lingers forever. */
    public static void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<RemotePreview> it = PREVIEWS.values().iterator(); it.hasNext(); ) {
            if (now - it.next().lastSeenAt() > PREVIEW_TTL_MS) it.remove();
        }
        for (Iterator<RemoteFlash> it = FLASHES.values().iterator(); it.hasNext(); ) {
            if (now - it.next().startMs() > FLASH_DURATION_MS) it.remove();
        }
    }
}
