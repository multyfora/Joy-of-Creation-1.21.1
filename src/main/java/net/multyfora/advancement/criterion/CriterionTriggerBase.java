package net.multyfora.advancement.criterion;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import net.multyfora.AeronauticsJoyofcreation;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public abstract class CriterionTriggerBase<T extends CriterionTriggerBase.Instance> implements net.minecraft.advancements.CriterionTrigger<T> {

    private final ResourceLocation id;
    protected final java.util.Map<PlayerAdvancements, Set<Listener<T>>> listeners = new java.util.HashMap<>();

    public CriterionTriggerBase(String id) {
        this.id = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, id);
    }

    @Override
    public void addPlayerListener(PlayerAdvancements advancements, Listener<T> listener) {
        listeners.computeIfAbsent(advancements, k -> new java.util.HashSet<>()).add(listener);
    }

    @Override
    public void removePlayerListener(PlayerAdvancements advancements, Listener<T> listener) {
        Set<Listener<T>> playerListeners = listeners.get(advancements);
        if (playerListeners != null) {
            playerListeners.remove(listener);
            if (playerListeners.isEmpty()) {
                listeners.remove(advancements);
            }
        }
    }

    @Override
    public void removePlayerListeners(PlayerAdvancements advancements) {
        listeners.remove(advancements);
    }

    protected void trigger(ServerPlayer player, @Nullable List<Supplier<Object>> suppliers) {
        PlayerAdvancements pa = player.getAdvancements();
        Set<Listener<T>> playerListeners = listeners.get(pa);
        if (playerListeners != null) {
            List<Listener<T>> list = new LinkedList<>();
            for (Listener<T> listener : playerListeners) {
                if (listener.trigger().test(suppliers)) {
                    list.add(listener);
                }
            }
            list.forEach(listener -> listener.run(pa));
        }
    }

    public ResourceLocation getId() {
        return id;
    }

    public abstract static class Instance implements SimpleCriterionTrigger.SimpleInstance {

        protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);

        public abstract Optional<ContextAwarePredicate> player();
    }
}
