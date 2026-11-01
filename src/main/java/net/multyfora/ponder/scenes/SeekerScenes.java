package net.multyfora.ponder.scenes;

import com.simibubi.create.AllBlocks;

import dev.simulated_team.simulated.index.SimBlocks;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.index.JocItems;

public class SeekerScenes {
    private static void setModule(SceneBuilder builder, SceneBuildingUtil util, BlockPos seeker, String module, BlockPos target) {
        builder.world().modifyBlockEntityNBT(util.select().position(seeker), SeekerBlockEntity.class, tag -> {
            tag.putString("module", module);
            tag.putBoolean("use_3d", true);
            tag.putBoolean("has_target", target != null);
            if (target != null) {
                tag.putDouble("target_x", target.getX());
                tag.putDouble("target_y", target.getY());
                tag.putDouble("target_z", target.getZ());
            }
        }, true);
    }

    private static void turnPointer(SceneBuilder builder, BlockPos seeker, float yaw, float pitch) {
        builder.world().modifyBlockEntity(seeker, SeekerBlockEntity.class, be -> {
            be.spyglassPointer.lerpedYawDegrees.chase(yaw, 0.2, LerpedFloat.Chaser.EXP);
            be.spyglassPointer.lerpedPitchDegrees.chase(pitch, 0.2, LerpedFloat.Chaser.EXP);
        });
    }

    private static void pointAt(SceneBuilder builder, BlockPos seeker, BlockPos target) {
        BlockPos diff = target.subtract(seeker);
        turnPointer(builder, seeker, (float) Math.toDegrees(Math.atan2(diff.getX(), diff.getZ())), 0);
    }

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_intro", "Tracking Targets with the Seeker");
        builder.configureBasePlate(0, 0, 7);
        builder.scaleSceneView(1.3f);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos seeker = util.grid().at(3, 1, 3);
        BlockPos eastWire = seeker.east(), northWire = seeker.north();
        BlockPos eastNixie = seeker.east(2), northNixie = seeker.north(2);
        BlockPos farTarget = util.grid().at(6, 1, 0), nearTarget = util.grid().at(6, 1, 3);

        builder.world().showSection(util.select().position(seeker), Direction.DOWN);
        builder.idle(10);
        builder.world().showSection(util.select().fromTo(1, 1, 3, 5, 1, 3).add(util.select().fromTo(3, 1, 1, 3, 1, 5)), Direction.DOWN);
        builder.idle(15);

        builder.overlay().showText(70)
                .text("The Seeker turns the direction of a Target into Redstone signals")
                .pointAt(util.vector().topOf(seeker))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        builder.world().showSection(util.select().position(farTarget), Direction.DOWN);
        builder.idle(10);
        builder.overlay().showControls(util.vector().topOf(farTarget), Pointing.DOWN, 50)
                .leftClick()
                .withItem(Items.SPYGLASS.getDefaultInstance());
        builder.overlay().showText(60)
                .text("Look through a Spyglass and Left-Click a block to mark it as the Target")
                .pointAt(util.vector().topOf(farTarget))
                .placeNearTarget()
                .attachKeyFrame();
        builder.overlay().showOutline(PonderPalette.GREEN, "target", util.select().position(farTarget), 60);
        builder.idle(70);

        builder.overlay().showControls(util.vector().topOf(seeker), Pointing.DOWN, 40)
                .rightClick()
                .withItem(Items.SPYGLASS.getDefaultInstance());
        builder.overlay().showText(60)
                .text("Then Right-Click the Seeker to slot the Spyglass in")
                .pointAt(util.vector().topOf(seeker))
                .placeNearTarget();
        builder.idle(25);
        setModule(builder, util, seeker, "SPYGLASS", farTarget);
        pointAt(builder, seeker, farTarget);
        builder.effects().indicateSuccess(seeker);
        builder.idle(50);

        JocSceneUtil.setWire(builder, eastWire, 8);
        JocSceneUtil.setWire(builder, northWire, 8);
        JocSceneUtil.setNixie(builder, util, eastNixie, 8);
        JocSceneUtil.setNixie(builder, util, northNixie, 8);
        builder.effects().indicateRedstone(eastWire);
        builder.effects().indicateRedstone(northWire);
        builder.overlay().showText(70)
                .text("The sides turned toward the Target emit a signal")
                .pointAt(util.vector().topOf(eastWire))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        builder.world().hideSection(util.select().position(farTarget), Direction.UP);
        builder.idle(10);
        builder.world().showSection(util.select().position(nearTarget), Direction.DOWN);
        builder.idle(10);
        setModule(builder, util, seeker, "SPYGLASS", nearTarget);
        pointAt(builder, seeker, nearTarget);
        builder.idle(10);
        JocSceneUtil.setWire(builder, eastWire, 15);
        JocSceneUtil.setWire(builder, northWire, 0);
        JocSceneUtil.setNixie(builder, util, eastNixie, 15);
        JocSceneUtil.setNixie(builder, util, northNixie, 0);
        builder.effects().indicateRedstone(eastWire);
        builder.overlay().showText(70)
                .text("The more directly a side faces the Target, the stronger its signal")
                .pointAt(util.vector().topOf(eastNixie))
                .placeNearTarget();
        builder.idle(80);

        builder.overlay().showControls(util.vector().topOf(seeker), Pointing.DOWN, 50).rightClick();
        builder.overlay().showText(70)
                .text("Right-Click with an empty hand to type coordinates, pick a Waypoint or switch between 3D and flat 2D tracking")
                .pointAt(util.vector().topOf(seeker))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(80);

        builder.overlay().showControls(util.vector().topOf(seeker), Pointing.DOWN, 50).rightClick().whileSneaking();
        builder.overlay().showText(60)
                .text("Sneak and Right-Click to take the Spyglass back out")
                .pointAt(util.vector().topOf(seeker))
                .placeNearTarget();
        builder.idle(70);

        builder.markAsFinished();
    }

    public static void eye(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_eye", "Reading the Player's Gaze with the Seeker");
        builder.configureBasePlate(0, 0, 7);
        builder.scaleSceneView(1.3f);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos seeker = util.grid().at(3, 1, 3);
        BlockPos eastWire = seeker.east(), southWire = seeker.south(), northWire = seeker.north(), westWire = seeker.west();
        BlockPos eastNixie = seeker.east(2), southNixie = seeker.south(2), northNixie = seeker.north(2), westNixie = seeker.west(2);

        builder.world().showSection(util.select().fromTo(1, 1, 1, 5, 1, 5), Direction.DOWN);
        builder.idle(20);

        Vec3 top = util.vector().topOf(seeker);
        builder.overlay().showControls(top, Pointing.DOWN, 40).rightClick().withItem(Items.ENDER_EYE.getDefaultInstance());
        builder.overlay().showText(60)
                .text("An Eye of Ender makes the Seeker follow a Player's gaze instead of a Target")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(25);
        setModule(builder, util, seeker, "PLAYER_DIR", null);
        builder.effects().indicateSuccess(seeker);
        builder.idle(45);

        builder.overlay().showControls(top, Pointing.DOWN, 40).rightClick();
        builder.overlay().showText(70)
                .text("Right-Click with an empty hand to bind it to you, and again to let go")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(25);
        builder.world().modifyBlockEntityNBT(util.select().position(seeker), SeekerBlockEntity.class,
                tag -> tag.putBoolean("player_dir_powered", true), true);
        builder.idle(55);

        builder.overlay().showText(80)
                .text("While bound, its sides output the direction you look, from any distance")
                .pointAt(util.vector().topOf(eastNixie))
                .placeNearTarget();
        turnPointer(builder, seeker, -90, 0);
        JocSceneUtil.setWire(builder, eastWire, 15);
        JocSceneUtil.setNixie(builder, util, eastNixie, 15);
        builder.effects().indicateRedstone(eastWire);
        builder.idle(50);

        turnPointer(builder, seeker, -45, 0);
        JocSceneUtil.setWire(builder, eastWire, 8);
        JocSceneUtil.setWire(builder, southWire, 8);
        JocSceneUtil.setNixie(builder, util, eastNixie, 8);
        JocSceneUtil.setNixie(builder, util, southNixie, 8);
        builder.effects().indicateRedstone(southWire);
        builder.idle(40);

        turnPointer(builder, seeker, 45, 0);
        JocSceneUtil.setWire(builder, eastWire, 0);
        JocSceneUtil.setWire(builder, westWire, 8);
        JocSceneUtil.setNixie(builder, util, eastNixie, 0);
        JocSceneUtil.setNixie(builder, util, westNixie, 8);
        builder.effects().indicateRedstone(westWire);
        builder.idle(40);

        turnPointer(builder, seeker, 135, 0);
        JocSceneUtil.setWire(builder, southWire, 0);
        JocSceneUtil.setWire(builder, northWire, 8);
        JocSceneUtil.setNixie(builder, util, southNixie, 0);
        JocSceneUtil.setNixie(builder, util, northNixie, 8);
        builder.effects().indicateRedstone(northWire);
        builder.idle(30);

        builder.overlay().showText(80)
                .text("Feed these into your Engines to steer a Contraption just by looking")
                .independent(10)
                .attachKeyFrame();
        builder.idle(90);

        builder.markAsFinished();
    }

    public static void modulating(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_modulating", "Measuring Distance with the Seeker");
        builder.configureBasePlate(0, 0, 9);
        builder.scaleSceneView(0.9f);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos seeker = util.grid().at(2, 2, 4);
        BlockPos wire = seeker.east(), nixie = seeker.east(2), target = util.grid().at(8, 1, 4);

        ElementLink<WorldSectionElement> cart = builder.world().showIndependentSection(util.select().fromTo(1, 1, 3, 4, 2, 5), Direction.DOWN);
        builder.idle(10);
        builder.world().showSection(util.select().position(target), Direction.DOWN);
        builder.idle(15);

        Vec3 top = util.vector().topOf(seeker);
        ItemStack receiver = SimBlocks.MODULATING_LINKED_RECEIVER.get().asItem().getDefaultInstance();
        builder.overlay().showControls(top, Pointing.DOWN, 40).rightClick().withItem(receiver);
        builder.overlay().showText(70)
                .text("A Modulating Linked Receiver makes the Seeker measure how far away the Target is")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(25);
        builder.world().modifyBlockEntityNBT(util.select().position(seeker), SeekerBlockEntity.class, tag -> {
            tag.putString("module", "MODULATING");
            tag.putBoolean("use_3d", true);
            tag.putBoolean("has_target", true);
            tag.putDouble("target_x", target.getX());
            tag.putDouble("target_y", seeker.getY());
            tag.putDouble("target_z", target.getZ());
            tag.putInt("min_distance", 2);
            tag.putInt("max_distance", 6);
        }, true);
        builder.effects().indicateSuccess(seeker);
        builder.idle(55);

        builder.overlay().showControls(top, Pointing.DOWN, 40).rightClick();
        builder.overlay().showText(80)
                .text("Right-Click to set a Min and Max distance: full signal at Min, none past Max")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.overlay().showText(90)
                .text("Every side outputs the same signal, and the Receiver spins faster as the Target gets closer")
                .pointAt(util.vector().topOf(nixie))
                .placeNearTarget()
                .attachKeyFrame();
        for (int i = 1; i <= 3; i++) {
            builder.world().moveSection(cart, util.vector().of(1, 0, 0), 15);
            builder.idle(15);
            int distance = 6 - i;
            int strength = Math.round(15f * (1f - (distance - 2) / 4f));
            builder.world().modifyBlockEntityNBT(util.select().position(seeker), SeekerBlockEntity.class,
                    tag -> tag.putDouble("target_x", seeker.getX() + distance), false);
            JocSceneUtil.setWire(builder, wire, strength);
            JocSceneUtil.setNixie(builder, util, nixie, strength);
            builder.idle(10);
        }
        builder.idle(30);

        builder.markAsFinished();
    }

    public static void linking(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_linking", "Linking Seekers Together");
        builder.configureBasePlate(0, 0, 9);
        builder.scaleSceneView(0.9f);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos left = util.grid().at(1, 2, 4), right = util.grid().at(7, 2, 4), target = util.grid().at(4, 1, 1);
        builder.world().showSection(util.select().fromTo(1, 1, 3, 1, 2, 5).add(util.select().fromTo(7, 1, 3, 7, 2, 5)), Direction.DOWN);
        builder.idle(10);
        builder.world().showSection(util.select().position(target), Direction.DOWN);
        builder.idle(10);

        setModule(builder, util, left, "SPYGLASS", target);
        setModule(builder, util, right, "SPYGLASS", null);
        pointAt(builder, left, target);
        builder.idle(15);

        Vec3 leftTop = util.vector().topOf(left), rightTop = util.vector().topOf(right);
        ItemStack seekerItem = JocItems.SEEKER.get().getDefaultInstance();
        builder.overlay().showControls(leftTop, Pointing.DOWN, 40).rightClick().withItem(seekerItem);
        builder.overlay().showText(60)
                .text("Right-Click a Seeker with a Seeker item to capture it")
                .pointAt(leftTop)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(70);

        builder.overlay().showControls(rightTop, Pointing.DOWN, 40).rightClick().withItem(seekerItem);
        builder.overlay().showText(70)
                .text("Right-Click a second Seeker to link them. Placing the held Seeker down links it too")
                .pointAt(rightTop)
                .placeNearTarget();
        builder.idle(30);
        setModule(builder, util, right, "SPYGLASS", target);
        pointAt(builder, right, target);
        builder.effects().indicateSuccess(right);
        builder.overlay().showOutline(PonderPalette.GREEN, "linked_left", util.select().position(left), 50);
        builder.overlay().showOutline(PonderPalette.GREEN, "linked_right", util.select().position(right), 50);
        builder.overlay().showLine(PonderPalette.GREEN, util.vector().centerOf(left), util.vector().centerOf(right), 50);
        builder.idle(50);

        builder.world().hideSection(util.select().position(target), Direction.UP);
        builder.idle(5);
        BlockPos moved = util.grid().at(4, 1, 7);
        builder.world().setBlock(moved, net.minecraft.world.level.block.Blocks.TARGET.defaultBlockState(), false);
        builder.world().showSection(util.select().position(moved), Direction.DOWN);
        builder.idle(10);
        setModule(builder, util, left, "SPYGLASS", moved);
        setModule(builder, util, right, "SPYGLASS", moved);
        pointAt(builder, left, moved);
        pointAt(builder, right, moved);
        builder.overlay().showText(80)
                .text("Linked Seekers share their Target, so changing one retargets them all")
                .independent(10)
                .attachKeyFrame();
        builder.idle(90);

        builder.markAsFinished();
    }

    public static void logicCore(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_logic_core", "Going Wireless with the Logic Core");
        builder.configureBasePlate(0, 0, 7);
        builder.scaleSceneView(1.3f);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos seeker = util.grid().at(3, 1, 3), target = util.grid().at(6, 1, 0);
        BlockPos wire = seeker.east(), nixie = seeker.east(2);
        BlockPos lamp = util.grid().at(1, 2, 5);
        Selection receiver = util.select().fromTo(1, 1, 5, 1, 2, 5);

        builder.world().showSection(util.select().fromTo(3, 1, 3, 5, 1, 3), Direction.DOWN);
        builder.idle(10);
        builder.world().showSection(util.select().position(target), Direction.DOWN);
        builder.idle(5);
        setModule(builder, util, seeker, "SPYGLASS", target);
        pointAt(builder, seeker, target);
        JocSceneUtil.setWire(builder, wire, 8);
        JocSceneUtil.setNixie(builder, util, nixie, 8);
        builder.idle(25);

        Vec3 top = util.vector().topOf(seeker);
        builder.overlay().showControls(top, Pointing.DOWN, 50)
                .rightClick()
                .withItem(JocItems.LOGIC_CORE.get().getDefaultInstance());
        builder.overlay().showText(80)
                .text("Open Advanced Options in the Seeker's screen and place a Logic Core in its slot")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        JocSceneUtil.setWire(builder, wire, 0);
        JocSceneUtil.setNixie(builder, util, nixie, 0);
        builder.overlay().showText(70)
                .text("The Seeker stops powering its sides...")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(wire))
                .placeNearTarget();
        builder.idle(80);

        builder.world().showSection(receiver, Direction.DOWN);
        builder.idle(15);
        builder.world().modifyBlock(lamp, state -> state.setValue(RedstoneLampBlock.LIT, true), false);
        builder.effects().indicateRedstone(lamp);
        builder.overlay().showControls(util.vector().topOf(lamp), Pointing.DOWN, 40)
                .withItem(AllBlocks.REDSTONE_LINK.asStack());
        builder.overlay().showText(90)
                .text("...and sends each side's signal over its own Redstone Link frequency instead")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(lamp))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(100);

        builder.markAsFinished();
    }
}
