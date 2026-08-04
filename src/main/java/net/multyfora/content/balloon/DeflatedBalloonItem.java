package net.multyfora.content.balloon;

import com.simibubi.create.foundation.item.CustomUseEffectsItem;
import net.createmod.catnip.data.TriState;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocSounds;

public class DeflatedBalloonItem extends Item implements CustomUseEffectsItem {

    public static final int USE_DURATION = 40;

    private final DyeColor color;

    public DeflatedBalloonItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player))
            return stack;

        level.playSound(null, player.blockPosition(), JocSounds.BALLOON_POP.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);

        if (level.isClientSide)
            return stack;

        stack.shrink(1);
        ItemStack inflated = new ItemStack(JocBlocks.BALLOON_ITEMS.get(color).get());
        if (!player.getInventory().add(inflated))
            player.drop(inflated, false);

        return stack;
    }

    @Override
    public TriState shouldTriggerUseEffects(ItemStack stack, LivingEntity entity) {
        return TriState.TRUE;
    }

    @Override
    public boolean triggerUseEffects(ItemStack stack, LivingEntity entity, int count, RandomSource random) {
        int ticksUsing = entity.getTicksUsingItem();
        if (ticksUsing == 10) {
            entity.playSound(JocSounds.BALLOON_BLOW.get(), 0.85F + 0.15F * random.nextFloat(),
                    0.85F + random.nextFloat() * 0.25F);
        }
        return true;
    }
}