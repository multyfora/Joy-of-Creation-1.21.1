package net.multyfora.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import net.multyfora.IMultiRopeBehavior;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * The base dependency provider only considers the primary attached strand, so the
 * cross-sublevel constraints of extra strands wouldn't be held as connection
 * dependencies. Merge in all extra strands' attachment sublevels.
 **/
@Mixin(value = RopeStrandHolderBlockEntity.class, remap = false)
public interface RopeStrandHolderBlockEntityMixin extends RopeStrandHolderBlockEntity {

    @Inject(method = "sable$getConnectionDependencies", at = @At("RETURN"), cancellable = true, remap = false)
    default void joc$addExtraDependencies(CallbackInfoReturnable<Iterable<SubLevel>> cir) {
        RopeStrandHolderBehavior behavior = getBehavior();
        if (!(behavior instanceof IMultiRopeBehavior multi)) return;

        List<UUID> ids = multi.joc$getAllAttachedRopeIDs();
        if (ids == null || ids.isEmpty()) return;

        SmartBlockEntity be = behavior.blockEntity;
        Level level = be.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(serverLevel);
        SubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        if (container == null) return;

        ObjectArrayList<SubLevel> extraLevels = null;
        for (UUID id : ids) {
            ServerRopeStrand strand = manager.getStrand(id);
            if (strand == null) continue;
            for (RopeAttachment attachment : strand.getAttachments()) {
                UUID subLevelID = attachment.subLevelID();
                if (subLevelID == null) continue;
                SubLevel subLevel = container.getSubLevel(subLevelID);
                if (subLevel == null) continue;
                if (extraLevels == null) extraLevels = new ObjectArrayList<>();
                if (!extraLevels.contains(subLevel)) extraLevels.add(subLevel);
            }
        }
        if (extraLevels == null) return;

        Iterable<SubLevel> base = cir.getReturnValue();
        if (base != null) {
            ObjectArrayList<SubLevel> merged = new ObjectArrayList<>();
            for (SubLevel subLevel : base) merged.add(subLevel);
            for (SubLevel subLevel : extraLevels) {
                if (!merged.contains(subLevel)) merged.add(subLevel);
            }
            cir.setReturnValue(merged);
        } else {
            cir.setReturnValue(extraLevels);
        }
    }
}