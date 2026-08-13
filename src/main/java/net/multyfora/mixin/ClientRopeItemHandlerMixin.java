package net.multyfora.mixin;

import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.items.rope.RopeItem.ClientRopeItemHandler;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import dev.simulated_team.simulated.index.SimDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client-side validity for the rope placement preview (the green/orange outliner line):
 * a connector that is already connected to the first rope attachment point is not a valid
 * target.
 */
@Mixin(value = ClientRopeItemHandler.class, remap = false)
public class ClientRopeItemHandlerMixin {

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/content/items/rope/RopeItem/RopeItem;isValidRopeAttachment(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z")
    )
    private static boolean joc$rejectAlreadyConnected(Level level, BlockPos blockPos) {
        boolean valid = RopeItem.isValidRopeAttachment(level, blockPos);
        if (!valid) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        BlockPos first = null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.has(SimDataComponents.ROPE_FIRST_CONNECTION)) {
                first = stack.get(SimDataComponents.ROPE_FIRST_CONNECTION);
                break;
            }
        }
        if (first != null && joc$hasDirectRope(level, first, blockPos)) {
            return false;
        }
        return valid;
    }

    // True if any rope already connects posA and posB (in either direction)
    @Unique
    private static boolean joc$hasDirectRope(Level level, BlockPos posA, BlockPos posB) {
        for (ClientRopeStrand strand : ClientLevelRopeManager.getOrCreate(level).getAllStrands()) {
            Vec3 start = strand.startAttachment;
            Vec3 end = strand.endAttachment;
            if (start == null || end == null) continue;
            boolean aMatches = posA.equals(BlockPos.containing(start)) || posA.equals(BlockPos.containing(end));
            if (aMatches && (posB.equals(BlockPos.containing(start)) || posB.equals(BlockPos.containing(end)))) {
                return true;
            }
        }
        return false;
    }
}