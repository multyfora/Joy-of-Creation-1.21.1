package net.multyfora.content.seeker;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MapPinItem extends Item {

    public MapPinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(),
            SoundSource.NEUTRAL, 0.4f, 1.6f + level.getRandom().nextFloat() * 0.3f);
        if (!level.isClientSide) {
            MapPinEntity pin = new MapPinEntity(level, player);
            pin.setItem(stack);
            pin.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 1.0f);
            level.addFreshEntity(pin);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
