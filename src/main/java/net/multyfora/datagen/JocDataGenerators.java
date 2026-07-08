package net.multyfora.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.multyfora.AeronauticsJoyofcreation;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = AeronauticsJoyofcreation.MODID)
public class JocDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(),
                new JocBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(),
                new JocItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(),
                new JocLootTableProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(),
                new JocBlockTagsProvider(packOutput, lookupProvider, existingFileHelper));
    }
}