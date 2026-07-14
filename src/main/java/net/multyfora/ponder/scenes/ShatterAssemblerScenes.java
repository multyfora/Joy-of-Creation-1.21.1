package net.multyfora.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class ShatterAssemblerScenes {
    public static void basic(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("shatter_assembler", "Using the Shatter Assembler");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos assemblerPos = util.grid().at(2, 1, 2);
        builder.world().showSection(util.select().position(assemblerPos), Direction.DOWN);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("The Shatter Assembler disassembles and reassembles ships. It must face a solid block.")
                .pointAt(util.vector().topOf(assemblerPos))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.overlay().showText(60)
                .text("Place it on a wall, floor, or ceiling. Right-click to open the interface and control assembly.")
                .pointAt(util.vector().blockSurface(assemblerPos, Direction.NORTH))
                .placeNearTarget();
        builder.idle(70);

        builder.markAsFinished();
    }
}
