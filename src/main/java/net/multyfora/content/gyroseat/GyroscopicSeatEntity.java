package net.multyfora.content.gyroseat;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import net.multyfora.index.JocEntityTypes;

public class GyroscopicSeatEntity extends SeatEntity {

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
}
