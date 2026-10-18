package net.multyfora.content;

import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class SpaceUtils {
    public static Vec3 getProjectedSelfPos(SubLevel subLevel, BlockPos worldPosition) {
        Vec3 pos = Vec3.atCenterOf(worldPosition);
        if (subLevel != null) {
            try {
                pos = subLevel.logicalPose().transformPosition(pos);
            } catch (Exception ignored) {}
        }
        return pos;
    }

    public static Vec3 rotateQuat(Vec3 vec, Quaterniond quat) {
        if (quat.equals(new Quaterniond())) return vec;
        Vector3d v = new Vector3d(vec.x, vec.y, vec.z);
        quat.transform(v);
        return new Vec3(v.x, v.y, v.z);
    }

    public static Quaterniond getSublevelRot(SubLevel subLevel) {
        Quaterniond q = new Quaterniond();
        if (subLevel != null) {
            try {
                q = subLevel.logicalPose().orientation();
            } catch (Exception ignored) {}
        }
        return q;
    }
}
