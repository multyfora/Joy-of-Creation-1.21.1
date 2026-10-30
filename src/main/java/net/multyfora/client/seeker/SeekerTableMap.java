package net.multyfora.client.seeker;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import net.multyfora.AeronauticsJoyofcreation;
import net.multyfora.content.seeker.SeekerTableBlockEntity;
import net.multyfora.content.seeker.SeekerTableMarker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class SeekerTableMap {

    private static final int MAX_SIZE = 512;
    private static final int SAMPLES_PER_TICK = 8192;
    private static final int RESCAN_DELAY_TICKS = 40;
    private static final int GRID_SPACING = 64;
    private static final int UNKNOWN_HEIGHT = Integer.MIN_VALUE;
    private static final double FLOOR_PERCENTILE = 0.1;
    private static final float FLOOR_EASE = 0.1f;
    private static final int GROUND_SEARCH = 40;

    private static final int PARCHMENT = abgr(236, 205, 147);
    private static final int PARCHMENT_DARK = abgr(224, 191, 132);
    private static final int PARCHMENT_LIGHT = abgr(239, 212, 161);
    private static final int CONTOUR = abgr(92, 58, 30);

    private static final Set<SeekerTableMap> LIVE = new HashSet<>();
    private static int nextId;

    private final ResourceLocation location;
    private final DynamicTexture texture;
    private final NativeImage image;
    private final int size;
    private final int blocksPerPixel;
    private final int scale, width, depth;

    private int originX, originZ;
    private boolean placed;
    private int row;
    private int[] previousHeights;
    private int[] currentHeights;
    private int[] previousGround;
    private int[] currentGround;
    private final int contourInterval;
    private final float[] ground;
    private final float[] relief;
    private final int blurRadius;
    private float floor = Float.NaN, shownFloor = Float.NaN;
    private int idleTicks;
    private long lastTick = Long.MIN_VALUE;
    private boolean dirty;

    double shownMarkerX = Double.NaN, shownMarkerZ = Double.NaN;
    SeekerTableMarker shownMarkerStyle;
    long markerPlacedAt;
    long lastParticleTick;

    private SeekerTableMap(int scale, int width, int depth) {
        this.scale = scale;
        this.width = width;
        this.depth = depth;
        int span = Mth.ceil(Math.hypot(width, depth) * scale) + 4;
        blocksPerPixel = Math.max(1, Mth.ceil(span / (double) MAX_SIZE));
        size = Mth.roundToward(Mth.ceil(span / (double) blocksPerPixel), 8);
        image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        image.fillRect(0, 0, size, size, PARCHMENT);
        texture = new DynamicTexture(image);
        location = ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "seeker_table_map/" + nextId++);
        Minecraft.getInstance().getTextureManager().register(location, texture);
        previousHeights = new int[size];
        currentHeights = new int[size];
        previousGround = new int[size];
        currentGround = new int[size];
        contourInterval = scale >= 64 ? 16 : 8;
        ground = new float[size * size];
        Arrays.fill(ground, Float.NaN);
        relief = new float[size * size];
        blurRadius = Math.max(1, Math.round(scale / SeekerTableRenderer.CELLS_PER_BLOCK / blocksPerPixel));
        LIVE.add(this);
    }

    public static SeekerTableMap get(SeekerTableBlockEntity table) {
        if (table.clientMap instanceof SeekerTableMap map
            && map.scale == table.getScale() && map.width == table.getWidth() && map.depth == table.getDepth())
            return map;
        release(table);
        SeekerTableMap map = new SeekerTableMap(table.getScale(), table.getWidth(), table.getDepth());
        table.clientMap = map;
        return map;
    }

    public static void release(SeekerTableBlockEntity table) {
        if (table.clientMap instanceof SeekerTableMap map)
            map.close();
        table.clientMap = null;
    }

    public static void releaseAll() {
        for (SeekerTableMap map : Set.copyOf(LIVE))
            map.close();
    }

    private void close() {
        if (LIVE.remove(this))
            Minecraft.getInstance().getTextureManager().release(location);
    }

    public ResourceLocation location() {
        return location;
    }

    public float u(double worldX) {
        return (float) ((worldX - originX) / (size * (double) blocksPerPixel));
    }

    public float v(double worldZ) {
        return (float) ((worldZ - originZ) / (size * (double) blocksPerPixel));
    }

    public float rise(double worldX, double worldZ) {
        if (Float.isNaN(shownFloor))
            return 0;
        double px = (worldX - originX) / blocksPerPixel - 0.5;
        double pz = (worldZ - originZ) / blocksPerPixel - 0.5;
        int x0 = Mth.floor(px), z0 = Mth.floor(pz);
        float fx = (float) (px - x0), fz = (float) (pz - z0);
        float height = Mth.lerp(fz,
            Mth.lerp(fx, reliefAt(x0, z0), reliefAt(x0 + 1, z0)),
            Mth.lerp(fx, reliefAt(x0, z0 + 1), reliefAt(x0 + 1, z0 + 1)));
        return Math.max(0, height - shownFloor);
    }

    private float reliefAt(int x, int z) {
        return x < 0 || z < 0 || x >= size || z >= size ? floor : relief[z * size + x];
    }

    public void tick(Level level, double centerX, double centerZ) {
        long time = level.getGameTime();
        if (time == lastTick)
            return;
        lastTick = time;

        recenter(centerX, centerZ);
        if (!Float.isNaN(floor))
            shownFloor = Float.isNaN(shownFloor) ? floor : Mth.lerp(FLOOR_EASE, shownFloor, floor);

        if (row >= size) {
            if (++idleTicks < RESCAN_DELAY_TICKS)
                return;
            idleTicks = 0;
            row = 0;
        }
        int rows = Math.max(1, SAMPLES_PER_TICK / size);
        for (int i = 0; i < rows && row < size; i++)
            sampleRow(level);
        if (row >= size)
            rebuildRelief();
        if (dirty) {
            texture.upload();
            dirty = false;
        }
    }

    private void recenter(double centerX, double centerZ) {
        int span = size * blocksPerPixel;
        int wantX = Mth.floor((centerX - span / 2.0) / blocksPerPixel) * blocksPerPixel;
        int wantZ = Mth.floor((centerZ - span / 2.0) / blocksPerPixel) * blocksPerPixel;
        if (!placed) {
            originX = wantX;
            originZ = wantZ;
            placed = true;
            row = 0;
            return;
        }
        int slack = span / 8;
        if (Math.abs(wantX - originX) < slack && Math.abs(wantZ - originZ) < slack)
            return;

        int shiftX = (wantX - originX) / blocksPerPixel;
        int shiftZ = (wantZ - originZ) / blocksPerPixel;
        int[] pixels = new int[size * size];
        float[] shifted = new float[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int sx = x + shiftX, sy = y + shiftZ;
                boolean known = sx >= 0 && sy >= 0 && sx < size && sy < size;
                pixels[y * size + x] = known
                    ? image.getPixelRGBA(sx, sy)
                    : unknown(wantX + x * blocksPerPixel, wantZ + y * blocksPerPixel);
                shifted[y * size + x] = known ? ground[sy * size + sx] : Float.NaN;
            }
        }
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                image.setPixelRGBA(x, y, pixels[y * size + x]);
        System.arraycopy(shifted, 0, ground, 0, ground.length);
        rebuildRelief();
        originX = wantX;
        originZ = wantZ;
        row = 0;
        idleTicks = 0;
        dirty = true;
    }

    private void sampleRow(Level level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int worldZ = originZ + row * blocksPerPixel + blocksPerPixel / 2;
        if (row == 0) {
            for (int x = 0; x < size; x++)
                previousHeights[x] = sample(level, originX + x * blocksPerPixel + blocksPerPixel / 2, worldZ - blocksPerPixel, pos, null);
            Arrays.fill(previousGround, UNKNOWN_HEIGHT);
        }
        int[] depthOut = new int[1];
        for (int x = 0; x < size; x++) {
            int worldX = originX + x * blocksPerPixel + blocksPerPixel / 2;
            int height = sample(level, worldX, worldZ, pos, depthOut);
            currentHeights[x] = height;
            currentGround[x] = UNKNOWN_HEIGHT;
            int color;
            if (height == UNKNOWN_HEIGHT) {
                color = unknown(worldX, worldZ);
                ground[row * size + x] = Float.NaN;
            } else {
                int groundY = groundHeight(level, worldX, height, worldZ, pos);
                ground[row * size + x] = groundY + 1;
                BlockState state = level.getBlockState(pos.set(worldX, height, worldZ));
                MapColor mapColor = state.getMapColor(level, pos);
                if (mapColor != MapColor.WATER)
                    currentGround[x] = groundY;
                boolean odd = ((worldX / blocksPerPixel + worldZ / blocksPerPixel) & 1) == 1;
                MapColor.Brightness brightness;
                if (mapColor == MapColor.WATER) {
                    double shade = depthOut[0] * 0.1 + (odd ? 0.2 : 0);
                    brightness = shade < 0.5 ? MapColor.Brightness.HIGH : shade > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                } else if (previousHeights[x] == UNKNOWN_HEIGHT) {
                    brightness = MapColor.Brightness.NORMAL;
                } else {
                    double slope = (height - previousHeights[x]) * 4.0 / (blocksPerPixel + 4) + ((odd ? 1 : 0) - 0.5) * 0.4;
                    brightness = slope > 0.6 ? MapColor.Brightness.HIGH : slope < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                }
                color = mapColor == MapColor.NONE ? unknown(worldX, worldZ) : lerp(mapColor.calculateRGBColor(brightness), PARCHMENT, 0.16f);
                if (onContour(currentGround[x], x > 0 ? currentGround[x - 1] : UNKNOWN_HEIGHT, previousGround[x]))
                    color = lerp(color, CONTOUR, 0.35f);
            }
            if (onGrid(worldX) || onGrid(worldZ))
                color = darken(color, 0.86f);
            image.setPixelRGBA(x, row, color);
        }
        int[] swap = previousHeights;
        previousHeights = currentHeights;
        currentHeights = swap;
        swap = previousGround;
        previousGround = currentGround;
        currentGround = swap;
        row++;
        dirty = true;
    }

    private static int groundHeight(Level level, int x, int y, int z, BlockPos.MutableBlockPos pos) {
        int floor = Math.max(level.getMinBuildHeight(), y - GROUND_SEARCH);
        int ground = y;
        while (ground > floor) {
            BlockState state = level.getBlockState(pos.set(x, ground, z));
            if (!state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && state.getMapColor(level, pos) != MapColor.NONE)
                break;
            ground--;
        }
        return ground;
    }

    private void rebuildRelief() {
        int step = Math.max(1, size / 128), count = 0;
        float[] known = new float[(size / step + 1) * (size / step + 1)];
        for (int z = 0; z < size; z += step)
            for (int x = 0; x < size; x += step)
                if (!Float.isNaN(ground[z * size + x]))
                    known[count++] = ground[z * size + x];
        if (count == 0)
            return;
        Arrays.sort(known, 0, count);
        floor = known[(int) (count * FLOOR_PERCENTILE)];

        float[] tmp = new float[size * size];
        for (int i = 0; i < relief.length; i++)
            relief[i] = Float.isNaN(ground[i]) ? floor : ground[i];
        for (int pass = 0; pass < 2; pass++) {
            blur(relief, tmp, 1, size);
            blur(tmp, relief, size, 1);
        }
    }

    private void blur(float[] from, float[] to, int step, int lineStep) {
        int r = blurRadius;
        float norm = 1f / (2 * r + 1);
        for (int line = 0; line < size; line++) {
            int base = line * lineStep;
            float sum = 0;
            for (int i = -r; i <= r; i++)
                sum += from[base + Mth.clamp(i, 0, size - 1) * step];
            for (int i = 0; i < size; i++) {
                to[base + i * step] = sum * norm;
                sum += from[base + Math.min(i + r + 1, size - 1) * step] - from[base + Math.max(i - r, 0) * step];
            }
        }
    }

    private boolean onContour(int ground, int west, int north) {
        if (ground == UNKNOWN_HEIGHT)
            return false;
        int band = Math.floorDiv(ground, contourInterval);
        return west != UNKNOWN_HEIGHT && band != Math.floorDiv(west, contourInterval)
            || north != UNKNOWN_HEIGHT && band != Math.floorDiv(north, contourInterval);
    }

    private boolean onGrid(int world) {
        return Math.floorMod(world, GRID_SPACING) < blocksPerPixel;
    }

    private static int sample(Level level, int x, int z, BlockPos.MutableBlockPos pos, int[] depthOut) {
        LevelChunk chunk = level.getChunkSource().getChunk(x >> 4, z >> 4, false);
        if (chunk == null)
            return UNKNOWN_HEIGHT;
        int minY = level.getMinBuildHeight();
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1;
        if (y <= minY + 1)
            return UNKNOWN_HEIGHT;
        BlockState state;
        do {
            state = chunk.getBlockState(pos.set(x, --y, z));
        } while (state.getMapColor(level, pos) == MapColor.NONE && y > minY);
        if (depthOut != null) {
            int depth = 0;
            if (!state.getFluidState().isEmpty()) {
                int below = y - 1;
                BlockState fluid;
                do {
                    fluid = chunk.getBlockState(pos.set(x, below--, z));
                    depth++;
                } while (below > minY && depth < 16 && !fluid.getFluidState().isEmpty());
            }
            depthOut[0] = depth;
        }
        return y;
    }

    private static int unknown(int worldX, int worldZ) {
        int hash = (worldX * 73856093) ^ (worldZ * 19349663);
        int n = Math.floorMod(hash >> 4, 16);
        return n < 2 ? PARCHMENT_DARK : n > 13 ? PARCHMENT_LIGHT : PARCHMENT;
    }

    private static int abgr(int r, int g, int b) {
        return 0xFF000000 | b << 16 | g << 8 | r;
    }

    private static int lerp(int from, int to, float t) {
        int r = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        int g = (int) Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF);
        int b = (int) Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF);
        return abgr(r, g, b);
    }

    private static int darken(int color, float factor) {
        return abgr((int) ((color & 0xFF) * factor), (int) ((color >> 8 & 0xFF) * factor), (int) ((color >> 16 & 0xFF) * factor));
    }
}
