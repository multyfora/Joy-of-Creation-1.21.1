package net.multyfora.ponder;

import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class JocPonderTags {
    public static final ResourceLocation JOC_AERONAUTICS =
            ResourceLocation.fromNamespaceAndPath("joc", "aeronautics");

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(JOC_AERONAUTICS)
                .item(Items.FEATHER)
                .title("Aeronautics Components")
                .description("Components for airships and aeronautical contraptions")
                .addToIndex();
    }
}
