package net.multyfora.config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class FeatureGuards {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickItem.class, FeatureGuards::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, FeatureGuards::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, FeatureGuards::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, FeatureGuards::onTooltip);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (JocFeatures.isEnabled(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        notifyDisabled(event.getEntity());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        boolean itemDisabled = !JocFeatures.isEnabled(event.getItemStack());
        boolean blockDisabled = !JocFeatures.isEnabled(event.getLevel().getBlockState(event.getPos()).getBlock());
        if (!itemDisabled && !blockDisabled) return;

        if (itemDisabled) event.setUseItem(TriState.FALSE);
        if (blockDisabled) event.setUseBlock(TriState.FALSE);
        notifyDisabled(event.getEntity());
    }

    private static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (JocFeatures.isEnabled(event.getPlacedBlock().getBlock())) return;
        event.setCanceled(true);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (JocFeatures.isEnabled(event.getItemStack())) return;
        event.getToolTip().add(1, Component.translatable("joc.tooltip.disabled").withStyle(ChatFormatting.RED));
    }

    public static void notifyDisabled(Player player) {
        if (player.level().isClientSide()) return;
        player.displayClientMessage(Component.translatable("joc.feature_disabled").withStyle(ChatFormatting.RED), true);
    }
}
