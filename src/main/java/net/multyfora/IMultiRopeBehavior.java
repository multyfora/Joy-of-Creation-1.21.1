package net.multyfora;

import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface IMultiRopeBehavior {
    void joc$setMaxRopeAttachments(int max);
    int joc$getMaxRopeAttachments();
    boolean joc$canAcceptAnotherRope();
    @Nullable UUID joc$getAttachedRopeID();
    void joc$clearAttachedRopeID();
    void joc$onRopeCreated(UUID ropeID, @Nullable UUID previousRopeID);
    void joc$onRopeDestroyed(UUID ropeID);
    boolean joc$destroyRopeByUUID(UUID ropeID, @Nullable ServerPlayer player, boolean returnItem);
    List<UUID> joc$getAllAttachedRopeIDs();

    /**
     * Registers a rope strand that was created outside the vanilla createRope flow
     * (e.g. by another mode's compat shim) on this holder. The owner end gets the
     * vanilla owner fields plus full JOC bookkeeping; the attached end only adds its
     * rope ID when the holder is JOC-managed, so its own owned strands are preserved.
     **/
    void joc$installExternalRope(UUID ropeID, boolean owner, @Nullable ServerRopeStrand strand);

    /**
     * Undoes joc$installExternalRope (used when the new strand cannot be registered).
     **/
    void joc$removeExternalRope(UUID ropeID, boolean owner, @Nullable ServerRopeStrand strand);

    @Nullable Map<UUID, ClientRopeStrand> joc$getAllClientStrands();
    void joc$putClientStrand(UUID uuid, ClientRopeStrand strand);
    void joc$removeClientStrand(UUID uuid);

    @Nullable ClientRopeStrand joc$getOwnedClientStrand();
    void joc$setOwnedClientStrand(@Nullable ClientRopeStrand strand);

    boolean joc$isStrandOwner();
    void joc$setStrandOwner(boolean owned);
}