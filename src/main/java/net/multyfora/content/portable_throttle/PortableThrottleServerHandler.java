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

import org.slf4j.Logger;
import net.multyfora.AeronauticsJoyofcreation;

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

    private static final Logger LOGGER = AeronauticsJoyofcreation.LOGGER;

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
                    LOGGER.info("[THROTTLE_SERVER] tick: EXPIRING entry uuid={} freq=({}|{}) strength={} timeout={}",
                        uid,
                        throttleEntry.getSecond().getFirst().getStack().getHoverName().getString(),
                        throttleEntry.getSecond().getSecond().getStack().getHoverName().getString(),
                        throttleEntry.strength, timeout);
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
        if (totalRemoved > 0) {
            LOGGER.info("[THROTTLE_SERVER] tick: removed {} expired entries", totalRemoved);
        }
    }

    /**
     * Forces all listeners on the given frequency to update their received strength.
     * Iterates the network set and calls setReceivedStrength on each alive, listening node.
     **/
    private static void forceUpdateListeners(LevelAccessor world, Couple<Frequency> freq, int power) {
        Set<IRedstoneLinkable> network = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(world, new ThrottleEntry(BlockPos.ZERO, freq, power));
        LOGGER.info("[THROTTLE_SERVER] forceUpdateListeners: freq=({}|{}) power={} network size={}",
            freq.getFirst().getStack().getHoverName().getString(),
            freq.getSecond().getStack().getHoverName().getString(),
            power, network.size());

        int total = 0;
        int dead = 0;
        int updated = 0;

        Iterator<IRedstoneLinkable> it = network.iterator();
        while (it.hasNext()) {
            IRedstoneLinkable other = it.next();
            total++;
            if (!other.isAlive()) {
                LOGGER.info("[THROTTLE_SERVER] forceUpdateListeners: removing DEAD entry class={}", other.getClass().getSimpleName());
                it.remove();
                dead++;
                continue;
            }
            if (other.isListening()) {
                LOGGER.info("[THROTTLE_SERVER] forceUpdateListeners: UPDATING listener class={} old_str={} new_power={} loc={}",
                    other.getClass().getSimpleName(), other.getTransmittedStrength(), power, other.getLocation());
                other.setReceivedStrength(power);
                updated++;
            } else {
                LOGGER.info("[THROTTLE_SERVER] forceUpdateListeners: SKIPPING non-listener class={} strength={} loc={}",
                    other.getClass().getSimpleName(), other.getTransmittedStrength(), other.getLocation());
            }
        }
        LOGGER.info("[THROTTLE_SERVER] forceUpdateListeners: total={} dead={} updated={}", total, dead, updated);
        if (updated == 0) {
            LOGGER.warn("[THROTTLE_SERVER] forceUpdateListeners: ZERO listeners updated! Network may be out of sync with link blocks for this frequency!");
        }
        if (total - dead == 0) {
            LOGGER.warn("[THROTTLE_SERVER] forceUpdateListeners: Network set EMPTY after dead cleanup! No link blocks in network for this frequency!");
        }
    }

    /**
     * Called when a throttle signal packet is received from a client.
     * Creates or updates a ThrottleEntry for the given player UUID and frequency,
     * and propagates the signal to all listeners on that frequency.
     **/
    public static void receiveSignal(LevelAccessor world, BlockPos pos, UUID uniqueID, Couple<Frequency> freq, int strength) {
        LOGGER.info("[THROTTLE_SERVER] receiveSignal ENTER: uuid={} freq=({}|{}) strength={} pos={}", uniqueID,
            freq.getFirst().getStack().getHoverName().getString(),
            freq.getSecond().getStack().getHoverName().getString(),
            strength, pos);

        Map<UUID, Collection<ThrottleEntry>> map = receivedInputs.get(world);
        Collection<ThrottleEntry> list = map.computeIfAbsent(uniqueID, $ -> {
            LOGGER.info("[THROTTLE_SERVER] receiveSignal: created new list for uuid={}", uniqueID);
            return new ArrayList<>();
        });

        // Find an existing entry for this frequency
        ThrottleEntry existing = null;
        for (ThrottleEntry entry : list) {
            if (entry.getSecond().equals(freq)) {
                existing = entry;
                LOGGER.info("[THROTTLE_SERVER] receiveSignal: found existing entry, oldStrength={} oldTimeout={}", entry.strength, entry.getFirst());
                break;
            }
        }

        // If strength is 0 or less, remove the entry and set power to 0
        if (strength <= 0) {
            LOGGER.info("[THROTTLE_SERVER] receiveSignal: strength<=0, removing entry. hasExisting={}", existing != null);
            if (existing != null) {
                forceUpdateListeners(world, freq, 0);
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, existing);
                list.remove(existing);
                LOGGER.info("[THROTTLE_SERVER] receiveSignal: removed entry from network and list");
            }
            LOGGER.info("[THROTTLE_SERVER] receiveSignal EXIT (strength<=0)");
            return;
        }

        // Update existing entry or create a new one
        if (existing != null) {
            existing.setStrength(strength);
            existing.setFirst(TIMEOUT);
            LOGGER.info("[THROTTLE_SERVER] receiveSignal: UPDATED existing entry -> strength={} timeout={}", strength, TIMEOUT);
        } else {
            existing = new ThrottleEntry(pos, freq, strength);
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(world, existing);
            list.add(existing);
            LOGGER.info("[THROTTLE_SERVER] receiveSignal: CREATED new entry at pos={} strength={} timeout={}", pos, strength, TIMEOUT);
        }

        forceUpdateListeners(world, freq, strength);
        LOGGER.info("[THROTTLE_SERVER] receiveSignal EXIT (normal)");
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
