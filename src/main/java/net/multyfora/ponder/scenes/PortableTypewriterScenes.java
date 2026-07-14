package net.multyfora.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import com.simibubi.create.AllBlocks;

public class PortableTypewriterScenes {
    public static void basic(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("portable_typewriter", "Using the Portable Typewriter");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos typewriterPos = util.grid().at(2, 1, 2);
        BlockPos redstoneLinkPos = util.grid().at(3, 1, 2);

        builder.world().setBlock(redstoneLinkPos,
                AllBlocks.REDSTONE_LINK.getDefaultState(), false);
        builder.world().showSection(util.select().position(redstoneLinkPos), Direction.UP);
        builder.world().showSection(util.select().position(typewriterPos), Direction.DOWN);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("The Portable Typewriter binds keyboard keys to Redstone Link frequencies for wireless control.")
                .pointAt(util.vector().topOf(typewriterPos))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.overlay().showControls(
                util.vector().blockSurface(redstoneLinkPos, Direction.UP),
                net.createmod.catnip.math.Pointing.DOWN, 60)
                .rightClick();
        builder.idle(10);

        builder.overlay().showText(60)
                .text("Right-click a Redstone Link while a key is selected to bind it. Press the key to activate the link.")
                .pointAt(util.vector().topOf(redstoneLinkPos))
                .placeNearTarget();
        builder.idle(70);

        builder.markAsFinished();
    }
}
