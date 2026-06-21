package net.multyfora.client.integration.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import net.multyfora.client.portable_throttle.PortableThrottleScreen;

public class ThrottleDragHandler implements EmiDragDropHandler<PortableThrottleScreen> {

    @Override
    public boolean dropStack(PortableThrottleScreen screen, EmiIngredient ingredient, int mouseX, int mouseY) {
        for (int i = 0; i < 2; i++) {
            Rect2i area = screen.getFreqSlotArea(i);
            if (area != null && area.contains(mouseX, mouseY)) {
                for (EmiStack es : ingredient.getEmiStacks()) {
                    ItemStack stack = es.getItemStack();
                    if (!stack.isEmpty()) {
                        screen.acceptFreqSlotIngredient(i, stack.copyWithCount(1));
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
