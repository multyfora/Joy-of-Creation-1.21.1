package net.multyfora.client;

import net.multyfora.client.portable_throttle.ThrottleVisualState;
import net.multyfora.client.portable_typewriter.TypewriterTypists;
import net.multyfora.network.PortableThrottlePosePayloads;
import net.multyfora.network.PortableTypewriterStatePayloads;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandlers {

    public static void handleThrottlePose(PortableThrottlePosePayloads.PoseState payload, IPayloadContext context) {
        context.enqueueWork(() -> ThrottleVisualState.set(payload.entityId(), payload.gripping(), payload.strength()));
    }

    public static void handleTypingState(PortableTypewriterStatePayloads.TypingState payload, IPayloadContext context) {
        context.enqueueWork(() -> TypewriterTypists.set(payload.entityId(), payload.typing()));
    }
}
