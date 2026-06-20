package net.multyfora;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocBlocks;
import net.multyfora.index.JocItems;
import net.multyfora.network.CoordNavPayloads;
import net.multyfora.network.PortableTypewriterInputPacket;
import net.multyfora.network.PortableTypewriterBindPacket;
import net.multyfora.network.PortableThrottleBindPacket;
import net.multyfora.network.PortableThrottleConfigPacket;
import net.multyfora.network.PortableThrottleSignalPacket;
import net.multyfora.content.coordnav.CoordNavBlockEntity;
import net.multyfora.content.portable_typewriter.PortableTypewriterServerHandler;
import net.multyfora.content.portable_throttle.PortableThrottleServerHandler;

@Mod(AeronauticsJoyofcreation.MODID)
public class AeronauticsJoyofcreation {
    // Unique identifier for this mod, used in registries, resource locations, and network channels
    public static final String MODID = "joc";
    // SLF4J logger instance for mod-wide debug and info logging
    public static final Logger LOGGER = LogUtils.getLogger();

    // NeoForge DeferredRegister for blocks, items, and creative mode tabs, all using the mod ID
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Custom creative tab "JOC" placed before the Combat tab, showing all mod items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JOC_TAB = CREATIVE_MODE_TABS.register("joc_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.joc"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> new ItemStack(JocItems.BALLOON.get()))
            .displayItems((parameters, output) -> {
                output.accept(JocItems.BALLOON.get());
                output.accept(JocItems.COORD_NAV.get());
                output.accept(JocItems.PORTABLE_TYPEWRITER.get());
                output.accept(JocItems.PORTABLE_THROTTLE.get());
            }).build());

    // Constructor: registers all mod content (blocks, items, block entity types, creative tab)
    // and sets up network packet handlers and tick listeners
    public AeronauticsJoyofcreation(IEventBus modEventBus, ModContainer modContainer) {
        // Register all content classes to trigger static initialisers and populate deferred registers
        JocBlocks.register();
        JocItems.register();
        // Register deferred registers with the mod event bus so NeoForge processes them
        JocBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        // Server tick listener: ticks the PortableTypewriter and PortableThrottle server handlers
        // each level tick to manage timeouts and signal decay
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (!event.getLevel().isClientSide()) {
                PortableTypewriterServerHandler.tick(event.getLevel());
                PortableThrottleServerHandler.tick(event.getLevel());
            }
        });

        // Register all custom network payloads (play-to-client and play-to-server)
        modEventBus.addListener(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent.class, event -> {
            var registrar = event.registrar("1.0.0");
            // Client-bound: opens the CoordNavScreen GUI when the server tells the client to
            registrar.playToClient(
                    CoordNavPayloads.OpenCoordNavPayload.TYPE,
                    CoordNavPayloads.OpenCoordNavPayload.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(() -> {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                            if (mc.level != null) {
                                mc.setScreen(new net.multyfora.client.coordnav.CoordNavScreen(payload.pos()));
                            }
                        });
                    }
            );
            // Server-bound: updates the CoordNavBlockEntity's target coordinates from the GUI
            registrar.playToServer(
                    CoordNavPayloads.UpdateCoordPayload.TYPE,
                    CoordNavPayloads.UpdateCoordPayload.CODEC,
                    (payload, context) -> {
                        context.enqueueWork(() -> {
                            var level = context.player().level();
                            if (level != null && level.getBlockEntity(payload.pos()) instanceof CoordNavBlockEntity be) {
                                be.setTarget(payload.x(), payload.y(), payload.z());
                            }
                        });
                    }
            );
            // Server-bound: handles typewriter key press/release input
            registrar.playToServer(
                    net.multyfora.network.PortableTypewriterInputPacket.TYPE,
                    net.multyfora.network.PortableTypewriterInputPacket.CODEC,
                    (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
            );
            // Server-bound: binds a keyboard key to a redstone link frequency for the typewriter
            registrar.playToServer(
                    net.multyfora.network.PortableTypewriterBindPacket.TYPE,
                    net.multyfora.network.PortableTypewriterBindPacket.CODEC,
                    (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
            );
            // Server-bound: binds the throttle item to a redstone link's frequency
            registrar.playToServer(
                    PortableThrottleBindPacket.TYPE,
                    PortableThrottleBindPacket.CODEC,
                    (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
            );
            // Server-bound: configures the throttle's frequency items
            registrar.playToServer(
                    PortableThrottleConfigPacket.TYPE,
                    PortableThrottleConfigPacket.CODEC,
                    (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
            );
            // Server-bound: sends throttle signal strength to the server
            registrar.playToServer(
                    PortableThrottleSignalPacket.TYPE,
                    PortableThrottleSignalPacket.CODEC,
                    (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
            );
        });
    }
}
