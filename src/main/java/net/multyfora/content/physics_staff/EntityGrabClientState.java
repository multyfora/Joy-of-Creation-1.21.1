package net.multyfora.content.physics_staff;

public class EntityGrabClientState {
    public static volatile int grabbedEntityId = 0;
    public static volatile double holdDistance = 5.0;
    public static volatile net.minecraft.world.phys.Vec3 grabTargetPos = null;
    public static volatile long lastGrabPacketTime = 0L;

    public static void reset() {
        grabbedEntityId = 0;
        holdDistance = 5.0;
        grabTargetPos = null;
        lastGrabPacketTime = 0L;
    }
}
