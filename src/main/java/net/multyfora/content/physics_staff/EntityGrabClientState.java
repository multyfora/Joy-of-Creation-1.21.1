package net.multyfora.content.physics_staff;

/**
 * Client-side only: tracks which entity is currently being grabbed by the staff.
 * Set by S→C packet handler, read by the client-side beam mixin.
 */
public class EntityGrabClientState {
    public static int grabbedEntityId = 0;
    public static double holdDistance = 5.0;
}
