package net.multyfora.content.seeker;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import net.multyfora.index.JocEntityTypes;
import net.multyfora.index.JocItems;

public class MapPinEntity extends ThrowableItemProjectile {

    public MapPinEntity(EntityType<? extends MapPinEntity> type, Level level) {
        super(type, level);
    }

    public MapPinEntity(Level level, LivingEntity shooter) {
        super(JocEntityTypes.MAP_PIN.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return JocItems.MAP_PIN.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), 1);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide)
            return;
        boolean marked = result instanceof BlockHitResult hit
            && SeekerTableBlock.markFromProjectile(level(), hit, SeekerTableMarker.PIN, SeekerTableMarker.DEFAULT_COLOR);
        if (!marked) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARROW_HIT, SoundSource.NEUTRAL, 0.5f, 1.8f);
            if (!(getOwner() instanceof Player player && player.getAbilities().instabuild))
                spawnAtLocation(getItem());
        }
        discard();
    }
}
