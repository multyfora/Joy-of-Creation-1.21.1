package net.multyfora;

import net.multyfora.register.CreativeRegister;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;
import net.multyfora.index.JocMenuTypes;
import net.multyfora.config.JocConfig;

import static net.multyfora.register.CreativeRegister.registerCreativeModeTab;
import static net.multyfora.register.EventsRegister.registerEvents;
import static net.multyfora.register.PayloadRegister.RegisterPayloads;

@Mod(AeronauticsJoyofcreation.MODID)
public class AeronauticsJoyofcreation {
    public static final String MODID = "joc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    /**
     * Constructor: registers all mod content (blocks, items, block entity types, creative tab)
     * and sets up network packet handlers and tick listeners
     **/
    public AeronauticsJoyofcreation(IEventBus modEventBus, ModContainer modContainer) {
        // Register all content classes to trigger static initializers and populate deferred registers
        JocBlocks.register();
        JocItems.register();
        JocMenuTypes.register();

        // Register deferred registers with the mod event bus so NeoForge processes them
        JocBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        JocMenuTypes.MENU_TYPES.register(modEventBus);
        CreativeRegister.CREATIVE_MODE_TABS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        // Register common config
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, JocConfig.SPEC);

        // Final Registrations
        registerCreativeModeTab();
        registerEvents();
        RegisterPayloads(modEventBus);
    }
}
