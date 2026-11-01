package net.multyfora.content.seeker;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.multyfora.index.JocItems;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class SeekerLinkBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<SeekerLinkBehaviour> TYPE = new BehaviourType<>();
    public static final int FREQUENCIES_PER_SIDE = 2;

    private final SeekerBlockEntity seeker;
    private final Map<Direction, Transmitter> transmitters = new EnumMap<>(Direction.class);

    private final ItemStackHandler core = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(JocItems.LOGIC_CORE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (isServer()) {
                refreshTransmitters();
                seeker.onLogicCoreChanged();
            }
        }
    };

    private final ItemStackHandler frequencies = new ItemStackHandler(Direction.values().length * FREQUENCIES_PER_SIDE) {
        @Override
        protected void onContentsChanged(int slot) {
            if (isServer()) {
                refreshTransmitter(Direction.values()[slot / FREQUENCIES_PER_SIDE]);
                seeker.setChanged();
                seeker.sendData();
            }
        }
    };

    public SeekerLinkBehaviour(SeekerBlockEntity seeker) {
        super(seeker);
        this.seeker = seeker;
    }

    public static int frequencySlot(Direction side, int index) {
        return side.ordinal() * FREQUENCIES_PER_SIDE + index;
    }

    public ItemStackHandler getCore() {
        return core;
    }

    public ItemStackHandler getFrequencies() {
        return frequencies;
    }

    public boolean hasCore() {
        return !core.getStackInSlot(0).isEmpty();
    }

    public ItemStack takeCore() {
        return core.extractItem(0, 1, false);
    }

    void updateStrengths() {
        transmitters.forEach((side, transmitter) -> {
            int strength = seeker.getSideStrength(side);
            if (transmitter.strength != strength) {
                transmitter.strength = strength;
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(getWorld(), transmitter);
            }
        });
    }

    private boolean isServer() {
        return getWorld() != null && !getWorld().isClientSide;
    }

    private void refreshTransmitters() {
        for (Direction side : Direction.values()) {
            refreshTransmitter(side);
        }
    }

    private void refreshTransmitter(Direction side) {
        Transmitter previous = transmitters.remove(side);
        if (previous != null) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), previous);
        }
        if (!hasCore()) return;

        Frequency first = Frequency.of(frequencies.getStackInSlot(frequencySlot(side, 0)));
        Frequency second = Frequency.of(frequencies.getStackInSlot(frequencySlot(side, 1)));
        if (first == Frequency.EMPTY && second == Frequency.EMPTY) return;

        Transmitter transmitter = new Transmitter(Couple.create(first, second), seeker.getSideStrength(side));
        transmitters.put(side, transmitter);
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), transmitter);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (isServer()) {
            refreshTransmitters();
        }
    }

    @Override
    public void unload() {
        super.unload();
        if (isServer()) {
            transmitters.values().forEach(t -> Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), t));
            transmitters.clear();
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.put("logic_core", core.serializeNBT(registries));
        nbt.put("link_frequencies", frequencies.serializeNBT(registries));
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        if (nbt.contains("logic_core")) {
            core.deserializeNBT(registries, nbt.getCompound("logic_core"));
        }
        if (nbt.contains("link_frequencies")) {
            frequencies.deserializeNBT(registries, nbt.getCompound("link_frequencies"));
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    private class Transmitter implements IRedstoneLinkable {
        private final Couple<Frequency> key;
        private int strength;

        Transmitter(Couple<Frequency> key, int strength) {
            this.key = key;
            this.strength = strength;
        }

        @Override
        public int getTransmittedStrength() {
            return strength;
        }

        @Override
        public void setReceivedStrength(int power) {}

        @Override
        public boolean isListening() {
            return false;
        }

        @Override
        public boolean isAlive() {
            return !seeker.isChunkUnloaded() && !seeker.isRemoved()
                    && getWorld().isLoaded(getPos()) && getWorld().getBlockEntity(getPos()) == seeker;
        }

        @Override
        public Couple<Frequency> getNetworkKey() {
            return key;
        }

        @Override
        public BlockPos getLocation() {
            return getPos();
        }
    }
}
