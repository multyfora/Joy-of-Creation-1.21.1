package net.multyfora.content.chain_rope;

import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

public class ChainCouplingItem extends RopeItem {
    private static boolean placing = false;

    public ChainCouplingItem(Properties properties) {
        super(properties);
    }

    public static boolean isPlacing() {
        return placing;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return super.useOn(context);
        }

        placing = true;
        try {
            return super.useOn(context);
        } finally {
            placing = false;
        }
    }
}
