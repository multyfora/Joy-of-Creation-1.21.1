package net.multyfora.index;

import dev.simulated_team.simulated.index.SimClickInteractions;
import net.multyfora.content.shatter_assembler.ShatterAssemblerGUIHandler;

public class JocClickInteractions {
    public static final ShatterAssemblerGUIHandler SHATTER_ASSEMBLER_MANAGER;

    static {
        SHATTER_ASSEMBLER_MANAGER = new ShatterAssemblerGUIHandler();
        SimClickInteractions.CLICK_INTERACTION_ENTRIES.add(SHATTER_ASSEMBLER_MANAGER);
    }
}
