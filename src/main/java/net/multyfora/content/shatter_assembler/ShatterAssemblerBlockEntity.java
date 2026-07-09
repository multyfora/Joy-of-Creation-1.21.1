package net.multyfora.content.shatter_assembler;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.index.JocBlockEntityTypes;

import java.util.List;

public class ShatterAssemblerBlockEntity extends SmartBlockEntity {
    private static final float FLICKED_ANGLE_DEGREES = 45.0F;
    private static final double LEVER_CHASE_SPEED = 0.75;

    public LerpedFloat visualAngle = LerpedFloat.linear();
    public boolean holdingLever = false;
    public boolean clientHoldLeverInPlace = false;

    private boolean leverInitialized = false;
    private boolean controlledByPlayer = false;
    private float playerAngle = 0.0F;

    public ShatterAssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ShatterAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.SHATTER_ASSEMBLER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!this.isVirtual()) {
            this.initializeLeverPosition();
        }
    }

    protected void initializeLeverPosition() {
        if (!this.leverInitialized) {
            this.clientFlickLeverTo(this.getSubLevel() != null);
            this.jerkLever();
            this.leverInitialized = true;
        }
    }

    private SubLevel getSubLevel() {
        return Sable.HELPER.getContaining(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.holdingLever) {
            this.visualAngle.setValue(this.visualAngle.getValue());
        } else if (this.controlledByPlayer) {
            this.visualAngle.setValue(this.visualAngle.getValue());
            this.visualAngle.setValueNoUpdate(this.playerAngle);
        } else {
            this.visualAngle.tickChaser();
        }
    }

    public void setClientHoldLeverInPlace(boolean holding) {
        this.holdingLever = holding;
    }

    public void updateControlledByPlayer(float angle) {
        if (!this.controlledByPlayer) {
            this.controlledByPlayer = true;
        }
        this.playerAngle = angle;
    }

    public boolean stopControllingPlayer() {
        if (!this.controlledByPlayer) {
            return false;
        }
        this.controlledByPlayer = false;
        return true;
    }

    public void clientFlickLeverTo(boolean flicked) {
        this.visualAngle.chase(flicked ? FLICKED_ANGLE_DEGREES : 0.0F, LEVER_CHASE_SPEED, LerpedFloat.Chaser.EXP);
    }

    public void jerkLever() {
        this.visualAngle.setValue(this.visualAngle.getChaseTarget());
        this.visualAngle.setValue(this.visualAngle.getChaseTarget());
    }

    public float getClientAngle(float partialTicks) {
        return this.visualAngle.getValue(partialTicks);
    }
}
