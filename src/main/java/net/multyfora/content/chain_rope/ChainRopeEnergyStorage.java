package net.multyfora.content.chain_rope;

import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import net.multyfora.IChainRopeStrand;
import net.multyfora.IMultiRopeBehavior;
import net.multyfora.config.JocFeatures;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChainRopeEnergyStorage implements IEnergyStorage {
    private static boolean transferring = false;

    private final RopeConnectorBlockEntity connector;
    @Nullable private final Direction side;

    public ChainRopeEnergyStorage(RopeConnectorBlockEntity connector, @Nullable Direction side) {
        this.connector = connector;
        this.side = side;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SimBlockEntityTypes.ROPE_CONNECTOR.get(), ChainRopeEnergyStorage::new);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (transferring || maxReceive <= 0 || !JocFeatures.isEnabled(JocFeatures.CHAIN_ROPE)
                || !(connector.getLevel() instanceof ServerLevel level)) {
            return 0;
        }

        Set<BlockPos> connectors = findConnectedConnectors(level);
        if (connectors.size() < 2) {
            return 0;
        }

        transferring = true;
        try {
            List<IEnergyStorage> receivers = findReceivers(level, connectors);
            int remaining = maxReceive;
            for (int i = 0; i < receivers.size() && remaining > 0; i++) {
                int left = receivers.size() - i;
                remaining -= receivers.get(i).receiveEnergy((remaining + left - 1) / left, simulate);
            }
            return maxReceive - remaining;
        } finally {
            transferring = false;
        }
    }

    private Set<BlockPos> findConnectedConnectors(ServerLevel level) {
        ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(level);
        Set<BlockPos> found = new LinkedHashSet<>();
        Deque<RopeConnectorBlockEntity> open = new ArrayDeque<>();
        found.add(connector.getBlockPos());
        open.add(connector);

        while (!open.isEmpty()) {
            for (ServerRopeStrand strand : getAttachedChains(manager, open.poll())) {
                for (RopeAttachment attachment : strand.getAttachments()) {
                    BlockPos pos = attachment.blockAttachment();
                    if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RopeConnectorBlockEntity next && found.add(pos)) {
                        open.add(next);
                    }
                }
            }
        }
        return found;
    }

    private static List<ServerRopeStrand> getAttachedChains(ServerLevelRopeManager manager, RopeConnectorBlockEntity connector) {
        List<ServerRopeStrand> chains = new ArrayList<>();
        if (connector.getRopeHolder() instanceof IMultiRopeBehavior holder) {
            Set<UUID> ids = new LinkedHashSet<>(holder.joc$getAllAttachedRopeIDs());
            if (holder.joc$getAttachedRopeID() != null) {
                ids.add(holder.joc$getAttachedRopeID());
            }
            for (UUID id : ids) {
                if (manager.getStrand(id) instanceof IChainRopeStrand chain && chain.joc$isChain()) {
                    chains.add(manager.getStrand(id));
                }
            }
        }
        return chains;
    }

    private List<IEnergyStorage> findReceivers(ServerLevel level, Set<BlockPos> connectors) {
        List<IEnergyStorage> receivers = new ArrayList<>();
        for (BlockPos pos : connectors) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = pos.relative(direction);
                boolean source = pos.equals(connector.getBlockPos()) && direction == side;
                if (source || connectors.contains(neighbour) || !level.isLoaded(neighbour)) {
                    continue;
                }

                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbour, direction.getOpposite());
                if (storage != null && storage.canReceive()) {
                    receivers.add(storage);
                }
            }
        }
        return receivers;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
