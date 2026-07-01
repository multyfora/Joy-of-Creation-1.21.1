package net.multyfora.content;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.multyfora.content.coordnav.CoordNavBlock;
import net.multyfora.content.coordnav.CoordNavBlockEntity;

public class Pointer {
    private float yaw = 0.0f, pitch = 0.0f;
    private SubLevel subLevel = null;

    public final LerpedFloat lerpedPitchDegrees = LerpedFloat.angular();
    public final LerpedFloat lerpedYawDegrees   = LerpedFloat.angular();

    public void tick(CoordNavBlockEntity parent) {
        calculateRelativeAngle(parent);
    }

    public void calculateRelativeAngle(CoordNavBlockEntity parent) {
        Vec3 targetWorld = parent.getTargetPosition(true);
        if (targetWorld == null) return;

        Direction facing = parent.getBlockState().getValue(CoordNavBlock.FACING);

        Vec3 selfPos = SpaceUtils.getProjectedSelfPos(
                parent.getSubLevel(),
                parent.getWorldPosition()
        );

        // Calculate the difference in WORLD space
        Vec3 diffWorld = targetWorld.subtract(selfPos);

        // Fetch the sublevel's forward rotation
        var forwardRot = SpaceUtils.getSublevelRot(parent.getSubLevel());

        // INVERT the rotation (Going from World -> Local requires the conjugate)
        var inverseRot = new org.joml.Quaterniond(forwardRot).conjugate();

        // Rotate the vector into sublevel-local space using the inverse
        Vec3 diff = SpaceUtils.rotateQuat(diffWorld, inverseRot);

        // Project onto the facing plane to get the yaw component
        Vec3 proj = NavigationTarget.getPlaneProjectedPos(diff, facing.getNormal());
        double planarLen = proj.length();


        double fullLen = diff.length();
        if (fullLen < 1e-6) {
            pitch = 0;
            return;
        }

        Vec3 facingNormal = Vec3.atLowerCornerOf(facing.getNormal());
        double outComponent = diff.dot(facingNormal);
        pitch = (float) Math.toDegrees(Math.atan2(-outComponent, planarLen));

        if (planarLen < 1e-6) return;
        Vec3 projNorm = proj.scale(1.0 / planarLen);

        double radians = switch (facing) {
            case UP    -> Math.atan2( projNorm.x,  projNorm.z);
            case DOWN  -> Math.atan2( projNorm.x, -projNorm.z);
            case NORTH -> Math.atan2( projNorm.x,  projNorm.y);
            case SOUTH -> Math.atan2( projNorm.x, -projNorm.y);
            case EAST  -> Math.atan2(-projNorm.y,  projNorm.z);
            case WEST  -> Math.atan2( projNorm.y,  projNorm.z);
        };

        yaw = (float) Math.toDegrees(radians);
    }

    public void setSubLevel(SubLevel subLevel) { this.subLevel = subLevel; }

    public void setLocation(BlockPos position) {}
    public void setLocation(Vec3 position) {}
    public void setTarget(BlockPos position) {}
    public void setTarget(Vec3 position) {}

    public void setYaw(float yaw)     { this.yaw   = yaw;   }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public float getYaw()   { return yaw;   }
    public float getPitch() { return pitch; }
}