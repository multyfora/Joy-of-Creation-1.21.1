package net.multyfora.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.advancement.JocAdvancement;
import net.multyfora.advancement.JocAdvancements;

public class JocLangProvider extends LanguageProvider {

    public JocLangProvider(PackOutput output) {
        super(output, AeronauticsJoyofcreation.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (JocAdvancement advancement : JocAdvancements.ENTRIES) {
            add(advancement.titleKey(), advancement.getTitle());
            add(advancement.descKey(), advancement.getDescription());
        }
    }
}
