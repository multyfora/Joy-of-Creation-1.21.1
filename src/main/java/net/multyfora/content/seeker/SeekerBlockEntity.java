package net.multyfora.content.seeker;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.createmod.catnip.animation.LerpedFloat;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.client.seeker.SeekerMenu;
import net.multyfora.content.Pointer;
import net.multyfora.content.SpaceUtils;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocItems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SeekerBlockEntity extends SmartBlockEntity implements MenuProvider {

    public enum ModuleType {
        NONE,
        SPYGLASS,
        PLAYER_DIR
    }


    private double targetX;
    private double targetY;
    private double targetZ;
    private Vec3 currentTarget;
    private boolean hasTarget;
    private boolean use3D = true;

    public boolean isPowering;
    public final Pointer spyglassPointer = new Pointer();


    private Vec3 playerDirLookDir = Vec3.ZERO;
    @Nullable
    private UUID trackedPlayerUUID = null;
    private boolean playerDirPowered = false;
    private static final double PLAYER_DIR_EPSILON = 1.0e-6;

    //anim stuff
    private static final float INSERT_ANIM_DURATION_TICKS = 12f; // 0.6s at 20 TPS
    private long insertAnimStartTick = Long.MIN_VALUE;


    private ModuleType module = ModuleType.NONE;

    private int ticks;
    private double distanceToTarget;
    private double lastDistanceToTarget;
    private final Map<Direction, Integer> signalStrengthCache = new EnumMap<>(Direction.class);
    private SubLevel subLevel;

    public SeekerBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.SEEKER.get(), pos, state);
        targetX = 0;
        targetY = 0;
        targetZ = 0;
        currentTarget = null;
        hasTarget = false;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();

        if (level == null) return;

        if (level.isClientSide) {
            spyglassPointer.lerpedPitchDegrees.tickChaser();
            spyglassPointer.lerpedYawDegrees.tickChaser();
        }

        if (isVirtual() || level.isClientSide) return;

        if (module == ModuleType.NONE) return;

        try {
            this.subLevel = Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {}

        if (module == ModuleType.SPYGLASS) {
            tickSpyglass();
        } else if (module == ModuleType.PLAYER_DIR) {
            tickPlayerDir();
        }
    }

    private void tickSpyglass() {
        updateTarget();
        Vec3 targetLocal = getTargetPosition(false);
        if (targetLocal != null) {
            distanceToTarget = SpaceUtils
                    .getProjectedSelfPos(subLevel, worldPosition)
                    .distanceTo(getTargetPosition(true));
            if (distanceToTarget >= 5000) {
                lastDistanceToTarget = distanceToTarget;
            }
            spyglassPointer.tick(this);
            sendData();
        }

        if (selectivelyUpdateNeighbors()) {
            notifyUpdate();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }

        if (getTargetPosition(false) != null) {
            ticks++;
            if (ticks >= 10) {
                ticks = 0;
                lastDistanceToTarget = distanceToTarget;
                distanceToTarget = SpaceUtils
                        .getProjectedSelfPos(subLevel, worldPosition)
                        .distanceTo(getTargetPosition(true));
            }
        }
    }

    private void tickPlayerDir() {
        if (!playerDirPowered) return;

        Player trackedPlayer = getTrackedPlayer();
        if (trackedPlayer == null) return;

        Vec3 lookAngle = trackedPlayer.getLookAngle();
        Quaterniond invRot = new Quaterniond(SpaceUtils.getSublevelRot(subLevel)).invert();
        playerDirLookDir = SpaceUtils.rotateQuat(lookAngle.normalize(), invRot).scale(-1);

        updatePlayerDirAngles();
        sendData();

        if (selectivelyUpdateNeighbors()) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private void updatePlayerDirAngles() {
        Vec3 dir = playerDirLookDir;
        double len = dir.length();
        if (len < 1e-6) return;

        Vec3 n = dir.scale(1.0 / len);
        float yaw   = (float) Math.toDegrees(Math.atan2(n.x, n.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-n.y, Math.sqrt(n.x * n.x + n.z * n.z)));

        spyglassPointer.setYaw(yaw);
        spyglassPointer.setPitch(pitch);
    }


    public void onPlayerDirActivated(Player player) {
        if (module != ModuleType.PLAYER_DIR || level == null || level.isClientSide) return;

        try {
            subLevel = Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {}

        signalStrengthCache.clear();

        if (!playerDirPowered) {
            trackedPlayerUUID = player.getUUID();
            playerDirPowered = true;

            Vec3 lookAngle = player.getLookAngle();
            Quaterniond invRot = new Quaterniond(SpaceUtils.getSublevelRot(subLevel)).invert();
            playerDirLookDir = SpaceUtils.rotateQuat(lookAngle.normalize(), invRot).scale(-1);
            updatePlayerDirAngles();
        } else {
            playerDirPowered = false;
        }

        setChanged();
        sendData();
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    @Nullable
    private Player getTrackedPlayer() {
        if (trackedPlayerUUID == null || level == null) return null;
        Player player = level.getPlayerByUUID(trackedPlayerUUID);
        if (player == null || !player.isAlive() || player.isSpectator()) return null;
        return player;
    }


    private void updateTarget() {
        if (hasTarget) {
            currentTarget = new Vec3(targetX, targetY, targetZ);
        } else {
            currentTarget = null;
        }
        notifyUpdate();
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x; this.targetY = y; this.targetZ = z;
        this.hasTarget = true;
        this.currentTarget = new Vec3(x, y, z);
        if (module == ModuleType.SPYGLASS) {
            spyglassPointer.calculateRelativeAngle(this);
        }
        setChanged();
        sendData();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public void clearTarget() {
        this.hasTarget = false;
        this.currentTarget = null;
        this.targetX = 0;
        this.targetY = 0;
        this.targetZ = 0;
        setChanged();
        sendData();

        if (level != null && !level.isClientSide) {
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }



    public ModuleType getModule() { return module; }
    public boolean hasModule() { return module != ModuleType.NONE; }

    public boolean insertModule(ItemStack stack, Player player) {
        if (hasModule()) return false;

        ModuleType incoming = moduleTypeForItem(stack);
        if (incoming == ModuleType.NONE) return false;

        this.module = incoming;

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        markInsertAnimation();

        if (level != null) {
            level.playSound(null, worldPosition, net.multyfora.index.JocSounds.SEEKER_ITEM_IN.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        onModuleChanged();
        return true;
    }

    public boolean extractModule(Player player) {
        if (!hasModule()) return false;

        ItemStack drop = itemForModuleType(module);
        this.module = ModuleType.NONE;

        spyglassPointer.setYaw(0);
        spyglassPointer.setPitch(0);
        playerDirLookDir = Vec3.ZERO;
        trackedPlayerUUID = null;
        playerDirPowered = false;
        signalStrengthCache.clear();

        if (!drop.isEmpty()) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }

        if (level != null) {
            level.playSound(null, worldPosition, net.multyfora.index.JocSounds.SEEKER_ITEM_OUT.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        onModuleChanged();
        return true;
    }

    private void onModuleChanged() {
        setChanged();
        sendData();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private static ModuleType moduleTypeForItem(ItemStack stack) {
        if (stack.is(Items.SPYGLASS)) {
            return ModuleType.SPYGLASS;
        }
        if (stack.is(Items.ENDER_EYE)) {
            return ModuleType.PLAYER_DIR;
        }
        return ModuleType.NONE;
    }

    private static ItemStack itemForModuleType(ModuleType type) {
        return switch (type) {
            case SPYGLASS   -> new ItemStack(Items.SPYGLASS);
            case PLAYER_DIR -> new ItemStack(Items.ENDER_EYE);
            case NONE       -> ItemStack.EMPTY;
        };
    }


    public int getRedstoneStrength(Direction direction) {
        return switch (module) {
            case SPYGLASS -> getSpyglassRedstoneStrength(direction);
            case PLAYER_DIR -> getPlayerDirRedstoneStrength(direction);
            case NONE -> 0;
        };
    }

    /** Called when a module is inserted, to kick off the client-side grow+twirl animation. */
    private void markInsertAnimation() {
        if (level != null) {
            insertAnimStartTick = level.getGameTime();
        }
    }

    /**
     * Returns animation progress in [0, 1]; 1 means "no animation running / finished".
     * Safe to call every frame from the renderer.
     **/
    public float getInsertAnimationProgress(float partialTick) {
        if (level == null || insertAnimStartTick == Long.MIN_VALUE) return 1f;
        float elapsed = (level.getGameTime() - insertAnimStartTick) + partialTick;
        return net.minecraft.util.Mth.clamp(elapsed / INSERT_ANIM_DURATION_TICKS, 0f, 1f);
    }

    private int getSpyglassRedstoneStrength(Direction direction) {
        if (this.level == null || getTargetPosition(false) == null) return 0;

        if (!use3D && (direction == Direction.UP || direction == Direction.DOWN)) return 0;

        Vec3 targetWorld = getTargetPosition(true);
        if (targetWorld == null) return 0;

        Vec3 selfPos = SpaceUtils.getProjectedSelfPos(subLevel, worldPosition);
        Vec3 diffWorld = targetWorld.subtract(selfPos);

        if (diffWorld.lengthSqr() < 1e-6) return 0;

        var forwardRot = SpaceUtils.getSublevelRot(subLevel);
        var inverseRot = new Quaterniond(forwardRot).conjugate();
        Vec3 diffLocal = SpaceUtils.rotateQuat(diffWorld, inverseRot);

        Vec3 diffForCalc;
        if (use3D) {
            diffForCalc = diffLocal.normalize();
        } else {
            Vec3 diffXZ = new Vec3(diffLocal.x, 0, diffLocal.z);
            if (diffXZ.lengthSqr() < 1e-6) return 0;
            diffForCalc = diffXZ.normalize();
        }

        Vec3 dirNormal = Vec3.atLowerCornerOf(direction.getNormal());
        double dot = -diffForCalc.dot(dirNormal);

        if (dot <= 0) return 0;

        int power = (int) Math.round((Math.asin(dot) / Math.PI) * 30.0);
        return Math.min(15, Math.max(0, power));
    }

    private int getPlayerDirRedstoneStrength(Direction direction) {
        if (level == null || !playerDirPowered) return 0;
        if (playerDirLookDir.lengthSqr() <= PLAYER_DIR_EPSILON) return 0;

        Vec3 dirNormal = Vec3.atLowerCornerOf(direction.getNormal());
        double dot = Math.max(-1.0, Math.min(1.0, playerDirLookDir.dot(dirNormal)));
        return (int) (Math.asin(dot) / Math.PI * 30.0 + 0.5);
    }


    public Vec3 getTargetPosition(boolean worldSpace) {
        if (currentTarget == null) return null;
        if (worldSpace && subLevel != null) {
            try {
                return Sable.HELPER.projectOutOfSubLevel(level, currentTarget);
            } catch (Exception ignored) {}
        }
        return currentTarget;
    }

    private boolean selectivelyUpdateNeighbors() {
        if (level == null || level.isClientSide) return false;

        BlockState state = getBlockState();
        boolean updated = false;

        for (Direction neighborDir : Direction.values()) {
            Direction queryDir = neighborDir.getOpposite();

            int prev = signalStrengthCache.getOrDefault(queryDir, -1);
            int curr = getRedstoneStrength(queryDir);

            if (prev != curr) {
                signalStrengthCache.put(queryDir, curr);
                level.updateNeighborsAt(worldPosition.relative(neighborDir), state.getBlock());
                updated = true;
            }
        }
        return updated;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("target_x", targetX);
        tag.putDouble("target_y", targetY);
        tag.putDouble("target_z", targetZ);
        tag.putBoolean("has_target", hasTarget);
        tag.putBoolean("use_3d", use3D);
        tag.putString("module", module.name());
        tag.putFloat("relative_angle", spyglassPointer.getYaw());
        tag.putFloat("tilt_angle",     spyglassPointer.getPitch());

        tag.putBoolean("player_dir_powered", playerDirPowered);
        if (trackedPlayerUUID != null) {
            tag.putUUID("tracked_player", trackedPlayerUUID);
        }

        // Only sync the animation trigger over the network — never persist it to the save file
        if (clientPacket) {
            tag.putLong("insert_anim_tick", insertAnimStartTick);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        targetX   = tag.getDouble("target_x");
        targetY   = tag.getDouble("target_y");
        targetZ   = tag.getDouble("target_z");
        hasTarget = tag.getBoolean("has_target");
        use3D     = tag.contains("use_3d") ? tag.getBoolean("use_3d") : true;

        if (tag.contains("module")) {
            try {
                module = ModuleType.valueOf(tag.getString("module"));
            } catch (IllegalArgumentException e) {
                module = ModuleType.NONE;
            }
        } else {
            module = ModuleType.NONE;
        }

        if (hasTarget) {
            currentTarget = new Vec3(targetX, targetY, targetZ);
        } else {
            currentTarget = null;
        }
        isPowering = hasTarget;

        float yaw   = tag.getFloat("relative_angle");
        float pitch = tag.getFloat("tilt_angle");
        spyglassPointer.setYaw(yaw);
        spyglassPointer.setPitch(pitch);

        if (clientPacket) {
            spyglassPointer.lerpedYawDegrees.chase(yaw,   1.0, LerpedFloat.Chaser.EXP);
            spyglassPointer.lerpedPitchDegrees.chase(pitch, 1.0, LerpedFloat.Chaser.EXP);
        }

        playerDirPowered = tag.getBoolean("player_dir_powered");
        if (tag.hasUUID("tracked_player")) {
            trackedPlayerUUID = tag.getUUID("tracked_player");
        } else {
            trackedPlayerUUID = null;
        }

        if (clientPacket && tag.contains("insert_anim_tick")) {
            insertAnimStartTick = tag.getLong("insert_anim_tick");
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("screen.joc.seeker");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int i, @NotNull Inventory inventory, @NotNull Player player
    ) {
        return new SeekerMenu(i, inventory, this);
    }

    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }
    public boolean hasTarget() { return hasTarget; }
    public boolean isUse3D()   { return use3D; }

    public void setUse3D(boolean use3D) {
        this.use3D = use3D;
        if (module == ModuleType.SPYGLASS) {
            spyglassPointer.calculateRelativeAngle(this);
        }
        setChanged();
        sendData();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public BlockPos getTarget() {
        if (!hasTarget) return null;
        return new BlockPos((int) targetX, (int) targetY, (int) targetZ);
    }

    public BlockPos getWorldPosition() { return worldPosition; }
    public SubLevel getSubLevel() { return subLevel; }
}
