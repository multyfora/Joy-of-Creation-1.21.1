package net.multyfora.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.advancement.JocAdvancements;
import net.multyfora.content.shears_cut.ShearsCutState;
import net.multyfora.content.shears_cut.SubLevelCutter;

public class ShearsCutPayloads {

    public record ShearsCutPayload(BlockPos point1, BlockPos point2, Direction orientation) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "shears_cut");
        public static final Type<ShearsCutPayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, ShearsCutPayload> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, ShearsCutPayload::point1,
                        BlockPos.STREAM_CODEC, ShearsCutPayload::point2,
                        Direction.STREAM_CODEC, ShearsCutPayload::orientation,
                        ShearsCutPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public void handle(ServerPlayer player) {
            ServerLevel level = player.serverLevel();

            SubLevel containing = Sable.HELPER.getContaining(level, point1);
            if (!(containing instanceof ServerSubLevel subLevel) || subLevel.isRemoved()) {
                player.sendSystemMessage(Component.literal("No sublevel found at that position"));
                return;
            }

            SubLevelCutter.Result result = SubLevelCutter.cut(
                    level,
                    subLevel,
                    point1,
                    point2,
                    orientation.getAxis(),
                    (int) ShearsCutState.facePlaneCoord(point1, orientation)
            );

            if (result == null) {
                player.displayClientMessage(Component.literal(
                        "Nothing separated - structure is still connected around the cut"), true);
                return;
            }

            JocAdvancements.DISMANTLE.awardTo(player);
        }
    }

}