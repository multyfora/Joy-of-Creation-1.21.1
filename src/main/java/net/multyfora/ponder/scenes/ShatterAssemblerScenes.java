package net.multyfora.ponder.scenes;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.multyfora.content.shatter_assembler.ShatterAssemblerBlockEntity;
import net.multyfora.index.JocItems;

import java.util.ArrayList;
import java.util.List;

public class ShatterAssemblerScenes {
    private static void flickLever(SceneBuilder builder, BlockPos assembler, boolean flicked) {
        builder.world().modifyBlockEntity(assembler, ShatterAssemblerBlockEntity.class, be -> be.clientFlickLeverTo(flicked));
    }

    private static void collapse(SceneBuilder builder, SceneBuildingUtil util, List<BlockPos> blocks) {
        for (int layer = 0; layer <= 4; layer++) {
            int y = 2 + layer;
            for (BlockPos pos : blocks) {
                if (pos.getY() != y)
                    continue;
                ElementLink<WorldSectionElement> piece = builder.world().makeSectionIndependent(util.select().position(pos));
                builder.world().configureCenterOfRotation(piece, Vec3.atCenterOf(pos));
                int seed = pos.getX() * 31 + pos.getY() * 17 + pos.getZ() * 7;
                double driftX = (seed % 5 - 2) * 0.05, driftZ = (seed % 7 - 3) * 0.04;
                builder.world().moveSection(piece, util.vector().of(driftX, -1.05 - layer * 0.2, driftZ), 12);
                builder.world().rotateSection(piece, (seed % 5 - 2) * 3, seed % 9 - 4, (seed % 3 - 1) * 4, 12);
            }
            builder.idle(3);
        }
    }

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("shatter_assembler_intro", "Breaking Structures Apart with the Shatter Assembler");
        builder.configureBasePlate(0, 0, 7);
        builder.showBasePlate();
        builder.idle(10);

        Selection legs = util.select().position(1, 1, 2).add(util.select().position(5, 1, 2))
                .add(util.select().position(1, 1, 4)).add(util.select().position(5, 1, 4));
        Selection structure = util.select().fromTo(1, 2, 2, 5, 4, 4)
                .add(util.select().fromTo(2, 5, 3, 4, 5, 3))
                .add(util.select().position(3, 6, 3));
        builder.world().showSection(legs, Direction.DOWN);
        builder.idle(5);
        builder.world().showSection(structure, Direction.DOWN);
        builder.idle(20);

        BlockPos assembler = util.grid().at(2, 5, 2);
        Vec3 top = util.vector().topOf(assembler);
        builder.world().showSection(util.select().position(assembler), Direction.DOWN);
        builder.overlay().showControls(top, Pointing.DOWN, 40)
                .rightClick()
                .withItem(JocItems.SHATTER_ASSEMBLER.get().getDefaultInstance());
        builder.overlay().showText(70)
                .text("Place a Shatter Assembler onto any block of a Structure")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        builder.overlay().showControls(top, Pointing.DOWN, 50).rightClick();
        builder.overlay().showText(70)
                .text("Hold Right-Click, pull the lever all the way, then let go")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(30);
        flickLever(builder, assembler, true);
        var dust = builder.effects().particleEmitterWithinBlockSpace(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()), util.vector().of(0, 0.08, 0));
        builder.effects().emitParticles(top.add(0, -0.9, 0), dust, 3, 40);
        builder.idle(45);

        builder.overlay().showText(80)
                .text("Every connected Solid Block breaks away as its own Simulated Contraption")
                .independent(10)
                .attachKeyFrame();
        builder.idle(5);

        List<BlockPos> blocks = new ArrayList<>();
        structure.forEach(pos -> blocks.add(pos.immutable()));
        collapse(builder, util, blocks);
        builder.idle(70);

        builder.overlay().showText(80)
                .text("The ground counts as a Structure too, so keep it from touching Solid terrain")
                .colored(PonderPalette.RED)
                .independent(10)
                .attachKeyFrame();
        builder.idle(90);

        builder.markAsFinished();
    }

    public static void solid(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("shatter_assembler_solid", "What the Shatter Assembler Connects");
        builder.configureBasePlate(0, 0, 7);
        builder.showBasePlate();
        builder.idle(10);

        builder.world().showSection(util.select().fromTo(1, 1, 3, 5, 4, 3), Direction.DOWN);
        builder.idle(10);
        builder.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        builder.idle(20);

        Selection solid = util.select().fromTo(1, 1, 3, 5, 1, 3).add(util.select().fromTo(3, 2, 3, 3, 3, 3));
        builder.overlay().showOutlineWithText(solid, 80)
                .text("The Assembler spreads from its Block through Solid Blocks that touch face to face")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(3, 2, 3))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        Selection skipped = util.select().position(1, 2, 3).add(util.select().position(5, 2, 3));
        builder.overlay().showOutlineWithText(skipped, 80)
                .text("Blocks that aren't full and solid, such as Lanterns and Fences, are left out")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(5, 2, 3))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.overlay().showOutlineWithText(util.select().position(1, 1, 1), 70)
                .text("So are Blocks that don't touch the rest")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(1, 1, 1))
                .placeNearTarget();
        builder.idle(80);

        builder.markAsFinished();
    }
}
