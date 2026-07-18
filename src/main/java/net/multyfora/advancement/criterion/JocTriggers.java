package net.multyfora.advancement.criterion;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.LinkedList;
import java.util.List;

public class JocTriggers {

    private static final List<CriterionTriggerBase<?>> triggers = new LinkedList<>();

    public static SimpleJocTrigger addSimple(String id) {
        SimpleJocTrigger trigger = new SimpleJocTrigger(id);
        triggers.add(trigger);
        return trigger;
    }

    public static void register() {
        triggers.forEach(trigger ->
            Registry.register(BuiltInRegistries.TRIGGER_TYPES, trigger.getId(), trigger)
        );
    }
}
