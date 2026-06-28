package net.multyfora.content.portable_throttle;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import static net.multyfora.AeronauticsJoyofcreation.LOGGER;

/**
 * Server-side handler for Portable Throttle signals.
 * Manages a per-world map of active throttle entries (ThrottleEntry) keyed by player UUID.
 * Each entry has a timeout counter that decrements every tick; expired entries are removed
 * and set the frequency's redstone power to 0. Keepalive packets from the client reset
 * the timeout to keep the signal alive.
 **/
public class PortableThrottleServerHandler {

    // Per-world map: player UUID -> collection of active throttle entries (different frequencies)
    public static final WorldAttached<Map<UUID, Collection<ThrottleEntry>>> receivedInputs =
        new WorldAttached<>($ -> new HashMap<>());

    // Timeout in ticks: after this many ticks without a keepalive, the signal is removed
    static final int TIMEOUT = 200;

    // Ticked every server level tick: decrements all throttle timeouts and removes expired entries
    public static void tick(LevelAccessor world) {
        Map<UUID, Collection<ThrottleEntry>> map = receivedInputs.get(world);
        int totalRemoved = 0;
        for (Iterator<Entry<UUID, Collection<ThrottleEntry>>> iterator = map.entrySet()
            .iterator(); iterator.hasNext(); ) {

            Entry<UUID, Collection<ThrottleEntry>> entry = iterator.next();
            UUID uid = entry.getKey();
            Collection<ThrottleEntry> list = entry.getValue();

            for (Iterator<ThrottleEntry> entryIterator = list.iterator(); entryIterator.hasNext(); ) {
                ThrottleEntry throttleEntry = entryIterator.next();
                throttleEntry.decrement();
                int timeout = throttleEntry.getFirst();
                if (!throttleEntry.isAlive()) {
                    Couple<Frequency> freq = throttleEntry.getSecond();
                    // Set power to 0 and remove from the network
                    forceUpdateListeners(world, freq, 0);
                    Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, throttleEntry);
                    entryIterator.remove();
                    totalRemoved++;
                }
            }

            if (list.isEmpty())
                iterator.remove();
        }
    }

    /**
     * Forces all listeners on the given frequency to update their received strength.
     * Iterates the network set and calls setReceivedStrength on each alive, listening node.
     **/
    private static void forceUpdateListeners(LevelAccessor world, Couple<Frequency> freq, int power) {
        Set<IRedstoneLinkable> network = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(world, new ThrottleEntry(BlockPos.ZERO, freq, power));

        int total = 0;
        int dead = 0;
        int updated = 0;

        Iterator<IRedstoneLinkable> it = network.iterator();
        while (it.hasNext()) {
            IRedstoneLinkable other = it.next();
            total++;
            if (!other.isAlive()) {
                it.remove();
                dead++;
                continue;
            }
            if (other.isListening()) {
                other.setReceivedStrength(power);
                updated++;
            }
        }
    }

    /**
     * Called when a throttle signal packet is received from a client.
     * Creates or updates a ThrottleEntry for the given player UUID and frequency,
     * and propagates the signal to all listeners on that frequency.
     **/
    public static void receiveSignal(LevelAccessor world, BlockPos pos, UUID uniqueID, Couple<Frequency> freq, int strength) {

        Map<UUID, Collection<ThrottleEntry>> map = receivedInputs.get(world);
        Collection<ThrottleEntry> list = map.computeIfAbsent(uniqueID, $ -> {
            return new ArrayList<>();
        });

        // Find an existing entry for this frequency
        ThrottleEntry existing = null;
        for (ThrottleEntry entry : list) {
            if (entry.getSecond().equals(freq)) {
                existing = entry;
                break;
            }
        }

        // If strength is 0 or less, remove the entry and set power to 0
        if (strength <= 0) {
            if (existing != null) {
                forceUpdateListeners(world, freq, 0);
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, existing);
                list.remove(existing);
            }
            return;
        }

        // Update existing entry or create a new one
        if (existing != null) {
            existing.setStrength(strength);
            existing.setFirst(TIMEOUT);
        } else {
            existing = new ThrottleEntry(pos, freq, strength);
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(world, existing);
            list.add(existing);
        }

        forceUpdateListeners(world, freq, strength);
    }

    /**
     * Represents an active throttle signal in the redstone link network.
     * Extends Pair<Integer, Couple<Frequency>> where first = timeout ticks remaining
     * and second = the frequency pair. Implements IRedstoneLinkable to integrate with
     * Create's redstone link network system.
     **/
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
            if (this == obj) return true;
            if (!(obj instanceof ThrottleEntry other)) return false;
            // Equality based on frequency only (so we can find matching entries by frequency)
            return getSecond().equals(other.getSecond());
        }

        @Override
        public int hashCode() {
            return getSecond().hashCode();
        }

        public void setStrength(int strength) {
            this.strength = strength;
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
