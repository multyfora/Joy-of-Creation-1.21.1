package net.multyfora.content.portable_typewriter;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Server-side handler for Portable Typewriter key press/release events.
 * Maintains a per-world map of active frequency entries keyed by player UUID.
 * Each entry represents a pressed key's frequency and has a short timeout.
 * Pressed keys output full strength (15) and are removed either by release or timeout.
 **/
public class PortableTypewriterServerHandler {

    // Per-world map: player UUID -> collection of active (pressed) frequency entries
    public static final WorldAttached<Map<UUID, Collection<ManualFrequencyEntry>>> receivedInputs =
        new WorldAttached<>($ -> new HashMap<>());

    // Timeout in ticks: a pressed key auto-releases after this many ticks without a refresh
    static final int TIMEOUT = 30;

    // Server tick: decrements all entry timeouts and removes expired ones from the network
    public static void tick(LevelAccessor world) {
        Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
        for (Iterator<Entry<UUID, Collection<ManualFrequencyEntry>>> iterator = map.entrySet()
            .iterator(); iterator.hasNext(); ) {

            Entry<UUID, Collection<ManualFrequencyEntry>> entry = iterator.next();
            Collection<ManualFrequencyEntry> list = entry.getValue();

            for (Iterator<ManualFrequencyEntry> entryIterator = list.iterator(); entryIterator.hasNext(); ) {
                ManualFrequencyEntry manualFrequencyEntry = entryIterator.next();
                manualFrequencyEntry.decrement();
                if (!manualFrequencyEntry.isAlive()) {
                    // Remove expired entry from the redstone link network
                    Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, manualFrequencyEntry);
                    entryIterator.remove();
                }
            }

            if (list.isEmpty())
                iterator.remove();
        }
    }

    /**
     * Called when key press/release packets arrive from clients.
     * For each activated frequency: finds an existing entry to update or creates a new one.
     * If pressed is false, the entry is marked for immediate expiration (timeout set to 0).
     **/
    public static void receivePressed(LevelAccessor world, BlockPos pos, UUID uniqueID, Collection<Couple<Frequency>> activated, boolean pressed) {
        Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
        Collection<ManualFrequencyEntry> list = map.computeIfAbsent(uniqueID, $ -> new ArrayList<>());

        for (Couple<Frequency> freq : activated) {
            boolean found = false;
            for (ManualFrequencyEntry existing : list) {
                if (existing.getSecond().equals(freq)) {
                    if (!pressed)
                        existing.setFirst(0); // Mark for immediate removal
                    else
                        existing.updatePosition(pos); // Refresh timeout
                    found = true;
                    break;
                }
            }
            if (found) continue;
            if (!pressed) continue; // No existing entry to release

            // Create a new entry for a freshly pressed key
            ManualFrequencyEntry entry = new ManualFrequencyEntry(pos, freq);
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(world, entry);
            list.add(entry);
        }
    }

    /**
     * Represents an active key press in the redstone link network.
     * Extends IntAttached<Couple<Frequency>> where the int is the timeout counter.
     * Implements IRedstoneLinkable to integrate with Create's network system.
     * Always transmits full strength (15) while alive.
     **/
    static class ManualFrequencyEntry extends IntAttached<Couple<Frequency>> implements IRedstoneLinkable {

        private BlockPos pos;

        public ManualFrequencyEntry(BlockPos pos, Couple<Frequency> second) {
            super(TIMEOUT, second);
            this.pos = pos;
        }

        // Refreshes the position and resets the timeout to the initial value
        public void updatePosition(BlockPos pos) {
            this.pos = pos;
            setFirst(TIMEOUT);
        }

        @Override
        public int getTransmittedStrength() {
            return isAlive() ? 15 : 0;
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
