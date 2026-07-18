package net.multyfora.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;

import net.multyfora.advancement.criterion.CriterionTriggerBase;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SimpleJocTrigger extends CriterionTriggerBase<SimpleJocTrigger.Instance> {

    public SimpleJocTrigger(String id) {
        super(id);
    }

    public void trigger(net.minecraft.server.level.ServerPlayer player) {
        super.trigger(player, null);
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public static class Instance extends CriterionTriggerBase.Instance {

        private static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(instance, Instance::new));

        private final Optional<ContextAwarePredicate> player;

        public Instance(Optional<ContextAwarePredicate> player) {
            this.player = player;
        }

        @Override
        protected boolean test(@Nullable List<Supplier<Object>> suppliers) {
            return true;
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return player;
        }
    }
}
