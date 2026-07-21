package net.multyfora.content.shears_cut;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.multyfora.network.ShearsCutPayloads;
import net.neoforged.neoforge.network.PacketDistributor;

public class ShearsCutState {
    public enum Mode { IDLE, PLACING }
    private static Mode mode = Mode.IDLE;
    private static BlockPos point1;
    private static Direction orientation;

    public static final long FLASH_DURATION_MS = 800;
    private static long flashStartMs = -1;
    private static Vec3 flashWorldCenter, flashWorldU, flashWorldV;
    private static double flashHu, flashHv;

    public record PlaneGeometry(Vec3 center, Vec3 u, Vec3 v, double hu, double hv) {}

    public static void startCut(BlockPos pos, Direction face) {
        point1 = pos;
        orientation = face;
        mode = Mode.PLACING;
    }

    public static void finishCut(BlockPos point2) {
        if (point1 == null || point2 == null || orientation == null) {
            reset();
            return;
        }

        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) {
            PlaneGeometry geom = computeGeometry(point1, point2, orientation);
            Vec3 localCenter = geom.center().add(Vec3.atLowerCornerOf(orientation.getNormal()).scale(0.01));
            Vec3 worldCenter = Sable.HELPER.projectOutOfSubLevel(mc.level, localCenter);
            Vec3 worldU = Sable.HELPER.projectOutOfSubLevel(mc.level, localCenter.add(geom.u())).subtract(worldCenter);
            Vec3 worldV = Sable.HELPER.projectOutOfSubLevel(mc.level, localCenter.add(geom.v())).subtract(worldCenter);
            flashWorldCenter = worldCenter;
            flashWorldU = worldU;
            flashWorldV = worldV;
            flashHu = geom.hu();
            flashHv = geom.hv();
            flashStartMs = System.currentTimeMillis();
        }

        PacketDistributor.sendToServer(new ShearsCutPayloads.ShearsCutPayload(point1, point2, orientation));
        reset();
    }

    public static void reset() {
        point1 = null;
        orientation = null;
        mode = Mode.IDLE;
    }

    public static Mode getMode() { return mode; }
    public static BlockPos getPoint1() { return point1; }
    public static Direction getOrientation() { return orientation; }

    /** The real coordinate of a block's face along its perpendicular axis.
     *  UP/SOUTH/EAST faces sit at pos+1 on their axis; DOWN/NORTH/WEST sit at pos. */
    public static double facePlaneCoord(BlockPos pos, Direction face) {
        return switch (face) {
            case UP -> pos.getY() + 1;
            case DOWN -> pos.getY();
            case NORTH -> pos.getZ();
            case SOUTH -> pos.getZ() + 1;
            case WEST -> pos.getX();
            case EAST -> pos.getX() + 1;
            default -> pos.getY();
        };
    }

    public static Vec3 axisU(Direction face) {
        return switch (face) {
            case EAST, WEST -> new Vec3(0, 0, 1);
            default -> new Vec3(1, 0, 0);
        };
    }

    public static Vec3 axisV(Direction face) {
        return switch (face) {
            case UP, DOWN -> new Vec3(0, 0, 1);
            default -> new Vec3(0, 1, 0);
        };
    }

    /** Origin point of the cutting plane, in sub-level-local space. */
    public static Vec3 planeOrigin(BlockPos p1, Direction face) {
        double plane = facePlaneCoord(p1, face);
        return switch (face) {
            case UP, DOWN -> new Vec3(p1.getX(), plane, p1.getZ());
            case NORTH, SOUTH -> new Vec3(p1.getX(), p1.getY(), plane);
            default -> new Vec3(plane, p1.getY(), p1.getZ());
        };
    }

    /** Computes the plane's center + in-plane basis + half-extents (with margin)
     *  in sub-level-local space, from the two selected corners and the face orientation.
     *  Shared by the renderer (live preview) and finishCut (flash geometry). */
    public static PlaneGeometry computeGeometry(BlockPos p1, BlockPos p2, Direction orientation) {
        Vec3 u = axisU(orientation);
        Vec3 v = axisV(orientation);
        Vec3 origin = planeOrigin(p1, orientation);

        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX()) + 1;
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY()) + 1;

        Vec3 center;
        double hu, hv;
        switch (orientation) {
            case UP, DOWN -> {
                center = new Vec3((minX + maxX) / 2.0, origin.y, (minZ + maxZ) / 2.0);
                hu = (maxX - minX) / 2.0; hv = (maxZ - minZ) / 2.0;
            }
            case NORTH, SOUTH -> {
                center = new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, origin.z);
                hu = (maxX - minX) / 2.0; hv = (maxY - minY) / 2.0;
            }
            default -> {
                center = new Vec3(origin.x, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
                hu = (maxZ - minZ) / 2.0; hv = (maxY - minY) / 2.0;
            }
        }
        double margin = 0.15;
        return new PlaneGeometry(center, u, v, hu + margin, hv + margin);
    }

    public static float getFlashProgress() {
        if (flashStartMs < 0) return -1f;
        long elapsed = System.currentTimeMillis() - flashStartMs;
        if (elapsed > FLASH_DURATION_MS) {
            flashStartMs = -1;
            return -1f;
        }
        return elapsed / (float) FLASH_DURATION_MS;
    }

    public static Vec3 getFlashWorldCenter() { return flashWorldCenter; }
    public static Vec3 getFlashWorldU() { return flashWorldU; }
    public static Vec3 getFlashWorldV() { return flashWorldV; }
    public static double getFlashHu() { return flashHu; }
    public static double getFlashHv() { return flashHv; }

    public static BlockPos getCursorPos() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() != HitResult.Type.MISS) {
            return bhr.getBlockPos();
        }
        return extendToPlane(mc);
    }

    /** When nothing is aimed at, project the player's look ray onto the already-placed
     *  cutting plane so the selection can still extend to the crosshair. Works entirely
     *  via the plane's own basis vectors so we never need an inverse local<->world
     *  transform — only the forward projectOutOfSubLevel that's already known-good. */
    private static BlockPos extendToPlane(net.minecraft.client.Minecraft mc) {
        if (point1 == null || orientation == null || mc.level == null || mc.player == null) return null;

        Vec3 u = axisU(orientation);
        Vec3 v = axisV(orientation);
        Vec3 origin = planeOrigin(point1, orientation);

        Vec3 worldOrigin = Sable.HELPER.projectOutOfSubLevel(mc.level, origin);
        Vec3 worldU = Sable.HELPER.projectOutOfSubLevel(mc.level, origin.add(u)).subtract(worldOrigin);
        Vec3 worldV = Sable.HELPER.projectOutOfSubLevel(mc.level, origin.add(v)).subtract(worldOrigin);
        Vec3 normal = worldU.cross(worldV).normalize();

        Vec3 eye = mc.player.getEyePosition(1.0f);
        Vec3 look = mc.player.getViewVector(1.0f);
        double denom = look.dot(normal);
        if (Math.abs(denom) < 1.0e-6) return null;

        double t = worldOrigin.subtract(eye).dot(normal) / denom;
        if (t <= 0 || t > 512) return null;

        Vec3 worldHit = eye.add(look.scale(t));
        Vec3 rel = worldHit.subtract(worldOrigin);
        double a = rel.dot(worldU) / worldU.dot(worldU);
        double b = rel.dot(worldV) / worldV.dot(worldV);

        Vec3 local = origin.add(u.scale(a)).add(v.scale(b));
        return BlockPos.containing(local.x, local.y, local.z);
    }
}