package net.multyfora.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class BalloonScenes {
    public static void basic(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("balloon", "Using Balloons");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos balloonPos = util.grid().at(2, 1, 2);
        builder.world().showSection(util.select().position(balloonPos), Direction.DOWN);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("Balloons provide lift when attached to your airship structure.")
                .pointAt(util.vector().topOf(balloonPos))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        BlockPos balloon2 = util.grid().at(2, 1, 3);
        BlockPos balloon3 = util.grid().at(2, 1, 1);
        builder.world().showSection(util.select().position(balloon2), Direction.DOWN);
        builder.world().showSection(util.select().position(balloon3), Direction.DOWN);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("Multiple balloons generate more lift. Distribute them evenly across your structure.")
                .pointAt(util.vector().centerOf(2, 2, 2))
                .placeNearTarget();
        builder.idle(90);

        builder.markAsFinished();
    }
}
