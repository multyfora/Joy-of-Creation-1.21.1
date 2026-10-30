package net.multyfora.content.gyroseat;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.multyfora.index.JocBlockEntityTypes;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GyroscopicSeatBlockEntity extends SmartBlockEntity {

    private float restYaw;

    private float clientYaw = Float.NaN;
    @Nullable
    private GyroscopicSeatEntity seat;
    private long lastSeatLookup = Long.MIN_VALUE;

    public GyroscopicSeatBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.GYROSCOPIC_SEAT.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public float getSeatYaw() {
        return Float.isNaN(clientYaw) ? restYaw : clientYaw;
    }

    public void setClientYaw(float yaw) {
        clientYaw = yaw;
    }

    public void rememberYaw(float yaw) {
        restYaw = yaw;
        clientYaw = yaw;
        if (level != null && !level.isClientSide) {
            setChanged();
            sendData();
        }
    }

    @Nullable
    public Entity getRider() {
        if (level == null)
            return null;
        if (seat == null || seat.isRemoved()) {
            seat = null;
            if (level.getGameTime() != lastSeatLookup) {
                lastSeatLookup = level.getGameTime();
                List<GyroscopicSeatEntity> seats = level.getEntitiesOfClass(GyroscopicSeatEntity.class,
                    GyroscopicSeatBlock.getCubeBounds(worldPosition));
                if (!seats.isEmpty())
                    seat = seats.get(0);
            }
        }
        return seat == null ? null : seat.getFirstPassenger();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return GyroscopicSeatBlock.getCubeBounds(worldPosition);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("RestYaw", restYaw);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        restYaw = tag.getFloat("RestYaw");
        if (clientPacket && seat == null)
            clientYaw = Float.NaN;
    }
}
