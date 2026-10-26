package net.multyfora.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.multyfora.client.portable_throttle.ThrottleVisualState;
import net.multyfora.client.portable_typewriter.TypewriterTypists;
import net.multyfora.client.seeker.SeekerDistanceMenu;
import net.multyfora.client.seeker.SeekerDistanceScreen;
import net.multyfora.client.seeker.SeekerMenu;
import net.multyfora.client.seeker.SeekerScreen;
import net.multyfora.content.seeker.SeekerBlockEntity;
import net.multyfora.network.PortableThrottlePosePayloads;
import net.multyfora.network.PortableTypewriterStatePayloads;
import net.multyfora.network.SeekerDistancePayloads.OpenSeekerDistancePayload;
import net.multyfora.network.SeekerPayloads.OpenSeekerPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandlers {

    public static void handleThrottlePose(PortableThrottlePosePayloads.PoseState payload, IPayloadContext context) {
        context.enqueueWork(() -> ThrottleVisualState.set(payload.entityId(), payload.gripping(), payload.strength()));
    }

    public static void handleTypingState(PortableTypewriterStatePayloads.TypingState payload, IPayloadContext context) {
        context.enqueueWork(() -> TypewriterTypists.set(payload.entityId(), payload.typing()));
    }

    public static void handleOpenSeeker(OpenSeekerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockEntity blockEntity = level.getBlockEntity(payload.pos());

            if (!(blockEntity instanceof SeekerBlockEntity seekerBlockEntity)) {
                return;
            }

            SeekerMenu seekerMenu = new SeekerMenu(0, player.getInventory(), seekerBlockEntity);
            Minecraft.getInstance().setScreen(new SeekerScreen(seekerMenu, player.getInventory()));
        });
    }

    public static void handleOpenSeekerDistance(OpenSeekerDistancePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockEntity blockEntity = level.getBlockEntity(payload.pos());
            if (!(blockEntity instanceof SeekerBlockEntity seekerBlockEntity)) {
                return;
            }
            SeekerDistanceMenu seekerDistanceMenu = new SeekerDistanceMenu(0, player.getInventory(), seekerBlockEntity);
            Minecraft.getInstance().setScreen(new SeekerDistanceScreen(seekerDistanceMenu, player.getInventory()));
        });
    }
}
