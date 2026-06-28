package net.multyfora.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.multyfora.index.JocItems;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.multyfora.AeronauticsJoyofcreation.MODID;

public class CreativeRegister {
    // NeoForge DeferredRegister for blocks, items, and creative mode tabs, all using the mod ID
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Custom creative tab "JOC" placed before the Combat tab, showing all mod items
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> JOC_TAB = null;
    public static void registerCreativeModeTab() {
        JOC_TAB = CREATIVE_MODE_TABS.register(
            "joc_tab", () -> {
                return CreativeModeTab
                    .builder()
                    .title(Component.translatable("itemGroup.joc"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon( () -> new ItemStack( JocItems.BALLOON.get() ) )
                    .displayItems(
                        (parameters, output) -> {
                            output.accept( JocItems.PORTABLE_TYPEWRITER.get() );
                            output.accept( JocItems.PORTABLE_THROTTLE.get() );
                            output.accept( JocItems.BALLOON.get() );
                            output.accept( JocItems.COORD_NAV.get() );
                            output.accept( JocItems.PLAYER_DIRECTION.get() );
                        }
                    )
                    .build()
                ;
            }
        );
    }
}
