package net.multyfora.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class GyroscopicSeatScenes {
    public static void basic(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("gyroscopic_seat", "Using the Gyroscopic Seat");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos seatPos = util.grid().at(2, 1, 2);
        builder.world().showSection(util.select().position(seatPos), Direction.DOWN);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("The Gyroscopic Seat prevents sublevel rotation for the rider, keeping your view stable.")
                .pointAt(util.vector().topOf(seatPos))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.overlay().showControls(util.vector().topOf(seatPos),
                net.createmod.catnip.math.Pointing.DOWN, 60)
                .rightClick();
        builder.idle(10);

        builder.overlay().showText(60)
                .text("Right-click to sit. Your view remains stable even when the ship rotates around you.")
                .pointAt(util.vector().topOf(seatPos))
                .placeNearTarget();
        builder.idle(70);

        builder.markAsFinished();
    }
}
