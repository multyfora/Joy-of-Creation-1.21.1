package net.multyfora.register;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.multyfora.content.shears_cut.SubLevelCutter;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.multyfora.index.JocBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

public class JocCommands {

    private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(
            Component.literal("Failed to spawn balloon — blocks could not be placed")
    );

    private static final SuggestionProvider<CommandSourceStack> AXIS_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    List.of("x", "y", "z"), builder);

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("joc")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("balloon")
                                .executes(ctx -> spawnBalloon(ctx.getSource(), 0))
                                .then(Commands.argument("color", IntegerArgumentType.integer(0, 15))
                                        .executes(ctx -> spawnBalloon(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "color")))
                                )
                        )
                        .then(Commands.literal("split_sublevel")
                                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                                .executes(ctx -> executeSplitSubLevel(ctx, "z"))
                                                .then(Commands.argument("axis", StringArgumentType.word())
                                                        .suggests(AXIS_SUGGESTIONS)
                                                        .executes(ctx -> executeSplitSubLevel(
                                                                ctx,
                                                                StringArgumentType.getString(ctx, "axis")))
                                                )
                                        )
                                )
                        )
        );
    }

    private static int spawnBalloon(CommandSourceStack source, int color) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("Command must be executed by a player"));
            return 0;
        }
        Level level = player.level();
        BlockPos feet = player.blockPosition();

        BlockPos connectorBottom = feet;
        BlockPos connectorTop = feet.above(2);
        BlockPos balloonPos = feet.above(3);

        Block ropeConnectorBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("simulated:rope_connector"));
        if (ropeConnectorBlock == null || ropeConnectorBlock == Blocks.AIR) {
            source.sendFailure(Component.literal("Rope connector block not found in registry"));
            return 0;
        }

        // Check all three positions are air or replaceable
        for (BlockPos pos : new BlockPos[]{connectorBottom, connectorTop, balloonPos}) {
            if (!level.getBlockState(pos).isAir()) {
                source.sendFailure(Component.literal("Cannot place block at " + pos.toShortString() + " — block obstructed"));
                return 0;
            }
        }

        BlockState bottomConnectorState = ropeConnectorBlock.defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.UP);
        BlockState topConnectorState = ropeConnectorBlock.defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.DOWN);
        DyeColor dyeColor = DyeColor.byId(color);
        BlockState balloonState = JocBlocks.BALLOONS.get(dyeColor).get().defaultBlockState();

        boolean placedBottom = level.setBlock(connectorBottom, bottomConnectorState, Block.UPDATE_ALL);
        boolean placedTop = level.setBlock(connectorTop, topConnectorState, Block.UPDATE_ALL);
        boolean placedBalloon = level.setBlock(balloonPos, balloonState, Block.UPDATE_ALL);

        if (!(placedBottom && placedTop && placedBalloon)) {
            // Rollback any successful placements
            if (placedBottom) level.setBlock(connectorBottom, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (placedTop) level.setBlock(connectorTop, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (placedBalloon) level.setBlock(balloonPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            source.sendFailure(Component.literal("Failed to place blocks"));
            return 0;
        }

        // Create rope between the two connectors
        if (level.getBlockEntity(connectorBottom) instanceof SmartBlockEntity bottomBE
                && level.getBlockEntity(connectorTop) instanceof SmartBlockEntity topBE) {
            RopeStrandHolderBehavior bottomBehavior = bottomBE.getBehaviour(RopeStrandHolderBehavior.TYPE);
            RopeStrandHolderBehavior topBehavior = topBE.getBehaviour(RopeStrandHolderBehavior.TYPE);
            if (bottomBehavior != null && topBehavior != null) {
                bottomBehavior.createRope(topBehavior, true);
            }
        }

        source.sendSuccess(() -> Component.literal("Spawned balloon at " + balloonPos.toShortString()), true);
        return 1;
    }

    private static int executeSplitSubLevel(CommandContext<CommandSourceStack> ctx, String direction) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(ctx, "pos1");
        BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(ctx, "pos2");

        SubLevel containing = Sable.HELPER.getContaining(level, pos1);
        if (containing == null || containing.isRemoved()) {
            source.sendFailure(Component.literal("Target position is not inside a sublevel"));
            return 0;
        }
        ServerSubLevel subLevel = (ServerSubLevel) containing;

        Direction.Axis axis = switch (direction.toLowerCase(Locale.ROOT)) {
            case "x" -> Direction.Axis.X;
            case "y" -> Direction.Axis.Y;
            case "z" -> Direction.Axis.Z;
            default -> null;
        };
        if (axis == null) {
            source.sendFailure(Component.literal("Unknown direction: " + direction
                    + ". Use: north, south, east, west, up, down"));
            return 0;
        }

        int planeCoord = switch (axis) {
            case X -> pos1.getX();
            case Y -> pos1.getY();
            case Z -> pos1.getZ();
        };

        SubLevelCutter.Result result = SubLevelCutter.cut(level, subLevel, pos1, pos2, axis, planeCoord);
        if (result == null) {
            source.sendFailure(Component.literal("Nothing separated — structure is still connected around the cut"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Split sublevel into " + result.pieceCount()
                + " pieces (largest piece kept " + result.largestSize() + " blocks)"), true);
        return 1;
    }
}
