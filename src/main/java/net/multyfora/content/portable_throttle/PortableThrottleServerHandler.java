package net.multyfora.content.portable_throttle;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

public class PortableThrottleServerHandler {

    public static final WorldAttached<Map<UUID, Map<Couple<Frequency>, ThrottleEntry>>> receivedInputs =
        new WorldAttached<>($ -> new HashMap<>());

    static final int TIMEOUT = 20;

    public static void tick(LevelAccessor world) {
        Map<UUID, Map<Couple<Frequency>, ThrottleEntry>> map = receivedInputs.get(world);
        for (Iterator<Map<Couple<Frequency>, ThrottleEntry>> iterator = map.values().iterator(); iterator.hasNext(); ) {
            Map<Couple<Frequency>, ThrottleEntry> entries = iterator.next();
            for (Iterator<ThrottleEntry> entryIterator = entries.values().iterator(); entryIterator.hasNext(); ) {
                ThrottleEntry throttleEntry = entryIterator.next();
                throttleEntry.decrement();
                if (!throttleEntry.isAlive()) {
                    entryIterator.remove();
                    removeTransmitter(world, throttleEntry);
                }
            }
            if (entries.isEmpty())
                iterator.remove();
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updatePlayer(player);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LevelAccessor world = event.getEntity().level();
        Map<Couple<Frequency>, ThrottleEntry> entries = receivedInputs.get(world).remove(event.getEntity().getUUID());
        if (entries == null) return;
        for (ThrottleEntry entry : entries.values()) {
            removeTransmitter(world, entry);
        }
    }

    public static void updatePlayer(ServerPlayer player) {
        LevelAccessor world = player.level();
        Map<Couple<Frequency>, Integer> wanted = new HashMap<>();
        if (!player.isSpectator()) {
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!(stack.getItem() instanceof PortableThrottleItem)) continue;
                int strength = PortableThrottleItem.getStrength(stack);
                if (strength <= 0) continue;
                Couple<Frequency> freq = PortableThrottleItem.getFrequency(stack, player.level().registryAccess());
                if (freq == null) continue;
                wanted.merge(freq, strength, Math::max);
            }
        }

        Map<UUID, Map<Couple<Frequency>, ThrottleEntry>> map = receivedInputs.get(world);
        Map<Couple<Frequency>, ThrottleEntry> entries = map.get(player.getUUID());
        if (entries == null) {
            if (wanted.isEmpty()) return;
            entries = new HashMap<>();
            map.put(player.getUUID(), entries);
        }

        for (Iterator<Entry<Couple<Frequency>, ThrottleEntry>> iterator = entries.entrySet().iterator(); iterator.hasNext(); ) {
            ThrottleEntry entry = iterator.next().getValue();
            if (!wanted.containsKey(entry.getSecond())) {
                iterator.remove();
                removeTransmitter(world, entry);
            }
        }

        BlockPos pos = player.blockPosition();
        for (Entry<Couple<Frequency>, Integer> want : wanted.entrySet()) {
            ThrottleEntry entry = entries.get(want.getKey());
            if (entry == null) {
                entry = new ThrottleEntry(pos, want.getKey(), want.getValue());
                entries.put(want.getKey(), entry);
                Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(world, entry);
                updateListeners(world, entry.getSecond());
                continue;
            }
            entry.setPos(pos);
            entry.setFirst(TIMEOUT);
            if (entry.getTransmittedStrength() != want.getValue()) {
                entry.setStrength(want.getValue());
                updateListeners(world, entry.getSecond());
            }
        }

        if (entries.isEmpty())
            map.remove(player.getUUID());
    }

    private static void removeTransmitter(LevelAccessor world, ThrottleEntry entry) {
        entry.setFirst(0);
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, entry);
        updateListeners(world, entry.getSecond());
    }

    private static void updateListeners(LevelAccessor world, Couple<Frequency> freq) {
        int power = 0;
        for (Map<Couple<Frequency>, ThrottleEntry> entries : receivedInputs.get(world).values()) {
            ThrottleEntry entry = entries.get(freq);
            if (entry != null) {
                power = Math.max(power, entry.getTransmittedStrength());
            }
        }

        Set<IRedstoneLinkable> network = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(world, new ThrottleEntry(BlockPos.ZERO, freq, 0));
        for (IRedstoneLinkable other : List.copyOf(network)) {
            if (!other.isAlive()) {
                continue;
            }
            if (other.isListening()) {
                other.setReceivedStrength(power);
            }
        }
    }

    static class ThrottleEntry extends Pair<Integer, Couple<Frequency>> implements IRedstoneLinkable {

        private BlockPos pos;
        private int strength;

        public ThrottleEntry(BlockPos pos, Couple<Frequency> second, int strength) {
            super(TIMEOUT, second);
            this.pos = pos;
            this.strength = strength;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        public void setStrength(int strength) {
            this.strength = strength;
        }

        public void setPos(BlockPos pos) {
            this.pos = pos;
        }

        public void decrement() {
            setFirst(getFirst() - 1);
        }

        @Override
        public int getTransmittedStrength() {
            return isAlive() ? strength : 0;
        }

        @Override
        public boolean isAlive() {
            return getFirst() > 0;
        }

        @Override
        public BlockPos getLocation() {
            return pos;
        }

        @Override
        public void setReceivedStrength(int power) {}

        @Override
        public boolean isListening() {
            return false;
        }

        @Override
        public Couple<Frequency> getNetworkKey() {
            return getSecond();
        }
    }
}
