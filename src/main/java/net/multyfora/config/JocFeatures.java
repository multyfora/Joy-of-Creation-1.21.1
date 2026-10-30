package net.multyfora.config;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

public class JocFeatures {
    public static final String BALLOONS = "balloons";
    public static final String DEFLATED_BALLOONS = "deflatedBalloons";
    public static final String SYMMETRIC_CROSS_SAILS = "symmetricCrossSails";
    public static final String SEEKER = "seeker";
    public static final String SHATTER_ASSEMBLER = "shatterAssembler";
    public static final String GYROSCOPIC_SEAT = "gyroscopicSeat";
    public static final String PORTABLE_THROTTLE = "portableThrottle";
    public static final String PORTABLE_TYPEWRITER = "portableTypewriter";
    public static final String SHEARS_CUTTING = "shearsCutting";
    public static final String MULTI_ROPE = "multiRope";

    private static Map<Item, String> itemFeatures;
    private static Map<Block, String> blockFeatures;

    public static boolean isEnabled(String feature) {
        return JocConfig.isFeatureEnabled(feature);
    }

    public static boolean isEnabled(ItemStack stack) {
        return stack.isEmpty() || isEnabled(stack.getItem());
    }

    public static boolean isEnabled(Item item) {
        String feature = itemFeatures().get(item);
        return feature == null || isEnabled(feature);
    }

    public static boolean isEnabled(Block block) {
        String feature = blockFeatures().get(block);
        return feature == null || isEnabled(feature);
    }

    private static Map<Item, String> itemFeatures() {
        if (itemFeatures == null) {
            Map<Item, String> map = new IdentityHashMap<>();
            putAll(map, JocBlocks.BALLOON_ITEMS.values(), BALLOONS);
            putAll(map, JocItems.DEFLATED_BALLOONS.values(), DEFLATED_BALLOONS);
            putAll(map, JocBlocks.SYMMETRIC_CROSS_SAIL_ITEMS.values(), SYMMETRIC_CROSS_SAILS);
            map.put(JocItems.SEEKER.get(), SEEKER);
            map.put(JocItems.MAP_PIN.get(), SEEKER);
            map.put(JocItems.SHATTER_ASSEMBLER.get(), SHATTER_ASSEMBLER);
            map.put(JocItems.GYROSCOPIC_SEAT.get(), GYROSCOPIC_SEAT);
            map.put(JocItems.PORTABLE_THROTTLE.get(), PORTABLE_THROTTLE);
            map.put(JocItems.PORTABLE_TYPEWRITER.get(), PORTABLE_TYPEWRITER);
            itemFeatures = map;
        }
        return itemFeatures;
    }

    private static Map<Block, String> blockFeatures() {
        if (blockFeatures == null) {
            Map<Block, String> map = new IdentityHashMap<>();
            putAll(map, JocBlocks.BALLOONS.values(), BALLOONS);
            putAll(map, JocBlocks.SYMMETRIC_CROSS_SAILS.values(), SYMMETRIC_CROSS_SAILS);
            map.put(JocBlocks.SEEKER.get(), SEEKER);
            map.put(JocBlocks.SEEKER_TABLE.get(), SEEKER);
            map.put(JocBlocks.SHATTER_ASSEMBLER.get(), SHATTER_ASSEMBLER);
            map.put(JocBlocks.GYROSCOPIC_SEAT.get(), GYROSCOPIC_SEAT);
            blockFeatures = map;
        }
        return blockFeatures;
    }

    private static <T> void putAll(Map<T, String> map, Collection<? extends DeferredHolder<? super T, ? extends T>> holders, String feature) {
        for (DeferredHolder<? super T, ? extends T> holder : holders) {
            map.put(holder.get(), feature);
        }
    }
}
