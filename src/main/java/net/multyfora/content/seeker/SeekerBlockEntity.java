package net.multyfora.content.seeker;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.createmod.catnip.animation.LerpedFloat;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.multyfora.advancement.JocAdvancements;
import net.multyfora.client.seeker.SeekerBakedModel;
import net.multyfora.client.seeker.SeekerMenu;
import net.multyfora.content.Pointer;
import net.multyfora.content.SpaceUtils;
import net.multyfora.client.seeker.SeekerDistanceMenu;
import net.multyfora.index.JocBlockEntityTypes;
import net.multyfora.index.JocDataComponents;
import net.multyfora.index.JocItems;
import net.multyfora.index.SeekerCapturedTarget;

import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SeekerBlockEntity extends SmartBlockEntity implements MenuProvider {

    public enum ModuleType {
        NONE,
        SPYGLASS,
        PLAYER_DIR,
        MODULATING
    }


    private double targetX;
    private double targetY;
    private double targetZ;
    private Vec3 currentTarget;
    private boolean hasTarget;
    private boolean use3D = true;

    private int minDistance = 0;
    private int maxDistance = 256;

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

    private final Set<BlockPos> linkedSeekers = new HashSet<>();
    private transient boolean isSyncingFromLink = false;

    private int ticks;
    private double distanceToTarget;
    private double lastDistanceToTarget;
    private final Map<Direction, Integer> signalStrengthCache = new EnumMap<>(Direction.class);
    private SubLevel subLevel;
    @Nullable
    private UUID trackedSubLevel;
    public boolean lostTarget;

    private static final java.util.Map<java.util.UUID, java.util.List<BlockPos>> EYES_ON_PLAYER = new java.util.HashMap<>();

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

            if (lostTarget) {
                float randomYaw = level.random.nextFloat() * 360;
                float randomPitch = (level.random.nextFloat() - 0.5f) * 180;
                spyglassPointer.lerpedYawDegrees.chase(randomYaw, 200.0, LerpedFloat.Chaser.EXP);
                spyglassPointer.lerpedPitchDegrees.chase(randomPitch, 200.0, LerpedFloat.Chaser.EXP);
            }
        }

        if (isVirtual() || level.isClientSide) {
            return;
        }

        if (module == ModuleType.NONE) return;

        // Periodic link validation: remove stale links (every 100 ticks)
        if (level.getGameTime() % 100 == 0) {
            linkedSeekers.removeIf(linkedPos -> {
                if (!level.isLoaded(linkedPos)) return false;
                if (!(level.getBlockEntity(linkedPos) instanceof SeekerBlockEntity other)) return true;
                ModuleType otherModule = other.getModule();
                return otherModule != ModuleType.SPYGLASS && otherModule != ModuleType.MODULATING;
            });
        }

        try {
            this.subLevel = Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {}

        if (trackedSubLevel != null) {
            updateTargetFromSubLevel();
            if (!lostTarget) {
                SubLevelContainer container = SubLevelContainer.getContainer(level);
                if (container == null || container.getSubLevel(trackedSubLevel) == null) {
                    lostTarget = true;
                    trackedSubLevel = null;
                    Player nearest = level.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 50, false);
                    if (nearest instanceof ServerPlayer sp) {
                        net.multyfora.advancement.JocAdvancements.SEEKING_NOTHING.awardTo(sp);
                    }
                    clearTarget();
                    return;
                }
            }
        }

        if (!lostTarget) {
            if (module == ModuleType.SPYGLASS) {
                tickSpyglass();
            } else if (module == ModuleType.PLAYER_DIR) {
                tickPlayerDir();
            } else if (module == ModuleType.MODULATING) {
                tickModulating();
            }
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


    public double getDistanceToTarget() {
        if (level == null || getTargetPosition(false) == null) return -1;
        if (subLevel == null) {
            try {
                subLevel = Sable.HELPER.getContaining(this);
            } catch (Exception ignored) {}
        }
        Vec3 selfPos = SpaceUtils.getProjectedSelfPos(subLevel, worldPosition);
        Vec3 targetWorld = getTargetPosition(true);
        if (targetWorld == null) return -1;
        return selfPos.distanceTo(targetWorld);
    }

    private void tickModulating() {
        updateTarget();
        if (getTargetPosition(false) != null) {
            sendData();
        }
        if (selectivelyUpdateNeighbors()) {
            notifyUpdate();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
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

            EYES_ON_PLAYER.computeIfAbsent(trackedPlayerUUID, k -> new java.util.ArrayList<>()).add(worldPosition);

            if (countActiveEyes(trackedPlayerUUID) >= 6) {
                net.multyfora.advancement.JocAdvancements.SIX_EYES.awardTo((net.minecraft.server.level.ServerPlayer) player);
            }
        } else {
            playerDirPowered = false;
            java.util.List<BlockPos> list = EYES_ON_PLAYER.get(trackedPlayerUUID);
            if (list != null) {
                list.remove(worldPosition);
            }
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

    private int countActiveEyes(java.util.UUID playerUUID) {
        java.util.List<BlockPos> positions = EYES_ON_PLAYER.get(playerUUID);
        if (positions == null || level == null) return 0;
        int count = 0;
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof SeekerBlockEntity s
                && s.module == ModuleType.PLAYER_DIR
                && s.playerDirPowered
                && playerUUID.equals(s.trackedPlayerUUID)) {
                count++;
            }
        }
        return count;
    }


    private void updateTarget() {
        if (hasTarget) {
            currentTarget = new Vec3(targetX, targetY, targetZ);
        } else {
            currentTarget = null;
        }
        notifyUpdate();
    }

    private void updateTargetFromSubLevel() {
        if (level == null || level.isClientSide || trackedSubLevel == null) return;
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;
        SubLevel sl = container.getSubLevel(trackedSubLevel);
        if (sl == null) return;

        dev.ryanhcode.sable.companion.math.BoundingBox3dc bb = sl.boundingBox();
        double cx = (bb.minX() + bb.maxX()) / 2.0;
        double cy = (bb.minY() + bb.maxY()) / 2.0;
        double cz = (bb.minZ() + bb.maxZ()) / 2.0;

        if (Math.abs(targetX - cx) > 1.0e-4 || Math.abs(targetY - cy) > 1.0e-4 || Math.abs(targetZ - cz) > 1.0e-4) {
            setTarget(cx, cy, cz);
        }
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x; this.targetY = y; this.targetZ = z;
        this.hasTarget = true;
        this.currentTarget = new Vec3(x, y, z);
        this.lostTarget = false;
        if (module == ModuleType.SPYGLASS || module == ModuleType.MODULATING) {
            spyglassPointer.calculateRelativeAngle(this);
        }
        setChanged();
        sendData();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());

            Player nearest = level.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 10, false);
            if (nearest instanceof ServerPlayer sp) {
                JocAdvancements.SET_TARGET.awardTo(sp);
            }
        }

        if (level != null && !level.isClientSide && !isSyncingFromLink) {
            isSyncingFromLink = true;
            for (BlockPos linkedPos : linkedSeekers) {
                if (level == null) break;
                if (level.getBlockEntity(linkedPos) instanceof SeekerBlockEntity other) {
                    ModuleType otherModule = other.getModule();
                    if (otherModule == ModuleType.SPYGLASS || otherModule == ModuleType.MODULATING) {
                        other.setTarget(x, y, z);
                    }
                }
            }
            isSyncingFromLink = false;
        }
    }

    public void clearTarget() {
        this.trackedSubLevel = null;
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

        if (level != null && !level.isClientSide && !isSyncingFromLink) {
            isSyncingFromLink = true;
            for (BlockPos linkedPos : linkedSeekers) {
                if (level == null) break;
                if (level.getBlockEntity(linkedPos) instanceof SeekerBlockEntity other) {
                    ModuleType otherModule = other.getModule();
                    if (otherModule == ModuleType.SPYGLASS || otherModule == ModuleType.MODULATING) {
                        other.clearTarget();
                    }
                }
            }
            isSyncingFromLink = false;
        }
    }



    public ModuleType getModule() { return module; }
    public boolean hasModule() { return module != ModuleType.NONE; }

    public boolean insertModule(ItemStack stack, Player player) {
        if (hasModule()) return false;

        ModuleType incoming = moduleTypeForItem(stack);
        if (incoming == ModuleType.NONE) return false;

        lostTarget = false;
        this.module = incoming;

        if ((incoming == ModuleType.SPYGLASS || incoming == ModuleType.MODULATING)
            && stack.has(JocDataComponents.SEEKER_CARRIED_TARGET.get())) {
            SeekerCapturedTarget carried = stack.get(JocDataComponents.SEEKER_CARRIED_TARGET.get());
            if (carried != null) {
                if (carried.subLevelId() != null && level != null) {
                    trackedSubLevel = carried.subLevelId();
                    updateTargetFromSubLevel();

                    if (player instanceof ServerPlayer sp) {
                        SubLevel current = Sable.HELPER.getContaining(this);
                        if (current != null && carried.subLevelId().equals(current.getUniqueId())) {
                            net.multyfora.advancement.JocAdvancements.INSEEKERPTION.awardTo(sp);
                        }
                    }
                } else {
                    trackedSubLevel = null;
                    setTarget(carried.pos().getX(), carried.pos().getY(), carried.pos().getZ());
                }
            }
            stack.remove(JocDataComponents.SEEKER_CARRIED_TARGET.get());
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (incoming == ModuleType.PLAYER_DIR && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.multyfora.advancement.JocAdvancements.ALL_SEEING_EYE.awardTo(serverPlayer);
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
        minDistance = 0;
        maxDistance = 256;
        playerDirLookDir = Vec3.ZERO;
        java.util.UUID oldTracked = trackedPlayerUUID;
        trackedPlayerUUID = null;
        playerDirPowered = false;
        if (oldTracked != null) {
            java.util.List<BlockPos> list = EYES_ON_PLAYER.get(oldTracked);
            if (list != null) {
                list.remove(worldPosition);
            }
        }
        signalStrengthCache.clear();
        trackedSubLevel = null;
        lostTarget = false;
        clearTarget();

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

    public void linkTo(BlockPos pos) {
        if (linkedSeekers.add(pos)) {
            setChanged();
            sendData();
        }
    }

    public void unlinkFrom(BlockPos pos) {
        if (linkedSeekers.remove(pos)) {
            setChanged();
            sendData();
        }
    }

    public void unlinkFromAll() {
        if (level == null || level.isClientSide) {
            linkedSeekers.clear();
            return;
        }
        for (BlockPos linkedPos : linkedSeekers) {
            if (level.getBlockEntity(linkedPos) instanceof SeekerBlockEntity other) {
                other.linkedSeekers.remove(worldPosition);
                other.setChanged();
                other.sendData();
            }
        }
        linkedSeekers.clear();
        setChanged();
        sendData();
    }

    public Set<BlockPos> getLinkedSeekers() {
        return linkedSeekers;
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

    public static ModuleType moduleTypeForItem(ItemStack stack) {
        if (stack.is(Items.SPYGLASS)) {
            return ModuleType.SPYGLASS;
        }
        if (stack.is(Items.ENDER_EYE)) {
            return ModuleType.PLAYER_DIR;
        }
        if (stack.is(dev.simulated_team.simulated.index.SimBlocks.MODULATING_LINKED_RECEIVER.get().asItem())) {
            return ModuleType.MODULATING;
        }
        return ModuleType.NONE;
    }

    private static ItemStack itemForModuleType(ModuleType type) {
        return switch (type) {
            case SPYGLASS   -> new ItemStack(Items.SPYGLASS);
            case PLAYER_DIR -> new ItemStack(Items.ENDER_EYE);
            case MODULATING -> new ItemStack(dev.simulated_team.simulated.index.SimBlocks.MODULATING_LINKED_RECEIVER.get().asItem());
            case NONE       -> ItemStack.EMPTY;
        };
    }


    public int getRedstoneStrength(Direction direction) {
        return switch (module) {
            case SPYGLASS -> getSpyglassRedstoneStrength(direction);
            case PLAYER_DIR -> getPlayerDirRedstoneStrength(direction);
            case MODULATING -> getModulatingRedstoneStrength();
            case NONE -> 0;
        };
    }

    private int getModulatingRedstoneStrength() {
        if (level == null || getTargetPosition(false) == null) return 0;
        double dist = getDistanceToTarget();
        if (dist < 0) return 0;
        if (dist <= minDistance) return 15;
        if (dist >= maxDistance) return 0;
        if (maxDistance <= minDistance) return 0;
        return (int) Math.round(15.0 * (1.0 - (dist - minDistance) / (double)(maxDistance - minDistance)));
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
        tag.putInt("min_distance", minDistance);
        tag.putInt("max_distance", maxDistance);
        tag.putFloat("relative_angle", spyglassPointer.getYaw());
        tag.putFloat("tilt_angle",     spyglassPointer.getPitch());

        tag.putBoolean("player_dir_powered", playerDirPowered);
        if (trackedPlayerUUID != null) {
            tag.putUUID("tracked_player", trackedPlayerUUID);
        }
        if (trackedSubLevel != null) {
            tag.putUUID("tracked_sub_level", trackedSubLevel);
        }

        // Only sync the animation trigger over the network — never persist it to the save file
        if (clientPacket) {
            tag.putLong("insert_anim_tick", insertAnimStartTick);
            tag.putBoolean("lost_target", lostTarget);
        }

        // Persist linked seekers in server save; sync to client for highlight rendering
        var list = new net.minecraft.nbt.ListTag();
        for (BlockPos p : linkedSeekers) {
            list.add(net.minecraft.nbt.NbtUtils.writeBlockPos(p));
        }
        tag.put("linked_seekers", list);
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

        minDistance = tag.contains("min_distance") ? tag.getInt("min_distance") : 0;
        maxDistance = tag.contains("max_distance") ? tag.getInt("max_distance") : 256;

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
        } else if (tag.contains("relative_angle")) {
            spyglassPointer.lerpedYawDegrees.setValue(yaw);
            spyglassPointer.lerpedPitchDegrees.setValue(pitch);
        }

        playerDirPowered = tag.getBoolean("player_dir_powered");
        if (tag.hasUUID("tracked_player")) {
            trackedPlayerUUID = tag.getUUID("tracked_player");
        } else {
            trackedPlayerUUID = null;
        }

        if (tag.hasUUID("tracked_sub_level")) {
            trackedSubLevel = tag.getUUID("tracked_sub_level");
        } else {
            trackedSubLevel = null;
        }

        if (clientPacket && tag.contains("insert_anim_tick")) {
            insertAnimStartTick = tag.getLong("insert_anim_tick");
            lostTarget = tag.getBoolean("lost_target");
        }

        // Restore linked seekers from both server save and client sync
        linkedSeekers.clear();
        if (tag.contains("linked_seekers")) {
            var list = tag.getList("linked_seekers", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                var entry = list.getCompound(i);
                linkedSeekers.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
            }
        }

        if (clientPacket) {
            requestModelDataUpdate();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        if (module == ModuleType.MODULATING) {
            return Component.translatable("screen.joc.seeker_distance");
        }
        return Component.translatable("screen.joc.seeker");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int i, @NotNull Inventory inventory, @NotNull Player player
    ) {
        if (module == ModuleType.MODULATING) {
            return new SeekerDistanceMenu(i, inventory, this);
        }
        return new SeekerMenu(i, inventory, this);
    }

    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }
    public boolean hasTarget() { return hasTarget; }
    public boolean isUse3D()   { return use3D; }

    public int getMinDistance() { return minDistance; }
    public int getMaxDistance() { return maxDistance; }

    public void setMinMaxDistance(int min, int max) {
        this.minDistance = Math.max(0, Math.min(256, min));
        this.maxDistance = Math.max(0, Math.min(256, max));
        if (this.minDistance > this.maxDistance) this.minDistance = this.maxDistance;
        setChanged();
        sendData();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public void setUse3D(boolean use3D) {
        this.use3D = use3D;
        if (module == ModuleType.SPYGLASS || module == ModuleType.MODULATING) {
            spyglassPointer.calculateRelativeAngle(this);
        }
        setChanged();
        sendData();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            selectivelyUpdateNeighbors();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }

        if (level != null && !level.isClientSide && !isSyncingFromLink) {
            isSyncingFromLink = true;
            for (BlockPos linkedPos : linkedSeekers) {
                if (level == null) break;
                if (level.getBlockEntity(linkedPos) instanceof SeekerBlockEntity other) {
                    ModuleType otherModule = other.getModule();
                    if (otherModule == ModuleType.SPYGLASS || otherModule == ModuleType.MODULATING) {
                        other.setUse3D(use3D);
                    }
                }
            }
            isSyncingFromLink = false;
        }
    }

    public BlockPos getTarget() {
        if (!hasTarget) return null;
        return new BlockPos((int) targetX, (int) targetY, (int) targetZ);
    }

    public BlockPos getWorldPosition() { return worldPosition; }
    public SubLevel getSubLevel() { return subLevel; }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(SeekerBakedModel.TEXTURE_VARIANT, getTextureVariant())
                .build();
    }

    private int getTextureVariant() {
        if (module == ModuleType.NONE) return 0;
        if (module == ModuleType.PLAYER_DIR) return playerDirPowered ? 1 : 0;
        if (module == ModuleType.MODULATING) {
            if (use3D) return hasTarget ? 1 : 0;
            return hasTarget ? 3 : 2;
        }
        if (use3D) return hasTarget ? 1 : 0;
        return hasTarget ? 3 : 2;
    }
}
