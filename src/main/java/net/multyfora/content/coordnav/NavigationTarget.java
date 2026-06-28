package net.multyfora.content.coordnav;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

// Based on dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget
public class NavigationTarget {
    static final float DEADZONE = 2.0F;
    static final float MAX_RANGE = 200.0F;
    static final float MODULATING_RANGE = 200.0F;

    GlobalPos location; //TODO: use in CoordNavBlockEntity

    public NavigationTarget(GlobalPos location) {
        this.location = location;
    }

    @Nullable BlockPos getTarget(ResourceKey<Level> dimensionFrom) {
        if( dimensionFrom != location.dimension() ) {
            return null;
        }
        return location.pos();
    }

    int getRedstoneStrength(CoordNavBlockEntity navBE, Direction direction) {
        return this.calculateSideStrength(navBE, direction);
    }

    int calculateModulatingStrength(CoordNavBlockEntity navBE) {
        Vec3 currentTarget = navBE.getTargetPosition(false);
        if(currentTarget == null) {
            return 0;
        } else {
            Vec3 target = navBE.getTargetPosition(true);
            Vec3 navPos = navBE.getProjectedSelfPos();
            double distance = target.distanceTo(navPos);
            return (int)Math.round(
                ( (double)MODULATING_RANGE - distance )  *  (double)(15.0F/MODULATING_RANGE)
            );
        }
    }

    int calculateSideStrength(CoordNavBlockEntity navBE, Direction direction) {
        if( navBE.getLevel() == null || navBE.getLevel().dimension() != this.location.dimension() ) {
            return 0;
        }

        Vec3 currentTarget = navBE.getTargetPosition(false);
        if (currentTarget == null) {
            return 0;
        } else {
            Direction facing = (Direction)navBE.getBlockState().getValue(NavTableBlock.FACING);
            Vec3i normal = facing.getNormal();
            Vec3 projectedTarget = navBE.getTargetPosition(true);
            Vec3 navPos = navBE.getProjectedSelfPos();
            Vec3 differenceVec = projectedTarget.subtract(navPos);
            Quaterniond worldshellRot = navBE.getSublevelRot();
            differenceVec = SimMathUtils.rotateQuat(differenceVec, worldshellRot);
            Vec3 projectedPos = getPlaneProjectedPos(differenceVec, normal);
            double distance = projectedPos.length();
            if(/*0.0F < MAX_RANGE &&*/ (double)MAX_RANGE - 1.0E-4 < distance) {
                return 0;
            } else if (distance < (double)DEADZONE - 1.0E-4) {
                return 0;
            } else {
                double dot = -projectedPos.dot(Vec3.atLowerCornerOf(direction.getNormal())) / distance;
                return (int)(Math.asin(dot) / Math.PI * (double)30.0F + (double)0.5F);
            }
        }

    }

    private Double distanceToTarget(CoordNavBlockEntity navBE) {
        if( navBE.getLevel() == null || navBE.getLevel().dimension() != this.location.dimension() ) {
            return (double)-1.0F;
        }

        Vec3 targetPosition = navBE.getTargetPosition(true);
        if(targetPosition != null) {
            return navBE.getProjectedSelfPos().distanceTo(targetPosition);
        }
        return (double)-1.0F;
    }

    static Vec3 getPlaneProjectedPos(Vec3 targetPos, Vec3i normal) {
        double dot = targetPos.dot(Vec3.atLowerCornerOf(normal));
        return targetPos.subtract(Vec3.atLowerCornerOf(normal).scale(dot));
    }
}
