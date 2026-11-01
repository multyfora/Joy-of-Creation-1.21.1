package net.multyfora.ponder.scenes;

import com.simibubi.create.AllItems;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.content.seeker.SeekerTableBlock;
import net.multyfora.content.seeker.SeekerTableBlockEntity;
import net.multyfora.content.seeker.SeekerTableMarker;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;

public class SeekerTableScenes {
    private static void form(SceneBuilder builder, SceneBuildingUtil util, BlockPos corner, Direction facing, int width, int depth, int scale) {
        Direction right = SeekerTableBlock.toWorld(facing, Direction.EAST);
        Direction down = SeekerTableBlock.toWorld(facing, Direction.SOUTH);
        BlockState base = JocBlocks.SEEKER_TABLE.get().defaultBlockState().setValue(SeekerTableBlock.FACING, facing);
        Vec3 out = Vec3.atLowerCornerOf(facing.getNormal());
        var wax = builder.effects().particleEmitterWithinBlockSpace(ParticleTypes.WAX_ON, Vec3.ZERO);
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                BlockState state = base
                        .setValue(SeekerTableBlock.NORTH, dz > 0)
                        .setValue(SeekerTableBlock.SOUTH, dz < depth - 1)
                        .setValue(SeekerTableBlock.WEST, dx > 0)
                        .setValue(SeekerTableBlock.EAST, dx < width - 1);
                BlockPos pos = corner.relative(right, dx).relative(down, dz);
                builder.world().setBlock(pos, state, false);
                builder.effects().emitParticles(Vec3.atLowerCornerOf(pos).add(out), wax, 2, 1);
            }
        }
        builder.world().modifyBlockEntityNBT(util.select().position(corner), SeekerTableBlockEntity.class, tag -> {
            tag.putInt("width", width);
            tag.putInt("depth", depth);
            tag.putInt("scale", scale);
        }, true);
    }

    private static void setMarker(SceneBuilder builder, SceneBuildingUtil util, BlockPos controller, SeekerTableMarker style, int color, double x, double z) {
        builder.world().modifyBlockEntityNBT(util.select().position(controller), SeekerTableBlockEntity.class, tag -> {
            CompoundTag marker = new CompoundTag();
            marker.putDouble("x", x);
            marker.putInt("y", 1);
            marker.putDouble("z", z);
            marker.putString("style", style.name().toLowerCase(java.util.Locale.ROOT));
            marker.putInt("color", color);
            tag.put("marker", marker);
        });
    }

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_table", "Mapping the World on a Seeker Table");
        builder.configureBasePlate(0, 0, 9);
        builder.scaleSceneView(0.9f);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos corner = util.grid().at(2, 1, 2);
        for (int ring = 0; ring < 9; ring++) {
            for (int x = 0; x < 5; x++) {
                int z = ring - x;
                if (z >= 0 && z < 5)
                    builder.world().showSection(util.select().position(corner.offset(x, 0, z)), Direction.DOWN);
            }
            builder.idle(3);
        }
        builder.idle(10);

        Selection grid = util.select().fromTo(2, 1, 2, 6, 1, 6);
        builder.overlay().showOutlineWithText(grid, 70)
                .text("An exact rectangle of empty Seekers, 3×3 or larger, joins into a Seeker Table")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(4, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(40);
        form(builder, util, corner, Direction.UP, 5, 5, 32);
        builder.idle(40);

        Vec3 center = util.vector().topOf(4, 1, 4);
        builder.overlay().showText(80)
                .text("Its surface draws a relief map of the land around it, rotating with the Contraption it sits on")
                .pointAt(center)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        BlockPos pinCell = util.grid().at(5, 1, 3);
        Vec3 pinAt = util.vector().topOf(pinCell);
        builder.overlay().showControls(pinAt, Pointing.DOWN, 40).rightClick();
        builder.overlay().showText(70)
                .text("Right-Click the map to pin a spot on it")
                .pointAt(pinAt)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(25);
        setMarker(builder, util, corner, SeekerTableMarker.PIN, SeekerTableMarker.DEFAULT_COLOR, 40, -20);
        builder.idle(60);

        Object[][] styles = {
                {Items.IRON_SWORD.getDefaultInstance(), SeekerTableMarker.KNIFE, SeekerTableMarker.DEFAULT_COLOR},
                {Items.LIGHT_BLUE_BANNER.getDefaultInstance(), SeekerTableMarker.FLAG, 0x3AB3DA},
                {Items.ARROW.getDefaultInstance(), SeekerTableMarker.ARROW, SeekerTableMarker.DEFAULT_COLOR},
                {Items.TORCH.getDefaultInstance(), SeekerTableMarker.TORCH, SeekerTableMarker.DEFAULT_COLOR}};
        builder.overlay().showText(120)
                .text("An Iron Sword, Banner, Arrow or Torch leaves its own kind of Marker")
                .pointAt(pinAt)
                .placeNearTarget();
        for (Object[] style : styles) {
            builder.overlay().showControls(pinAt, Pointing.DOWN, 25).rightClick().withItem((ItemStack) style[0]);
            builder.idle(10);
            setMarker(builder, util, corner, SeekerTableMarker.PIN, (int) style[2], 40, -20);
            setMarker(builder, util, corner, (SeekerTableMarker) style[1], (int) style[2], 40, -20);
            builder.idle(22);
        }
        builder.idle(5);

        Vec3 throwAt = util.vector().topOf(3, 1, 5);
        builder.overlay().showControls(throwAt, Pointing.DOWN, 50).withItem(JocItems.MAP_PIN.get().getDefaultInstance());
        builder.overlay().showText(70)
                .text("Map Pins can also be thrown, and Arrows shot, to mark wherever they land")
                .pointAt(throwAt)
                .placeNearTarget();
        builder.idle(25);
        setMarker(builder, util, corner, SeekerTableMarker.PIN, SeekerTableMarker.DEFAULT_COLOR, -30, 30);
        builder.idle(55);

        builder.overlay().showControls(center, Pointing.DOWN, 40).rightClick().withItem(AllItems.WRENCH.asStack());
        builder.overlay().showText(70)
                .text("Use a Wrench to cycle the map's scale, from 1:8 up to 1:128")
                .pointAt(center)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(25);
        builder.world().modifyBlockEntityNBT(util.select().position(corner), SeekerTableBlockEntity.class,
                tag -> tag.putInt("scale", 64));
        builder.idle(55);

        builder.overlay().showControls(center, Pointing.DOWN, 40).rightClick().whileSneaking();
        builder.overlay().showText(60)
                .text("Sneak and Right-Click to remove the Marker")
                .pointAt(center)
                .placeNearTarget();
        builder.idle(20);
        builder.world().modifyBlockEntityNBT(util.select().position(corner), SeekerTableBlockEntity.class,
                tag -> tag.remove("marker"));
        builder.idle(50);

        builder.markAsFinished();
    }

    public static void following(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_table_following", "Navigating by the Seeker Table");
        builder.configureBasePlate(0, 0, 9);
        builder.scaleSceneView(0.9f);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos corner = util.grid().at(2, 1, 2), seeker = util.grid().at(4, 1, 8);
        form(builder, util, corner, Direction.UP, 5, 5, 32);
        builder.world().showSection(util.select().fromTo(2, 1, 2, 6, 1, 6), Direction.DOWN);
        builder.idle(15);
        builder.world().showSection(util.select().position(seeker), Direction.DOWN);
        builder.idle(10);
        builder.world().modifyBlockEntityNBT(util.select().position(seeker), SeekerBlockEntity.class,
                tag -> tag.putString("module", "SPYGLASS"), true);
        builder.idle(15);

        Vec3 map = util.vector().topOf(4, 1, 4), top = util.vector().topOf(seeker);
        builder.overlay().showControls(map, Pointing.DOWN, 40).rightClick().withItem(JocItems.SEEKER.get().getDefaultInstance());
        builder.overlay().showText(60)
                .text("Right-Click the map with a Seeker item to capture the Table")
                .pointAt(map)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(70);

        builder.overlay().showControls(top, Pointing.DOWN, 40).rightClick().withItem(JocItems.SEEKER.get().getDefaultInstance());
        builder.overlay().showText(70)
                .text("Then Right-Click a Seeker holding a Spyglass or Receiver. It now follows the Table")
                .pointAt(top)
                .placeNearTarget();
        builder.idle(80);

        BlockPos pinCell = util.grid().at(6, 1, 3);
        Vec3 pinAt = util.vector().topOf(pinCell);
        builder.overlay().showControls(pinAt, Pointing.DOWN, 30).rightClick();
        builder.idle(15);
        setMarker(builder, util, corner, SeekerTableMarker.PIN, SeekerTableMarker.DEFAULT_COLOR, 70, -35);
        builder.idle(10);
        builder.world().modifyBlockEntityNBT(util.select().position(seeker), SeekerBlockEntity.class, tag -> {
            tag.putBoolean("has_target", true);
            tag.putDouble("target_x", 70);
            tag.putDouble("target_y", 1);
            tag.putDouble("target_z", -35);
        }, true);
        builder.world().modifyBlockEntity(seeker, SeekerBlockEntity.class, be ->
                be.spyglassPointer.lerpedYawDegrees.chase(Math.toDegrees(Math.atan2(66, -43)), 0.2, LerpedFloat.Chaser.EXP));
        builder.overlay().showText(90)
                .text("Wherever you pin the map, the Seeker points to that spot in the World")
                .pointAt(top)
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(100);

        builder.markAsFinished();
    }

    public static void wall(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker_table_wall", "Hanging a Seeker Table on a Wall");
        builder.configureBasePlate(0, 0, 7);
        builder.showBasePlate();
        builder.idle(5);

        builder.world().showSection(util.select().fromTo(1, 1, 4, 5, 5, 5), Direction.DOWN);
        builder.idle(15);
        BlockPos corner = util.grid().at(4, 4, 3);
        for (int y = 2; y <= 4; y++) {
            builder.world().showSection(util.select().fromTo(2, y, 3, 4, y, 3), Direction.SOUTH);
            builder.idle(6);
        }
        builder.idle(10);

        builder.overlay().showOutlineWithText(util.select().fromTo(2, 2, 3, 4, 4, 3), 70)
                .text("Seekers placed in an upright rectangle form a wall-mounted Table, facing the side you built it from")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(40);
        form(builder, util, corner, Direction.NORTH, 3, 3, 32);
        builder.idle(40);

        Vec3 pinAt = util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.NORTH);
        builder.overlay().showControls(pinAt, Pointing.LEFT, 40).rightClick();
        builder.overlay().showText(80)
                .text("It works just like a flat Table, with the bottom edge of the map pointing the way the wall faces")
                .pointAt(pinAt)
                .placeNearTarget();
        builder.idle(25);
        setMarker(builder, util, corner, SeekerTableMarker.PIN, SeekerTableMarker.DEFAULT_COLOR, 30, 20);
        builder.idle(70);

        builder.markAsFinished();
    }
}
