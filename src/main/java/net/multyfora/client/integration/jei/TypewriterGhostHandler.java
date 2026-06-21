package net.multyfora.client.integration.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import net.multyfora.client.portable_typewriter.PortableTypewriterScreen;

public class TypewriterGhostHandler<R extends PortableTypewriterScreen> implements IGhostIngredientHandler<R> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(R screen, ITypedIngredient<I> ingredient, boolean doStart) {
        I ing = ingredient.getIngredient();
        if (!(ing instanceof ItemStack stack) || stack.isEmpty())
            return List.of();

        List<Target<I>> targets = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Rect2i area = screen.getFreqSlotArea(i);
            if (area == null) continue;
            final int slot = i;
            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I ingredient) {
                    screen.acceptFreqSlotIngredient(slot, stack.copyWithCount(1));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {}
}
