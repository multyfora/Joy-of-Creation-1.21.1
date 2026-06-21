package net.multyfora;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for block entities that support multiple simultaneous rope attachments.
 * Implemented via mixin on RopeStrandHolderBehavior to extend the single-rope system.
 * The "joc$" prefix follows Minecraft naming conventions to avoid conflicts with shadowed methods.
 **/
public interface IMultiRopeBehavior {
    // Sets the maximum number of rope attachments this holder can accept
    void joc$setMaxRopeAttachments(int max);
    // Returns true if this holder can accept another rope (below the maximum)
    boolean joc$canAcceptAnotherRope();
    // Returns the UUID of the currently attached rope, or null if none
    @Nullable UUID joc$getAttachedRopeID();
    // Clears the attached rope ID (used when transferring ownership during rope creation)
    void joc$clearAttachedRopeID();
    /**
     * Called when a rope is successfully created to/from this holder; registers the rope ID and
     * optionally the previous rope ID if one was displaced
     **/
    void joc$onRopeCreated(UUID ropeID, @Nullable UUID previousRopeID);
    // Called when a rope attached to this holder is destroyed; removes the rope ID from tracking
    void joc$onRopeDestroyed(UUID ropeID);
}
