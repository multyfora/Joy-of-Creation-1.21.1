package net.multyfora.mixin.physics_staff;

import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(PhysicsStaffServerHandler.class)
public interface PhysicsStaffServerHandlerMixin {
    @Accessor("draggingSessions")
    Map<UUID, ?> joc$getDraggingSessions();
}
