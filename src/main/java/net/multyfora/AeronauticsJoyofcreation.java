package net.multyfora;

import net.multyfora.index.*;
import net.multyfora.ponder.JocPonderPlugin;
import net.multyfora.register.CreativeRegister;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.multyfora.config.JocConfig;
import net.multyfora.content.crosssail.SymmetricCrossSailBlock; // ADDED THIS

// CREATE API IMPORTS
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

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
        JocSounds.register(modEventBus);
        JocDataComponents.register(modEventBus);

        // Register deferred registers with the mod event bus so NeoForge processes them
        JocBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        JocEntityTypes.ENTITY_TYPES.register(modEventBus);
        JocMenuTypes.MENU_TYPES.register(modEventBus);
        CreativeRegister.CREATIVE_MODE_TABS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        // Register common config
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, JocConfig.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // Final Registrations
        registerCreativeModeTab();
        registerEvents();
        RegisterPayloads(modEventBus);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new JocPonderPlugin()));
    }

    // this is for registering the cross sail to autoconnect when assembling bearings
    private void commonSetup(final FMLCommonSetupEvent event) {
        BlockMovementChecks.registerMovementNecessaryCheck((state, world, pos) -> {
            if (state.getBlock() instanceof SymmetricCrossSailBlock) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (state.getBlock() instanceof SymmetricCrossSailBlock) {
                BlockState adjacentState = world.getBlockState(pos.relative(direction));

                if (adjacentState.getBlock() instanceof SymmetricCrossSailBlock) {
                    if (adjacentState.getValue(RotatedPillarBlock.AXIS) == state.getValue(RotatedPillarBlock.AXIS)) {
                        return BlockMovementChecks.CheckResult.SUCCESS;
                    }
                }
                String blockRegistryName = adjacentState.getBlock().getClass().getName().toLowerCase();
                if (blockRegistryName.contains("bearing")) {
                    return BlockMovementChecks.CheckResult.SUCCESS;
                }
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
    }
}