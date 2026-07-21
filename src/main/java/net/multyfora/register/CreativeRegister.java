package net.multyfora.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import static net.multyfora.AeronauticsJoyofcreation.MODID;

public class CreativeRegister {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> JOC_TAB = null;

    public static void registerCreativeModeTab() {
        JOC_TAB = CREATIVE_MODE_TABS.register(
                "joc_tab", () -> {
                    return CreativeModeTab
                            .builder()
                            .title(Component.translatable("itemGroup.joc"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon( () -> new ItemStack( JocBlocks.BALLOONS.get(DyeColor.WHITE).get() ) )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept( JocItems.PORTABLE_TYPEWRITER.get() );
                                        output.accept( JocItems.PORTABLE_THROTTLE.get() );
                                        for (DyeColor color : DyeColor.values()) {
                                            output.accept( JocBlocks.BALLOON_ITEMS.get(color).get() );
                                        }
                                        output.accept( JocItems.SEEKER.get() );
                                        output.accept( JocItems.SHATTER_ASSEMBLER.get() );
                                        output.accept( JocItems.GYROSCOPIC_SEAT.get() );
                                        output.accept( JocItems.DEFLATED_BALLOON.get() );

                                        for (DyeColor color : DyeColor.values()) {
                                            output.accept( JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get() );
                                        }
                                    }
                            )
                            .build()
                            ;
                }
        );
    }
}