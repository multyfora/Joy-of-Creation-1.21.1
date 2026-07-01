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
    private BlockPos location = null, target = null;
    private SubLevel subLevel = null;

    // Smoothed angle for client-side rendering of the pointer
    public final LerpedFloat lerpedPitchDegrees = LerpedFloat.angular();
    public final LerpedFloat lerpedYawDegrees = LerpedFloat.angular();

    public void tick(CoordNavBlockEntity parent) {
        calculateRelativeAngle(parent);
    }

    /**
     * Computes the angle (degrees) that, when applied as a YP rotation after the
     * per-facing reorientation in the renderer, points the spyglass toward the
     * projected target direction.
     * <p>
     * Derivation sketch (all cases verified with rotation-matrix algebra):
     *   proj = normalise( project(targetDir, facingNormal) )
     *   FACING=UP/DOWN : atan2( proj.x,  proj.z)   — sweep in XZ, no reorientation
     *   FACING=NORTH   : atan2( proj.x, -proj.y)   — reorient XP90, tip starts at −Y
     *   FACING=SOUTH   : atan2( proj.x,  proj.y)   — reorient XP−90, tip starts at +Y
     *   FACING=EAST    : atan2( proj.y,  proj.z)   — reorient ZP90,  tip starts at +Y in YZ
     *   FACING=WEST    : atan2(-proj.y,  proj.z)   — reorient ZP−90, tip starts at −Y in YZ
     */
    public void calculateRelativeAngle(CoordNavBlockEntity parent) {
        if(target == null) {
            return;
        }

        Direction facing = parent.getBlockState().getValue(CoordNavBlock.FACING);
        Vec3 targetWorld = parent.getTargetPosition(true);
        Vec3 location = SpaceUtils.getProjectedSelfPos(
            parent.getSubLevel(),
            parent.getWorldPosition()
        );
        Vec3 difference = targetWorld.subtract(location);

        Vec3 diff = SpaceUtils
            .rotateQuat(
                difference,
                SpaceUtils.getSublevelRot( parent.getSubLevel() )
            )
            .add(
                new Vec3(0.5, 0.5, .5)
            )
        ;

        // Planar component (for yaw)
        Vec3 proj = NavigationTarget.getPlaneProjectedPos( diff, facing.getNormal() );
        double planarLen = proj.length();

        // Tilt: angle between planar projection and full vector
        double fullLen = diff.length();
        if (fullLen < 1e-6) {
            pitch = 0;
            return;
        }

        Vec3 facingNormal = Vec3.atLowerCornerOf( facing.getNormal() );
        double outComponent = diff.dot(facingNormal);
        pitch = (float)Math.toDegrees( Math.atan2(-outComponent, planarLen) );

        // Yaw: angle in the sweep plane
        if (planarLen < 1e-6) {
            return;
        }
        Vec3 projNorm = proj.scale(1.0 / planarLen);

        double radians = switch(facing) {
            case UP    -> Math.atan2( projNorm.x,  projNorm.z);
            case DOWN  -> Math.atan2( projNorm.x, -projNorm.z);
            case NORTH -> Math.atan2( projNorm.x,  projNorm.y);
            case SOUTH -> Math.atan2( projNorm.x, -projNorm.y);
            case EAST  -> Math.atan2(-projNorm.y,  projNorm.z);
            case WEST  -> Math.atan2( projNorm.y,  projNorm.z);
        };

        yaw = (float)Math.toDegrees(radians);
    }

    public void setSubLevel(SubLevel subLevel) {
        this.subLevel = subLevel;
    }

    public void setLocation(BlockPos position) {
        this.location = position;
    }
    public void setLocation(Vec3 position) {
        this.location = new BlockPos(
            (int)position.x,
            (int)position.y,
            (int)position.z
        );
    }

    public void setTarget(BlockPos position) {
        this.target = position;
    }
    public void setTarget(Vec3 position) {
        this.target = new BlockPos(
            (int)position.x,
            (int)position.y,
            (int)position.z
        );
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
