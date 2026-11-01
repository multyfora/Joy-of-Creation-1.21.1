package net.multyfora.ponder.scenes;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;

public class BalloonScenes {
    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("balloon_intro", "Lifting Contraptions with Balloons");
        builder.configureBasePlate(0, 0, 9);
        builder.scaleSceneView(0.7f);
        builder.setSceneOffsetY(-2f);
        builder.showBasePlate();
        builder.idle(10);

        ElementLink<WorldSectionElement> ship = builder.world().showIndependentSection(
                util.select().fromTo(1, 1, 3, 7, 3, 5), Direction.DOWN);
        builder.idle(20);

        Vec3 deck = util.vector().topOf(4, 3, 4);
        builder.overlay().showControls(deck, Pointing.DOWN, 50)
                .rightClick()
                .withItem(JocItems.DEFLATED_BALLOONS.get(DyeColor.RED).get().getDefaultInstance());
        builder.overlay().showText(60)
                .text("Hold Right-Click with a Deflated Balloon to blow it up")
                .pointAt(deck)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(70);

        for (int y = 4; y <= 7; y++) {
            builder.world().showSectionAndMerge(util.select().layer(y), Direction.DOWN, ship);
            builder.idle(6);
        }
        builder.idle(10);

        Vec3 envelope = util.vector().blockSurface(util.grid().at(1, 5, 4), Direction.WEST);
        builder.overlay().showText(70)
                .text("Balloons lift Simulated Contraptions without needing Hot Air")
                .pointAt(envelope)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        builder.overlay().showText(90)
                .text("Each Balloon lifts the weight of 1.5 Stone Blocks, yet weighs only a quarter of one")
                .pointAt(envelope)
                .placeNearTarget();
        builder.idle(40);
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(0, 2.6, 0), 50);
        builder.overlay().showText(110)
                .text("Lift weakens as Air Pressure drops, so a Contraption overshoots, then bobs around the Altitude where Lift equals Weight")
                .independent(10)
                .attachKeyFrame();
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(0, -1.2, 0), 40);
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(0, 0.7, 0), 34);
        JocSceneUtil.smoothMove(builder, ship, util.vector().of(0, -0.3, 0), 30);
        builder.idle(30);

        builder.overlay().showText(70)
                .text("With far too much Lift it will climb past the Atmosphere, where its Balloons pop")
                .colored(PonderPalette.RED)
                .independent(10);
        builder.idle(80);

        builder.overlay().showText(90)
                .text("Each Balloon pulls where it sits, so spread them evenly or the Contraption will tilt")
                .independent(10)
                .attachKeyFrame();
        builder.idle(30);
        Selection right = util.select().fromTo(5, 4, 2, 7, 7, 6);
        builder.world().setBlocks(right, Blocks.AIR.defaultBlockState(), true);
        builder.idle(5);
        builder.world().configureCenterOfRotation(ship, util.vector().centerOf(4, 3, 4));
        builder.world().rotateSection(ship, 0, 0, -14, 40);
        builder.idle(70);

        builder.markAsFinished();
    }

    public static void pop(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("balloon_pop", "Popping and Painting Balloons");
        builder.configureBasePlate(0, 0, 7);
        builder.showBasePlate();
        builder.idle(10);
        builder.world().showSection(util.select().fromTo(1, 1, 2, 5, 2, 4), Direction.DOWN);
        builder.idle(10);
        builder.world().showSection(util.select().fromTo(1, 3, 2, 5, 4, 4), Direction.DOWN);
        builder.idle(20);

        Selection left = util.select().fromTo(1, 3, 2, 3, 4, 4);
        Selection right = util.select().fromTo(4, 3, 2, 5, 4, 4);

        Vec3 top = util.vector().topOf(4, 4, 3);
        builder.overlay().showControls(top, Pointing.DOWN, 50)
                .rightClick()
                .withItem(Items.LIGHT_BLUE_DYE.getDefaultInstance());
        builder.overlay().showText(60)
                .text("Right-Click with Dye to repaint a Balloon")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(30);
        builder.world().setBlocks(right, JocBlocks.BALLOONS.get(DyeColor.LIGHT_BLUE).get().defaultBlockState(), false);
        builder.idle(40);

        builder.overlay().showOutlineWithText(left, 70)
                .text("Touching Balloons of one color count as a single Envelope")
                .colored(PonderPalette.WHITE)
                .pointAt(util.vector().blockSurface(util.grid().at(1, 4, 3), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        Vec3 from = util.vector().of(-3.5, 4.2, 3.5);
        BlockPos hit = util.grid().at(1, 4, 3);
        ElementLink<EntityElement> arrow = builder.world().createEntity(level -> {
            Arrow entity = new Arrow(EntityType.ARROW, level);
            entity.setPos(from);
            entity.setNoPhysics(true);
            entity.setNoGravity(true);
            entity.setDeltaMovement(0.5, 0, 0);
            entity.setYRot(-90);
            entity.yRotO = -90;
            return entity;
        });
        builder.idle(12);
        builder.world().modifyEntity(arrow, entity -> entity.discard());

        ItemStack rubber = JocBlocks.BALLOON_ITEMS.get(DyeColor.WHITE).get().getDefaultInstance();
        var burst = builder.effects().particleEmitterWithinBlockSpace(
                new ItemParticleOption(ParticleTypes.ITEM, rubber), util.vector().of(0, 0.05, 0));
        left.forEach(pos -> builder.effects().emitParticles(util.vector().of(pos.getX(), pos.getY(), pos.getZ()), burst, 3, 2));
        builder.world().setBlocks(left, Blocks.AIR.defaultBlockState(), false);

        builder.overlay().showText(80)
                .text("An Arrow pops every connected Balloon of the same color at once")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(hit))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.overlay().showOutlineWithText(right, 80)
                .text("Other colors survive, so divide large Envelopes to keep some Lift")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(util.grid().at(4, 4, 3), Direction.WEST))
                .placeNearTarget();
        builder.idle(90);

        builder.markAsFinished();
    }
}
