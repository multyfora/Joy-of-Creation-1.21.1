package net.multyfora.content.balloon;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlockEntityTypes;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Balloon block entity: handles rope tethering behaviour (via RopeStrandHolderBehavior) and
 * tracks the position of the connector block the balloon is tethered to (via ConnectorPosBehaviour).
 * The connector position is used by the client-side tether renderer to draw the rope line.
 **/
public class BalloonBlockEntity extends SmartBlockEntity implements RopeStrandHolderBlockEntity {

    // The rope holder behaviour from the Simulated mod, managing rope strand physics
    private RopeStrandHolderBehavior ropeHolder;
    // Custom behaviour tracking where this balloon's tether connector is located
    private ConnectorPosBehaviour connectorPosBehaviour;

    public BalloonBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.BALLOON.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // Add the rope holder behaviour so the balloon can attach/detach ropes
        behaviours.add(ropeHolder = new RopeStrandHolderBehavior(this));
        // Add the connector position tracker so the tether line renderer knows where to draw to
        behaviours.add(connectorPosBehaviour = new ConnectorPosBehaviour(this));
    }

    @Override
    public RopeStrandHolderBehavior getBehavior() {
        return ropeHolder;
    }

    // Returns the attachment point of the rope at the center of this block
    @Override
    public Vec3 getAttachmentPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos);
    }

    // Returns the position of the connector block this balloon is tethered to, or null if untethered
    @Nullable
    public BlockPos getConnectorPos() {
        return connectorPosBehaviour != null ? connectorPosBehaviour.getConnectorPos() : null;
    }

    public void setConnectorPos(@Nullable BlockPos pos) {
        if (connectorPosBehaviour != null) {
            connectorPosBehaviour.setConnectorPos(pos);
        }
    }

    /**
     * Custom BlockEntityBehaviour that persists the connector block position in NBT
     * so it survives chunk loads and saves. This is separate from the rope system because
     * the rope may not always be loaded when we need the connector position for rendering.
     **/
    private static class ConnectorPosBehaviour extends BlockEntityBehaviour {
        // Unique type identifier for this behaviour
        public static final BehaviourType<ConnectorPosBehaviour> TYPE = new BehaviourType<>("joc:connector_pos");

        @Nullable private BlockPos connectorPos;

        public ConnectorPosBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Nullable
        public BlockPos getConnectorPos() {
            return connectorPos;
        }

        public void setConnectorPos(@Nullable BlockPos pos) {
            this.connectorPos = pos;
            blockEntity.setChanged();
        }

        @Override
        public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
            super.write(nbt, registries, clientPacket);
            if (connectorPos != null) {
                // Store as a single long (packed BlockPos) to save space
                nbt.putLong("connector_pos", connectorPos.asLong());
            }
        }

        @Override
        public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
            super.read(nbt, registries, clientPacket);
            if (nbt.contains("connector_pos")) {
                connectorPos = BlockPos.of(nbt.getLong("connector_pos"));
            } else {
                connectorPos = null;
            }
        }

        @Override
        public BehaviourType<?> getType() {
            return TYPE;
        }
    }
}
