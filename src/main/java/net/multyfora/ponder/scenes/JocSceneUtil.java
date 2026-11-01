package net.multyfora.ponder.scenes;

import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.phys.Vec3;

final class JocSceneUtil {
    private JocSceneUtil() {}

    static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    static void smoothMove(SceneBuilder builder, ElementLink<WorldSectionElement> link, Vec3 offset, int ticks) {
        for (int i = 0; i < ticks; i++) {
            double step = ease((i + 1.0) / ticks) - ease((double) i / ticks);
            builder.world().moveSection(link, offset.scale(step), 1);
            builder.idle(1);
        }
    }

    static void setWire(SceneBuilder builder, BlockPos pos, int power) {
        builder.world().modifyBlock(pos, state -> state.setValue(RedStoneWireBlock.POWER, power), false);
    }

    static void setNixie(SceneBuilder builder, SceneBuildingUtil util, BlockPos pos, int strength) {
        builder.world().modifyBlockEntityNBT(util.select().position(pos), NixieTubeBlockEntity.class,
                tag -> tag.putInt("RedstoneStrength", strength), true);
    }
}
