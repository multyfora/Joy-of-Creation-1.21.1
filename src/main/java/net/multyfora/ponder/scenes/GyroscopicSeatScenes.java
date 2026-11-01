package net.multyfora.ponder.scenes;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import net.multyfora.content.gyroseat.GyroscopicSeatBlockEntity;
import net.multyfora.index.JocItems;

public class GyroscopicSeatScenes {
    private static void roll(SceneBuilder builder, ElementLink<WorldSectionElement> deck, BlockPos seat,
                             double fromX, double fromZ, double toX, double toZ, int ticks) {
        for (int i = 1; i <= ticks; i++) {
            double before = JocSceneUtil.ease((i - 1.0) / ticks), after = JocSceneUtil.ease((double) i / ticks);
            builder.world().rotateSection(deck, (toX - fromX) * (after - before), 0, (toZ - fromZ) * (after - before), 1);
            float x = (float) (fromX + (toX - fromX) * after), z = (float) (fromZ + (toZ - fromZ) * after);
            builder.world().modifyBlockEntity(seat, GyroscopicSeatBlockEntity.class, be -> be.setVirtualTilt(x, z));
            builder.idle(1);
        }
    }

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("gyroscopic_seat", "Riding Steady in the Gyroscopic Seat");
        builder.configureBasePlate(0, 0, 8);
        builder.setSceneOffsetY(-1f);
        builder.showBasePlate();
        builder.idle(10);

        BlockPos master = util.grid().at(3, 2, 3);
        Vec3 center = Vec3.atLowerCornerOf(master).add(1, 1, 1);
        Selection seat = util.select().fromTo(3, 2, 3, 4, 3, 4);
        Selection hull = util.select().fromTo(1, 1, 1, 6, 2, 6).substract(seat);

        ElementLink<WorldSectionElement> deck = builder.world().showIndependentSection(hull, Direction.DOWN);
        builder.world().configureCenterOfRotation(deck, center);
        builder.idle(20);

        Vec3 top = util.vector().topOf(util.grid().at(4, 3, 4));
        builder.overlay().showControls(top, Pointing.DOWN, 40)
                .rightClick()
                .withItem(JocItems.GYROSCOPIC_SEAT.get().getDefaultInstance());
        builder.idle(10);
        builder.world().showSectionAndMerge(seat, Direction.DOWN, deck);
        builder.overlay().showOutlineWithText(seat, 70)
                .text("The Gyroscopic Seat fills a 2×2×2 space on a Contraption")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        builder.overlay().showControls(top, Pointing.DOWN, 40).rightClick();
        builder.overlay().showText(70)
                .text("Right-Click to climb into the middle of it")
                .pointAt(top)
                .placeNearTarget();
        builder.idle(20);
        ElementLink<ParrotElement> rider = builder.special().createBirb(center.add(0, 0.3, 0), ParrotPose.FacePointOfInterestPose::new);
        builder.special().movePointOfInterest(center.add(-5, 0.5, -5));
        builder.idle(60);

        builder.overlay().showText(120)
                .text("As the Contraption rolls and pitches, the rings swing so the Seat stays level")
                .independent(10)
                .attachKeyFrame();
        roll(builder, deck, master, 0, 0, 0, 22, 35);
        builder.idle(10);
        roll(builder, deck, master, 0, 22, 18, -18, 50);
        builder.idle(10);
        roll(builder, deck, master, 18, -18, -16, 12, 50);
        builder.idle(10);

        builder.overlay().showText(80)
                .text("Your view stays steady, however rough the flight")
                .independent(10);
        roll(builder, deck, master, -16, 12, 0, 0, 45);
        builder.idle(50);

        builder.special().hideElement(rider, Direction.UP);
        builder.markAsFinished();
    }
}
