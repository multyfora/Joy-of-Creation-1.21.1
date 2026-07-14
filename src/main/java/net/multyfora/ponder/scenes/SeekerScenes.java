package net.multyfora.ponder.scenes;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RedStoneWireBlock;

public class SeekerScenes {
    public static void basic(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("seeker", "Using the Seeker");
        builder.configureBasePlate(0, 0, 7);
        builder.showBasePlate();
        builder.idle(5);

        BlockPos seekerPos = util.grid().at(3, 1, 3);
        BlockPos nixie1 = util.grid().at(5, 1, 3);
        BlockPos nixie1Redstone = util.grid().at(4, 1, 3);
        BlockPos nixie2 = util.grid().at(3, 1, 1);
        BlockPos nixie2Redstone = util.grid().at(3, 1, 2);

        BlockPos [] nixieRedstones = {nixie1Redstone, nixie2Redstone};

        builder.world().showSection(
                util.select().cuboid(util.grid().at(1, 1, 1), util.grid().at(6, 1, 6)),
                Direction.DOWN
        );
        builder.idle(10);

        builder.overlay().showText(80)
                .text("The Seeker tracks targets and emits redstone signals based on the direction to the target.")
                .pointAt(util.vector().topOf(seekerPos))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.overlay().showControls(util.vector().topOf(seekerPos), Pointing.DOWN, 60)
                .rightClick()
                .withItem(Items.SPYGLASS.getDefaultInstance());
        builder.idle(10);

        builder.overlay().showText(60)
                .text("Insert a Spyglass module to add coordinate tracking. The Seeker will output a redstone signal toward the target.")
                .pointAt(util.vector().topOf(seekerPos))
                .placeNearTarget();
        builder.idle(70);

        builder.world().modifyBlockEntityNBT(
                util.select().position(seekerPos),
                net.minecraft.world.level.block.entity.BlockEntity.class,
                tag -> {
                    tag.putString("module", "SPYGLASS");
                    tag.putBoolean("has_target", true);
                    tag.putDouble("target_x", 3.0);
                    tag.putDouble("target_y", 1.0);
                    tag.putDouble("target_z", 3.0);
                    tag.putFloat("relative_angle", 135.0f);
                    tag.putFloat("tilt_angle", 0.0f);
                    tag.putBoolean("use_3d", true);
                }
        );
        builder.idle(5);
        builder.effects().indicateRedstone(seekerPos);
        builder.idle(10);

        builder.world().modifyBlock(
                nixie1Redstone,
                blockState -> blockState.setValue(RedStoneWireBlock.POWER, 15),
                false
        );
        builder.world().modifyBlockEntityNBT(
                util.select().position(nixie1),
                net.minecraft.world.level.block.entity.BlockEntity.class,
                tag -> tag.putInt("RedstoneStrength", 7),
                true
        );
        builder.effects().indicateRedstone(nixie1Redstone);

        builder.world().modifyBlock(
                nixie2Redstone,
                blockState -> blockState.setValue(RedStoneWireBlock.POWER, 15),
                false
        );
        builder.world().modifyBlockEntityNBT(
                util.select().position(nixie2),
                net.minecraft.world.level.block.entity.BlockEntity.class,
                tag -> tag.putInt("RedstoneStrength", 8),
                true
        );
        builder.effects().indicateRedstone(nixie2Redstone);

        builder.overlay().showText(80)
                .text("The nixie tubes now show the redstone signal strength, indicating the angle to the target.")
                .pointAt(util.vector().blockSurface(nixie1, Direction.UP))
                .placeNearTarget()
                .attachKeyFrame();
        builder.idle(90);

        builder.markAsFinished();
    }
}