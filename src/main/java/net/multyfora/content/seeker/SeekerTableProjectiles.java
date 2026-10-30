package net.multyfora.content.seeker;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

public class SeekerTableProjectiles {

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!projectile.getType().is(EntityTypeTags.ARROWS) || !(event.getRayTraceResult() instanceof BlockHitResult hit))
            return;
        if (!SeekerTableBlock.markFromProjectile(projectile.level(), hit, SeekerTableMarker.ARROW, SeekerTableMarker.DEFAULT_COLOR))
            return;
        event.setCanceled(true);
        projectile.discard();
    }
}
