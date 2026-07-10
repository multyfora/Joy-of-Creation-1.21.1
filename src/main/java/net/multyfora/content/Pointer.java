package net.multyfora.content;

import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.multyfora.content.seeker.SeekerBlockEntity;
import org.joml.Quaterniond;

public class Pointer {
    private float yaw = 0.0f, pitch = 0.0f;
    private SubLevel subLevel = null;

    public final LerpedFloat lerpedPitchDegrees = LerpedFloat.angular();
    public final LerpedFloat lerpedYawDegrees   = LerpedFloat.angular();

    public void tick(SeekerBlockEntity parent) {
        calculateRelativeAngle(parent);
    }

    public void calculateRelativeAngle(SeekerBlockEntity parent) {
        Vec3 rawTarget = parent.getTargetPosition(true);
        if (rawTarget == null) {
            yaw = 0;
            pitch = 0;
            return;
        }

        Vec3 targetWorld = rawTarget.add(new Vec3(0.5, 0.5, 0.5));

        Vec3 selfPos = SpaceUtils.getProjectedSelfPos(
            parent.getSubLevel(),
            parent.getWorldPosition()
        );

        Vec3 diffWorld = targetWorld.subtract(selfPos);

        var forwardRot = SpaceUtils.getSublevelRot(parent.getSubLevel());
        var inverseRot = new Quaterniond(forwardRot).conjugate();
        Vec3 diff = SpaceUtils.rotateQuat(diffWorld, inverseRot);

        if (!parent.isUse3D()) {
            Vec3 diffXZ = new Vec3(diff.x, 0, diff.z);
            double len = diffXZ.length();
            if (len < 1e-6) {
                pitch = 0;
                return;
            }
            Vec3 n = diffXZ.scale(1.0 / len);
            yaw = (float) Math.toDegrees(Math.atan2(n.x, n.z));
            pitch = 0;
            return;
        }

        double fullLen = diff.length();
        if (fullLen < 1e-6) {
            pitch = 0;
            return;
        }

        pitch = (float) Math.toDegrees(Math.atan2(-diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
        yaw = (float) Math.toDegrees(Math.atan2(diff.x, diff.z));
    }

    public void setYaw(float yaw)     {
        this.yaw   = yaw;
    }
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw()   {
        return yaw;
    }
    public float getPitch() {
        return pitch;
    }
}
