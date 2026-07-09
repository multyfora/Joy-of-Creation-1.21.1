package net.multyfora.register;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.multyfora.content.balloon.BalloonBlock;
import net.multyfora.index.JocBlocks;

public class JocCommands {

    private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(
            Component.literal("Failed to spawn balloon — blocks could not be placed")
    );

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
        BlockState balloonState = JocBlocks.BALLOON.get().defaultBlockState()
                .setValue(BalloonBlock.COLOR, color);

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
                bottomBehavior.createRope(topBehavior);
            }
        }

        source.sendSuccess(() -> Component.literal("Spawned balloon at " + balloonPos.toShortString()), true);
        return 1;
    }
}
