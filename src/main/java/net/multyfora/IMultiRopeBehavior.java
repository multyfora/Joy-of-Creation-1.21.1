package net.multyfora;

import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;

import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface IMultiRopeBehavior {
    void joc$setMaxRopeAttachments(int max);
    boolean joc$canAcceptAnotherRope();
    @Nullable UUID joc$getAttachedRopeID();
    void joc$clearAttachedRopeID();
    void joc$onRopeCreated(UUID ropeID, @Nullable UUID previousRopeID);
    void joc$onRopeDestroyed(UUID ropeID);

    @Nullable Map<UUID, ClientRopeStrand> joc$getAllClientStrands();
    void joc$putClientStrand(UUID uuid, ClientRopeStrand strand);
    void joc$removeClientStrand(UUID uuid);

    @Nullable ClientRopeStrand joc$getOwnedClientStrand();
    void joc$setOwnedClientStrand(@Nullable ClientRopeStrand strand);

    boolean joc$isStrandOwner();
    void joc$setStrandOwner(boolean owned);
}