package net.multyfora.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.advancement.JocAdvancements;
import net.multyfora.content.shears_cut.ShearsPreviewServerHandler;
import net.multyfora.content.shears_cut.SubLevelCutter;

import java.util.UUID;

public class ShearsCutPayloads {

    public record ShearsCutPayload(UUID subLevelId, BlockPos point1, BlockPos point2, Direction orientation) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "shears_cut");
        public static final Type<ShearsCutPayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, ShearsCutPayload> CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, ShearsCutPayload::subLevelId,
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

            try {
                SubLevel containing = SubLevelContainer.getContainer(level).getSubLevel(subLevelId);
                if (!(containing instanceof ServerSubLevel subLevel) || subLevel.isRemoved()) {
                    AeronauticsJoyofcreation.LOGGER.warn("Shears cut rejected: no sublevel {} found for {}", subLevelId, player.getName().getString());
                    player.sendSystemMessage(Component.literal("No sublevel found for that cut"));
                    return;
                }

                ShearsPreviewServerHandler.broadcast(level,
                        new ShearsPreviewPayload(player.getUUID(), subLevelId, point1, point2, orientation, PreviewPhase.DONE),
                        player);

                if (AeronauticsJoyofcreation.LOGGER.isDebugEnabled()) {
                    AeronauticsJoyofcreation.LOGGER.debug("Shears cut {} -> {} on {} of sublevel {}", point1, point2, orientation, subLevelId);
                }

                SubLevelCutter.Result result = SubLevelCutter.cut(
                        level,
                        subLevel,
                        point1,
                        point2,
                        orientation.getAxis(),
                        SubLevelCutter.planeCoord(point1, orientation)
                );

                if (result == null) {
                    AeronauticsJoyofcreation.LOGGER.info("Shears cut at {} on sublevel {} separated nothing", point1, subLevelId);
                    player.sendSystemMessage(Component.literal("Nothing separated - structure is still connected around the cut"));
                    return;
                }

                AeronauticsJoyofcreation.LOGGER.info("Shears cut split sublevel {} into {} pieces (largest kept {})",
                        subLevelId, result.pieceCount(), result.largestSize());
                JocAdvancements.DISMANTLE.awardTo(player);
            } catch (Exception e) {
                AeronauticsJoyofcreation.LOGGER.error("Shears cut failed", e);
                player.sendSystemMessage(Component.literal("Cut failed: " + e.getMessage()));
            }
        }
    }

    public enum PreviewPhase { PLACING, CANCEL, DONE }

    public record ShearsPreviewPayload(UUID actorId, UUID subLevelId, BlockPos point1, BlockPos point2,
                                       Direction orientation, PreviewPhase phase) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "shears_preview");
        public static final Type<ShearsPreviewPayload> TYPE = new Type<>(ID);

        private static final StreamCodec<RegistryFriendlyByteBuf, PreviewPhase> PHASE_STREAM_CODEC = StreamCodec.of(
                (buf, phase) -> buf.writeByte(phase.ordinal()),
                buf -> PreviewPhase.values()[buf.readByte()]
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ShearsPreviewPayload> CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, ShearsPreviewPayload::actorId,
                        UUIDUtil.STREAM_CODEC, ShearsPreviewPayload::subLevelId,
                        BlockPos.STREAM_CODEC, ShearsPreviewPayload::point1,
                        BlockPos.STREAM_CODEC, ShearsPreviewPayload::point2,
                        Direction.STREAM_CODEC, ShearsPreviewPayload::orientation,
                        PHASE_STREAM_CODEC, ShearsPreviewPayload::phase,
                        ShearsPreviewPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

}