package net.multyfora.content.seeker;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.createmod.catnip.lang.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.multyfora.index.JocBlockEntityTypes;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class SeekerTableBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int[] SCALES = {8, 16, 32, 64, 128};
    public static final int DEFAULT_SCALE = 32;
    public static final double RIM = 2 / 16.0;

    private int width = 1;
    private int depth = 1;
    private int scale = DEFAULT_SCALE;
    private boolean hasMarker;
    private double markerX;
    private double markerZ;
    private int markerY;
    private SeekerTableMarker markerStyle = SeekerTableMarker.PIN;
    private int markerColor = SeekerTableMarker.DEFAULT_COLOR;

    @Nullable
    public Object clientMap;

    public record Settings(int scale, boolean hasMarker, double markerX, int markerY, double markerZ,
                           SeekerTableMarker markerStyle, int markerColor) {}

    public SeekerTableBlockEntity(BlockPos pos, BlockState state) {
        super(JocBlockEntityTypes.SEEKER_TABLE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && (width > 1 || depth > 1) && !isController())
            SeekerTableMultiblock.tryForm(level, worldPosition, null);
    }

    public boolean isController() {
        return getBlockState().getBlock() instanceof SeekerTableBlock && SeekerTableBlock.isController(getBlockState());
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof SeekerTableBlock ? state.getValue(SeekerTableBlock.FACING) : Direction.UP;
    }

    public double getMapY() {
        return (getFacing() == Direction.UP ? SeekerTableBlock.TABLE_MAP_Y : SeekerTableBlock.WALL_MAP_Y) / 16.0;
    }

    public int getWidth() { return width; }
    public int getDepth() { return depth; }
    public int getScale() { return scale; }
    public boolean hasMarker() { return hasMarker; }
    public double getMarkerX() { return markerX; }
    public double getMarkerZ() { return markerZ; }
    public SeekerTableMarker getMarkerStyle() { return markerStyle; }
    public int getMarkerColor() { return markerColor; }

    public Settings getSettings() {
        return new Settings(scale, hasMarker, markerX, markerY, markerZ, markerStyle, markerColor);
    }

    void assemble(int width, int depth, @Nullable Settings settings) {
        this.width = width;
        this.depth = depth;
        if (settings != null) {
            scale = settings.scale();
            hasMarker = settings.hasMarker();
            markerX = settings.markerX();
            markerY = settings.markerY();
            markerZ = settings.markerZ();
            markerStyle = settings.markerStyle();
            markerColor = settings.markerColor();
        }
        notifyUpdate();
    }

    void resetAsPart() {
        width = depth = 1;
        scale = DEFAULT_SCALE;
        hasMarker = false;
        notifyUpdate();
    }

    public void cycleScale() {
        int next = 0;
        for (int i = 0; i < SCALES.length; i++) {
            if (SCALES[i] == scale)
                next = (i + 1) % SCALES.length;
        }
        scale = SCALES[next];
        notifyUpdate();
    }

    @Nullable
    public BlockPos getMarkerTarget() {
        return hasMarker ? BlockPos.containing(markerX, markerY, markerZ) : null;
    }

    public void clearMarker() {
        hasMarker = false;
        notifyUpdate();
    }

    public boolean placeMarkerAt(Vec3 hit, BlockPos clickedCell, SeekerTableMarker style, int color) {
        if (level == null)
            return false;
        SubLevel subLevel = getSubLevelSafe();
        Vec3 local = hit;
        if (subLevel != null && local.distanceToSqr(Vec3.atCenterOf(clickedCell)) > 16)
            local = subLevel.logicalPose().transformPositionInverse(hit);

        Vec3 onMap = blockToLocal(local);
        double x = Mth.clamp(onMap.x, RIM, width - RIM);
        double z = Mth.clamp(onMap.z, RIM, depth - RIM);

        Vec3 world = mapToWorld(x, z, subLevel == null ? null : subLevel.logicalPose());
        hasMarker = true;
        markerStyle = style;
        markerColor = color;
        markerX = world.x;
        markerZ = world.z;
        int bx = Mth.floor(world.x), bz = Mth.floor(world.z);
        markerY = level.hasChunk(bx >> 4, bz >> 4)
            ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz)
            : Mth.floor(project(subLevel, getMapCenterLocal()).y);
        notifyUpdate();
        return true;
    }

    public Vec3 localToBlock(double x, double y, double z) {
        Direction facing = getFacing();
        Direction right = SeekerTableBlock.toWorld(facing, Direction.EAST);
        Direction down = SeekerTableBlock.toWorld(facing, Direction.SOUTH);
        x -= 0.5;
        y -= 0.5;
        z -= 0.5;
        return new Vec3(
            worldPosition.getX() + 0.5 + x * right.getStepX() + y * facing.getStepX() + z * down.getStepX(),
            worldPosition.getY() + 0.5 + x * right.getStepY() + y * facing.getStepY() + z * down.getStepY(),
            worldPosition.getZ() + 0.5 + x * right.getStepZ() + y * facing.getStepZ() + z * down.getStepZ());
    }

    public Vec3 blockToLocal(Vec3 block) {
        Direction facing = getFacing();
        Direction right = SeekerTableBlock.toWorld(facing, Direction.EAST);
        Direction down = SeekerTableBlock.toWorld(facing, Direction.SOUTH);
        double dx = block.x - worldPosition.getX() - 0.5;
        double dy = block.y - worldPosition.getY() - 0.5;
        double dz = block.z - worldPosition.getZ() - 0.5;
        return new Vec3(
            0.5 + dx * right.getStepX() + dy * right.getStepY() + dz * right.getStepZ(),
            0.5 + dx * facing.getStepX() + dy * facing.getStepY() + dz * facing.getStepZ(),
            0.5 + dx * down.getStepX() + dy * down.getStepY() + dz * down.getStepZ());
    }

    private Vec3 mapAxisX() {
        return Vec3.atLowerCornerOf(SeekerTableBlock.toWorld(getFacing(), Direction.EAST).getNormal());
    }

    private Vec3 mapAxisZ() {
        Direction facing = getFacing();
        return Vec3.atLowerCornerOf((facing == Direction.UP ? Direction.SOUTH : facing).getNormal());
    }

    public Vec3 getMapCenterLocal() {
        return localToBlock(width / 2.0, getMapY(), depth / 2.0);
    }

    private double[] heading(@Nullable Pose3dc pose) {
        Vec3 ex = mapAxisX(), ez = mapAxisZ();
        if (pose != null) {
            ex = pose.transformNormal(ex);
            ez = pose.transformNormal(ez);
        }
        double angle = Math.atan2(ex.z - ez.x, ex.x + ez.z);
        return new double[]{Math.cos(angle), Math.sin(angle)};
    }

    public Vec3 mapToWorld(double x, double z, @Nullable Pose3dc pose) {
        Vec3 center = getMapCenterLocal();
        Vec3 worldCenter = pose == null ? center : pose.transformPosition(center);
        double[] turn = heading(pose);
        double dx = (x - width / 2.0) * scale, dz = (z - depth / 2.0) * scale;
        return new Vec3(
            worldCenter.x + dx * turn[0] - dz * turn[1],
            worldCenter.y,
            worldCenter.z + dx * turn[1] + dz * turn[0]);
    }

    public double[] worldToMapOffset(double worldX, double worldZ, Vec3 worldCenter, double scale, @Nullable Pose3dc pose) {
        double[] turn = heading(pose);
        double dx = (worldX - worldCenter.x) / scale;
        double dz = (worldZ - worldCenter.z) / scale;
        return new double[]{dx * turn[0] + dz * turn[1], dz * turn[0] - dx * turn[1]};
    }

    @Nullable
    public SubLevel getSubLevelSafe() {
        try {
            return Sable.HELPER.getContaining(this);
        } catch (Exception ignored) {
            return null;
        }
    }

    public Vec3 getWorldCenter() {
        return project(getSubLevelSafe(), getMapCenterLocal());
    }

    private static Vec3 project(@Nullable SubLevel subLevel, Vec3 local) {
        if (subLevel == null)
            return local;
        try {
            return subLevel.logicalPose().transformPosition(local);
        } catch (Exception ignored) {
            return local;
        }
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(localToBlock(0, 0, 0), localToBlock(width, 2, depth));
    }

    public AABB getLocalBounds() {
        double rim = getMapY() + SeekerTableBlock.RIM_RISE / 16.0;
        return new AABB(localToBlock(0, 0, 0), localToBlock(width, rim, depth)).move(
            -worldPosition.getX(), -worldPosition.getY(), -worldPosition.getZ());
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (level != null && level.isClientSide)
            net.multyfora.client.seeker.SeekerTableMap.release(this);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("width", width);
        tag.putInt("depth", depth);
        tag.putInt("scale", scale);
        if (hasMarker) {
            CompoundTag marker = new CompoundTag();
            marker.putDouble("x", markerX);
            marker.putInt("y", markerY);
            marker.putDouble("z", markerZ);
            marker.putString("style", markerStyle.name().toLowerCase(Locale.ROOT));
            marker.putInt("color", markerColor);
            tag.put("marker", marker);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        width = Math.max(1, tag.getInt("width"));
        depth = Math.max(1, tag.getInt("depth"));
        scale = tag.contains("scale") ? tag.getInt("scale") : DEFAULT_SCALE;
        hasMarker = tag.contains("marker");
        if (hasMarker) {
            CompoundTag marker = tag.getCompound("marker");
            markerX = marker.getDouble("x");
            markerY = marker.getInt("y");
            markerZ = marker.getDouble("z");
            markerStyle = SeekerTableMarker.byName(marker.getString("style"));
            markerColor = marker.contains("color") ? marker.getInt("color") : SeekerTableMarker.DEFAULT_COLOR;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        SeekerTableBlockEntity table = isController() ? this
            : level == null ? null : SeekerTableBlock.getController(level, worldPosition);
        if (table == null)
            return false;

        Lang.builder("joc").translate("goggles.seeker_table.header").forGoggles(tooltip);
        Lang.builder("joc")
            .translate("goggles.seeker_table.size")
            .add(Lang.builder("joc").text(table.width + " × " + table.depth).style(ChatFormatting.WHITE))
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip, 1);
        Lang.builder("joc")
            .translate("goggles.seeker_table.scale")
            .add(Lang.builder("joc").text("1 : " + table.scale).style(ChatFormatting.GOLD))
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip, 1);
        if (table.hasMarker) {
            Lang.builder("joc")
                .translate("goggles.seeker_table.marker")
                .add(Lang.builder("joc").text(fmt(table.markerX) + ", " + fmt(table.markerZ)).style(ChatFormatting.WHITE))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
            Vec3 center = table.getWorldCenter();
            double distance = Math.hypot(table.markerX - center.x, table.markerZ - center.z);
            Lang.builder("joc")
                .translate("goggles.seeker.distance")
                .add(Lang.builder("joc").text(fmt(distance)).style(ChatFormatting.GREEN))
                .translate("goggles.seeker.distance_unit")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
        } else {
            Lang.builder("joc")
                .translate("goggles.seeker_table.no_marker")
                .style(ChatFormatting.DARK_GRAY)
                .forGoggles(tooltip, 1);
        }
        return true;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
