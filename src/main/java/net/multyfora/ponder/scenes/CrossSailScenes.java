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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlocks;

public class CrossSailScenes {
    private static final ResourceLocation SYMMETRIC_SAIL = ResourceLocation.fromNamespaceAndPath("simulated", "white_symmetric_sail");

    public static void placing(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("symmetric_cross_sail_placing", "Building with Symmetric Cross Sails");
        builder.configureBasePlate(0, 0, 7);
        builder.showBasePlate();
        builder.idle(10);
        builder.world().showSection(util.select().fromTo(2, 1, 3, 4, 1, 3), Direction.DOWN);
        builder.idle(15);

        var sailItem = JocBlocks.SYMMETRIC_CROSS_SAIL_ITEMS.get(DyeColor.WHITE).get().getDefaultInstance();
        Vec3 first = util.vector().topOf(3, 1, 3);
        builder.overlay().showControls(first, Pointing.DOWN, 40).rightClick().withItem(sailItem);
        builder.idle(10);
        builder.world().showSection(util.select().position(3, 2, 3), Direction.DOWN);
        builder.overlay().showText(70)
                .text("Symmetric Cross Sails are two Sails crossed along one Axis, set by the direction you face")
                .pointAt(util.vector().centerOf(3, 2, 3))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        Vec3 upper = util.vector().topOf(3, 2, 3);
        builder.overlay().showControls(upper, Pointing.DOWN, 40).rightClick().withItem(sailItem);
        builder.overlay().showText(70)
                .text("Right-Click a Cross Sail with another to extend it along its Axis")
                .pointAt(upper)
                .placeNearTarget();
        builder.idle(15);
        builder.world().showSection(util.select().position(3, 3, 3), Direction.DOWN);
        builder.idle(10);
        builder.world().showSection(util.select().position(3, 4, 3), Direction.DOWN);
        builder.idle(65);

        builder.world().showSection(util.select().fromTo(2, 2, 3, 2, 4, 3), Direction.EAST);
        builder.world().showSection(util.select().fromTo(4, 2, 3, 4, 4, 3), Direction.WEST);
        builder.idle(20);

        Vec3 side = util.vector().blockSurface(util.grid().at(4, 3, 3), Direction.EAST);
        builder.overlay().showControls(side, Pointing.RIGHT, 40)
                .rightClick()
                .withItem(BuiltInRegistries.ITEM.get(SYMMETRIC_SAIL).getDefaultInstance());
        builder.idle(5);
        builder.overlay().showText(70)
                .text("Right-Clicking with a regular Sail attaches it to the side, lined up with the Cross Sail")
                .pointAt(side)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(10);
        builder.world().showSection(util.select().fromTo(5, 2, 3, 5, 4, 3), Direction.WEST);
        builder.idle(70);

        BlockState red = JocBlocks.SYMMETRIC_CROSS_SAILS.get(DyeColor.RED).get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockPos dyed = util.grid().at(3, 3, 3);
        Vec3 dyeAt = util.vector().blockSurface(dyed, Direction.NORTH);
        builder.overlay().showControls(dyeAt, Pointing.LEFT, 60).rightClick().withItem(Items.RED_DYE.getDefaultInstance());
        builder.overlay().showText(80)
                .text("Right-Click with Dye to paint it, and keep clicking to spread to the Cross Sails beside it")
                .pointAt(dyeAt)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(20);
        builder.world().setBlock(dyed, red, false);
        builder.idle(15);
        builder.world().setBlock(util.grid().at(2, 3, 3), red, false);
        builder.idle(15);
        builder.world().setBlock(util.grid().at(4, 3, 3), red, false);
        builder.idle(50);

        builder.markAsFinished();
    }

    public static void drag(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("symmetric_cross_sail_drag", "Steering Contraptions with Cross Sail Fins");
        builder.configureBasePlate(0, 0, 9);
        builder.scaleSceneView(0.7f);
        builder.setSceneOffsetY(-2f);
        builder.showBasePlate();
        builder.idle(10);

        Selection all = util.select().fromTo(1, 1, 2, 7, 7, 6);
        ElementLink<WorldSectionElement> ship = builder.world().showIndependentSection(all, Direction.DOWN);
        builder.idle(5);
        builder.world().moveSection(ship, util.vector().of(0, 1, 0), 1);
        builder.idle(20);

        Vec3 fin = util.vector().of(4.5, 2.5, 2);
        builder.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "fin", new AABB(2, 2, 2, 7, 3, 3), 70);
        builder.overlay().showText(70)
                .text("Cross Sails produce no Lift, only Drag")
                .colored(PonderPalette.OUTPUT)
                .pointAt(fin)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        builder.overlay().showText(70)
                .text("Moving along their Axis, the Sails slip through the Air with no Drag at all")
                .colored(PonderPalette.GREEN)
                .independent(10)
                .attachKeyFrame();
        builder.idle(10);
        builder.overlay().showBigLine(PonderPalette.GREEN, util.vector().of(0.5, 1.1, 1.5), util.vector().of(8.5, 1.1, 1.5), 60);
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(1.5, 0, 0), 28);
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(-1.5, 0, 0), 28);
        builder.idle(20);

        builder.overlay().showText(70)
                .text("Moving across it, both faces catch the Air and Drag pushes back")
                .colored(PonderPalette.RED)
                .independent(10)
                .attachKeyFrame();
        builder.idle(10);
        builder.overlay().showBigLine(PonderPalette.RED, util.vector().of(4.5, 1.1, 8.5), util.vector().of(4.5, 1.1, 6.5), 40);
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(0, 0, 0.8), 14);
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(0, 0, -0.8), 22);
        builder.idle(25);

        builder.overlay().showText(90)
                .text("A fin along the Hull lets a Contraption fly straight, while resisting drift sideways and vertically")
                .independent(10)
                .attachKeyFrame();
        builder.idle(100);

        builder.markAsFinished();
    }
}
