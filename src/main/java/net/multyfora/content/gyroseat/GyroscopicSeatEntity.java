package net.multyfora.content.gyroseat;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocEntityTypes;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class GyroscopicSeatEntity extends SeatEntity {

    public static final double SEAT_HEIGHT = -0.52;
    private static final float SWIVEL_FOLLOW = 0.12F;
    private static final float COMFORTABLE_HEAD_TURN = 50.0F;
    private static final float FAST_SWIVEL_FOLLOW = 0.35F;
    private static final float MAX_HEAD_TURN = 100.0F;

    public GyroscopicSeatEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public GyroscopicSeatEntity(Level level) {
        this(JocEntityTypes.GYROSCOPIC_SEAT.get(), level);
        noPhysics = true;
    }

    public static EntityType.Builder<?> build(EntityType.Builder<?> builder) {
        return builder.sized(0.25f, 0.35f);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger))
            return;
        double eyeHeight = passenger.getEyeHeight();
        Vector3d eyeOffset = new Vector3d(0, SEAT_HEIGHT - passenger.getVehicleAttachmentPoint(this).y + eyeHeight, 0);
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel != null)
            subLevel.logicalPose().orientation().transformInverse(eyeOffset);
        callback.accept(passenger, getX() + eyeOffset.x, getY() + eyeOffset.y - eyeHeight, getZ() + eyeOffset.z);
        if (passenger instanceof LivingEntity living)
            swivel(living);
    }

    private static void swivel(LivingEntity living) {
        float head = living.getYHeadRot();
        float gap = Mth.wrapDegrees(head - living.yBodyRot);
        float body = living.yBodyRot + gap * (Math.abs(gap) > COMFORTABLE_HEAD_TURN ? FAST_SWIVEL_FOLLOW : SWIVEL_FOLLOW);
        float lag = Mth.wrapDegrees(head - body);
        if (Math.abs(lag) > MAX_HEAD_TURN)
            body = head - Math.copySign(MAX_HEAD_TURN, lag);
        living.setYBodyRot(body);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        GyroscopicSeatBlockEntity be = getSeatBlockEntity();
        if (passenger instanceof LivingEntity living && be != null) {
            float yaw = be.getSeatYaw();
            living.setYBodyRot(yaw);
            living.yBodyRotO = yaw;
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        GyroscopicSeatBlockEntity be = getSeatBlockEntity();
        if (be != null)
            be.rememberYaw(passenger instanceof LivingEntity living ? living.yBodyRot : passenger.getYRot());
    }

    @Nullable
    private GyroscopicSeatBlockEntity getSeatBlockEntity() {
        BlockPos master = BlockPos.containing(getX() - 1, getY() - 1, getZ() - 1);
        return level().getBlockEntity(master) instanceof GyroscopicSeatBlockEntity be ? be : null;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos master = BlockPos.containing(getX() - 1, getY() - 1, getZ() - 1);
        Vec3 look = passenger.getLookAngle();
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel != null) {
            Vector3d local = subLevel.logicalPose().orientation().transformInverse(new Vector3d(look.x, look.y, look.z));
            look = new Vec3(local.x, local.y, local.z);
        }
        Direction facing = Direction.getNearest(look.x, 0, look.z);
        Direction[] sides = {facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()};
        for (Direction side : sides) {
            Direction along = side.getClockWise();
            BlockPos outside = master.offset(Math.max(side.getStepX(), 0), 0, Math.max(side.getStepZ(), 0)).relative(side);
            for (int i = 0; i < 2; i++) {
                BlockPos cell = outside.relative(along, i * (along.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1));
                Vec3 spot = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), cell, true);
                if (spot != null)
                    return spot;
            }
        }
        for (int part = 0; part < 4; part++) {
            BlockPos top = master.offset(part & 1, 2, part >> 1);
            Vec3 spot = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), top, true);
            if (spot != null)
                return spot;
        }
        return super.getDismountLocationForPassenger(passenger);
    }
}
