package net.multyfora.content.seeker;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.multyfora.index.JocItems;

import org.jetbrains.annotations.Nullable;

public enum SeekerTableMarker {
    PIN(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.4f),
    KNIFE(SoundEvents.TRIDENT_HIT, 1.3f),
    FLAG(SoundEvents.WOOL_PLACE, 1.0f),
    ARROW(SoundEvents.ARROW_HIT, 1.0f),
    TORCH(SoundEvents.FIRECHARGE_USE, 1.6f);

    public static final int DEFAULT_COLOR = 0xB02E26;

    public final SoundEvent placeSound;
    public final float pitch;

    SeekerTableMarker(SoundEvent placeSound, float pitch) {
        this.placeSound = placeSound;
        this.pitch = pitch;
    }

    @Nullable
    public static SeekerTableMarker forItem(ItemStack stack) {
        if (stack.isEmpty() || stack.is(JocItems.MAP_PIN.get()))
            return PIN;
        if (stack.is(Items.IRON_SWORD))
            return KNIFE;
        if (stack.is(ItemTags.BANNERS))
            return FLAG;
        if (stack.is(Items.ARROW))
            return ARROW;
        if (stack.is(Items.TORCH))
            return TORCH;
        return null;
    }

    public static int colorFor(ItemStack stack) {
        return stack.getItem() instanceof BannerItem banner ? banner.getColor().getTextureDiffuseColor() & 0xFFFFFF : DEFAULT_COLOR;
    }

    public static SeekerTableMarker byName(String name) {
        for (SeekerTableMarker marker : values()) {
            if (marker.name().equalsIgnoreCase(name))
                return marker;
        }
        return PIN;
    }
}
