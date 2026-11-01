package net.multyfora.client.balloon;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.CTContext;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.ContextRequirement;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.AeronauticsJoyofcreation;

import java.util.EnumMap;
import java.util.Map;

public class BalloonCTBehaviour extends SimpleCTBehaviour {

    private static final int KNOT_INDEX = 7;

    private static final Map<DyeColor, BalloonCTBehaviour> BEHAVIOURS = new EnumMap<>(DyeColor.class);

    private static final CTType BOTTOM = new CTType() {
        private static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "balloon_bottom");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public int getSheetSize() {
            return AllCTTypes.OMNIDIRECTIONAL.getSheetSize();
        }

        @Override
        public ContextRequirement getContextRequirement() {
            return AllCTTypes.OMNIDIRECTIONAL.getContextRequirement();
        }

        @Override
        public int getTextureIndex(CTContext context) {
            int index = AllCTTypes.OMNIDIRECTIONAL.getTextureIndex(context);
            return index == 0 ? KNOT_INDEX : index;
        }
    };

    static {
        for (DyeColor color : DyeColor.values()) {
            String texture = "block/balloon_" + color.getSerializedName();
            BEHAVIOURS.put(color, new BalloonCTBehaviour(CTSpriteShifter.getCT(
                    AllCTTypes.OMNIDIRECTIONAL,
                    ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, texture),
                    ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, texture + "_connected"))));
        }
    }

    private BalloonCTBehaviour(CTSpriteShiftEntry shift) {
        super(shift);
    }

    public static BalloonCTBehaviour get(DyeColor color) {
        return BEHAVIOURS.get(color);
    }

    public static void init() {}

    @Override
    public CTType getDataType(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
        return direction == Direction.DOWN ? BOTTOM : super.getDataType(world, pos, state, direction);
    }
}
