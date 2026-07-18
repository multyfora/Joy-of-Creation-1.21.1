package net.multyfora.advancement;

import com.google.common.collect.Sets;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.index.JocItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class JocAdvancements implements DataProvider {

    public static final List<JocAdvancement> ENTRIES = new ArrayList<>();

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public JocAdvancements(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    public static final JocAdvancement ROOT = create("root", b -> b
        .icon(JocItems.SEEKER.get())
        .title("Seek the Truth")
        .description("Acquire a Seeker")
        .whenIconCollected()
    );

    public static final JocAdvancement SET_TARGET = create("set_target", b -> b
        .icon(Items.SPYGLASS)
        .title("Looking for something?")
        .description("Set a target in the Seeker")
        .after(ROOT)
    );

    public static final JocAdvancement ALL_SEEING_EYE = create("all_seeing_eye", b -> b
        .icon(Items.ENDER_EYE)
        .title("The All Seeing Eye")
        .description("Put an Eye of Ender in the Seeker")
        .after(ROOT)
    );

    public static final JocAdvancement INSEEKERPTION = create("inseekerption", b -> b
        .icon(Items.COMPASS)
        .title("Inseekerption")
        .description("Complete the recursion")
        .after(ROOT)
        .special(AdvancementType.CHALLENGE)
    );

    public static final JocAdvancement SEEKING_NOTHING = create("seeking_nothing", b -> b
        .icon(Items.SPYGLASS)
        .title("Seeking Nothing")
        .description("Lose the target sublevel")
        .after(SET_TARGET)
        .special(AdvancementType.CHALLENGE)
    );

    public static final JocAdvancement SIX_EYES = create("six_eyes", b -> b
        .icon(Items.ENDER_EYE)
        .title("Six Eyes")
        .description("use six eyes to see everything")
        .after(ALL_SEEING_EYE)
    );

    public static void register() {
        Object _reg = ROOT;
        Object _reg2 = SET_TARGET;
        Object _reg3 = ALL_SEEING_EYE;
        Object _reg4 = INSEEKERPTION;
        Object _reg5 = SEEKING_NOTHING;
        Object _reg6 = SIX_EYES;
    }

    public static JocAdvancement create(String id, Function<JocAdvancement, JocAdvancement> builder) {
        JocAdvancement advancement = new JocAdvancement(id, builder);
        ENTRIES.add(advancement);
        return advancement;
    }

    public static void provideLang(Consumer<String> consumer) {
        for (JocAdvancement advancement : ENTRIES) {
            consumer.accept(advancement.titleKey());
            consumer.accept(advancement.descKey());
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(provider -> {
            PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
            List<CompletableFuture<?>> futures = new ArrayList<>();
            Set<ResourceLocation> set = Sets.newHashSet();

            Consumer<AdvancementHolder> consumer = holder -> {
                ResourceLocation id = holder.id();
                if (!set.add(id)) {
                    throw new IllegalStateException("Duplicate advancement " + id);
                }
                java.nio.file.Path path = pathProvider.json(id);
                futures.add(DataProvider.saveStable(cache, provider, Advancement.CODEC, holder.value(), path));
            };

            for (JocAdvancement advancement : ENTRIES) {
                advancement.save(consumer, provider);
            }

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Advancements: " + AeronauticsJoyofcreation.MODID;
    }
}
