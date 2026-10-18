package net.multyfora.content.portable_throttle;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import static net.multyfora.AeronauticsJoyofcreation.LOGGER;

public class PortableThrottleServerHandler {

    public static final WorldAttached<Map<UUID, Collection<ThrottleEntry>>> receivedInputs =
        new WorldAttached<>($ -> new HashMap<>());

    private static final Map<UUID, Long> lastSyncTick = new HashMap<>();

    static final int TIMEOUT = 200;

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
                    forceUpdateListeners(world, freq, 0);
                    Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, throttleEntry);
                    entryIterator.remove();
                    totalRemoved++;
                }
            }

            if (list.isEmpty())
                iterator.remove();
        }
        lastSyncTick.keySet().removeIf(uid -> !map.containsKey(uid));
    }

    private static void forceUpdateListeners(LevelAccessor world, Couple<Frequency> freq, int power) {
        Set<IRedstoneLinkable> network = Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(world, new ThrottleEntry(BlockPos.ZERO, freq, power));

        for (IRedstoneLinkable other : List.copyOf(network)) {
            if (!other.isAlive()) {
                continue;
            }
            if (other.isListening()) {
                other.setReceivedStrength(power);
            }
        }
    }

    public static void receiveSignal(LevelAccessor world, BlockPos pos, UUID uniqueID, Couple<Frequency> freq, int strength) {

        Map<UUID, Collection<ThrottleEntry>> map = receivedInputs.get(world);
        Collection<ThrottleEntry> list = map.computeIfAbsent(uniqueID, $ -> {
            return new ArrayList<>();
        });

        ThrottleEntry existing = null;
        for (ThrottleEntry entry : list) {
            if (entry.getSecond().equals(freq)) {
                existing = entry;
                break;
            }
        }

        if (strength <= 0) {
            if (existing != null) {
                forceUpdateListeners(world, freq, 0);
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(world, existing);
                list.remove(existing);
            }
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.getPlayerByUUID(uniqueID);
            }
            return;
        }

        if (existing != null) {
            existing.setStrength(strength);
            existing.setFirst(TIMEOUT);
        } else {
            existing = new ThrottleEntry(pos, freq, strength);
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(world, existing);
            list.add(existing);
        }

        forceUpdateListeners(world, freq, strength);

        if (world instanceof ServerLevel serverLevel) {
            long gameTime = serverLevel.getGameTime();
            Long lastSync = lastSyncTick.get(uniqueID);
            if (lastSync == null || gameTime - lastSync >= 2) {
                if (serverLevel.getPlayerByUUID(uniqueID) instanceof ServerPlayer) {
                    lastSyncTick.put(uniqueID, gameTime);
                }
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
            if (this == obj) return true;
            if (!(obj instanceof ThrottleEntry other)) return false;
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
