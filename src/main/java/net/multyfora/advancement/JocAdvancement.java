package net.multyfora.advancement;

import com.google.common.collect.Sets;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.advancement.criterion.JocTriggers;
import net.multyfora.advancement.criterion.SimpleJocTrigger;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class JocAdvancement {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "textures/gui/advancements.png");

    private final ResourceLocation id;
    private String title;
    private String description;
    private Supplier<ItemStack> iconSupplier;
    private ItemStack icon;
    private JocAdvancement parent;
    private AdvancementType type = AdvancementType.TASK;
    private boolean externalTrigger;
    private String customName;
    private SimpleJocTrigger builtinTrigger;

    public JocAdvancement(String id, Function<JocAdvancement, JocAdvancement> builder) {
        this.id = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, id);
        builder.apply(this);
        if (!externalTrigger) {
            builtinTrigger = JocTriggers.addSimple(id + "_builtin");
        }
    }

    public JocAdvancement icon(ItemStack stack) {
        this.iconSupplier = () -> stack;
        return this;
    }

    public JocAdvancement icon(ItemLike item) {
        this.iconSupplier = () -> new ItemStack(item);
        return this;
    }

    public JocAdvancement title(String title) {
        this.title = title;
        return this;
    }

    public JocAdvancement description(String description) {
        this.description = description;
        return this;
    }

    public JocAdvancement after(JocAdvancement parent) {
        this.parent = parent;
        return this;
    }

    public JocAdvancement special(AdvancementType type) {
        this.type = type;
        return this;
    }

    public JocAdvancement whenIconCollected() {
        this.externalTrigger = true;
        return this;
    }

    public JocAdvancement whenItemCollectedWithCustomName(String name) {
        this.externalTrigger = true;
        this.customName = name;
        return this;
    }

    public JocAdvancement awardedForFree() {
        this.externalTrigger = true;
        return this;
    }

    public void awardTo(ServerPlayer player) {
        if (builtinTrigger != null) {
            builtinTrigger.trigger(player);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public AdvancementHolder save(Consumer<AdvancementHolder> consumer, HolderLookup.Provider registries) {
        if (icon == null) {
            icon = iconSupplier != null ? iconSupplier.get() : ItemStack.EMPTY;
        }
        if (icon == null || icon.isEmpty()) {
            throw new IllegalStateException("No icon defined for advancement " + id);
        }

        DisplayInfo display = new DisplayInfo(
            icon,
            Component.translatable(titleKey()),
            Component.translatable(descKey()),
            Optional.ofNullable(parent == null ? BACKGROUND : null),
            type,
            true,
            type == AdvancementType.GOAL || type == AdvancementType.CHALLENGE,
            type == AdvancementType.CHALLENGE
        );

        Criterion<?> criterion;
        if (!externalTrigger) {
            criterion = new Criterion<>(builtinTrigger, new SimpleJocTrigger.Instance(Optional.empty()));
        } else {
            if (customName != null) {
                criterion = InventoryChangeTrigger.TriggerInstance.hasItems(
                    ItemPredicate.Builder.item()
                        .of(icon.getItem())
                        .hasComponents(
                            DataComponentPredicate.builder()
                                .expect(DataComponents.CUSTOM_NAME, Component.literal(customName))
                                .build()
                        )
                );
            } else if (icon != null && !icon.isEmpty()) {
                criterion = InventoryChangeTrigger.TriggerInstance.hasItems(icon.getItem());
            } else {
                criterion = InventoryChangeTrigger.TriggerInstance.hasItems(new ItemPredicate[0]);
            }
        }

        AdvancementRequirements requirements = AdvancementRequirements.allOf(Sets.newHashSet("0"));
        Advancement advancement = new Advancement(
            Optional.ofNullable(parent == null ? null : parent.id),
            Optional.of(display),
            AdvancementRewards.EMPTY,
            java.util.Map.of("0", criterion),
            requirements,
            false
        );

        AdvancementHolder holder = new AdvancementHolder(id, advancement);
        consumer.accept(holder);
        return holder;
    }

    public String titleKey() {
        return "advancement." + AeronauticsJoyofcreation.MODID + "." + id.getPath();
    }

    public String descKey() {
        return titleKey() + ".desc";
    }

    public ResourceLocation getId() {
        return id;
    }
}
