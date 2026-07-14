package net.multyfora.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class CrossSailScenes {
    public static void basic(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("cross_sail", "Using Symmetric Cross Sails");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos sailPos = util.grid().at(2, 1, 2);
        builder.world().showSection(util.select().position(sailPos), Direction.DOWN);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("Cross Sails generate lift and drag for airship movement. They come in 16 colors.")
                .pointAt(util.vector().topOf(sailPos))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        BlockPos sail2 = util.grid().at(2, 1, 3);
        BlockPos sail3 = util.grid().at(2, 1, 1);
        builder.world().showSection(util.select().position(sail2), Direction.DOWN);
        builder.world().showSection(util.select().position(sail3), Direction.DOWN);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("Place them adjacent to form larger sail surfaces. Use dye to recolor connected sails.")
                .pointAt(util.vector().centerOf(2, 1, 2))
                .placeNearTarget();
        builder.idle(90);

        builder.markAsFinished();
    }
}
