package net.multyfora.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.client.FreqScreenMenu;

public class JocMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, AeronauticsJoyofcreation.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<FreqScreenMenu>> TYPEWRITER_SCREEN =
            MENU_TYPES.register("typewriter_screen",
                    () -> IMenuTypeExtension.create((id, inv, buf) -> new FreqScreenMenu(id, inv, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<FreqScreenMenu>> THROTTLE_SCREEN =
            MENU_TYPES.register("throttle_screen",
                    () -> IMenuTypeExtension.create((id, inv, buf) -> new FreqScreenMenu(id, inv, buf)));

    public static void register() {}
}
