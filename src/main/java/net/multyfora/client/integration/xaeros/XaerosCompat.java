package net.multyfora.client.integration.xaeros;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.multyfora.AeronauticsJoyofcreation;
import net.neoforged.fml.ModList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


//fucked up class, ill fix this later idk
public final class XaerosCompat {
    public static final String MINIMAP_MOD_ID = "xaerominimap";
    public static final String WORLD_MAP_MOD_ID = "xaeroworldmap";

    private static final String LOG_PREFIX = "[XaerosCompat]";
    private static Boolean loaded;

    private static Object cachedMinimapModule;
    private static Method cachedGetSession;
    private static Object cachedSession;
    private static Method cachedGetWorldManager;
    private static Object cachedWorldManager;
    private static Method cachedGetCurrentWorld;
    private static Object cachedCurrentWorld;
    private static Object cachedDimKey;
    private static Method cachedGetWaypointSets;
    private static Class<?> cachedWaypointClass;
    private static Method cachedGetName;
    private static Method cachedGetInitials;
    private static Method cachedGetX;
    private static Method cachedGetZ;
    private static Method cachedGetY;
    private static Method cachedGetWaypointColor;
    private static Method cachedGetColor;
    private static Field cachedColorField;
    private static String cachedSetListFieldName;

    private XaerosCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(MINIMAP_MOD_ID)
                    || ModList.get().isLoaded(WORLD_MAP_MOD_ID);
            if (loaded) {
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " Xaero mod(s) detected");
            }
        }
        return loaded;
    }

    public static List<WaypointData> getCurrentWaypoints(Level level) {
        List<WaypointData> result = new ArrayList<>();
        if (!isLoaded()) return result;

        try {
            Object session = resolveSession();
            if (session == null) {
                AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " no session");
                return result;
            }

            Object currentWorld = resolveCurrentWorld(session);
            if (currentWorld == null) {
                AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " no current world");
                return result;
            }

            Object dimKey = resolveDimKey(currentWorld);
            if (dimKey == null) {
                AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " no dim key on world");
                return result;
            }
            if (!matchesDimension(dimKey, level)) {
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " wrong dimension, skipping (dimKey={} [{}], expected={})",
                        dimKey, dimKey.getClass().getName(), level.dimension().location());
                return result;
            }

            Iterable<?> waypointSets = resolveWaypointSets(currentWorld);
            if (waypointSets != null) {
                int setCount = 0;
                for (Object set : waypointSets) {
                    setCount++;
                    List<?> wpList = readWaypointList(set);
                    if (wpList == null || wpList.isEmpty()) continue;

                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " found server waypoint set with {} entries", wpList.size());

                    for (Object wp : wpList) {
                        WaypointData data = readWaypoint(wp);
                        if (data != null) result.add(data);
                    }
                }
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " server waypoints: {} across {} set(s)", result.size(), setCount);
            } else {
                AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " no waypoint sets resolver");
            }

            if (cachedWorldManager != null) {
                int customBefore = result.size();
                resolveCustomWaypoints(cachedWorldManager, dimKey, result);
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " custom waypoints: {}", result.size() - customBefore);
            }

            if (session != null) {
                int sessionBefore = result.size();
                resolveSessionWaypoints(session, dimKey, result);
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " session waypoints: {}", result.size() - sessionBefore);
            }

            if (session != null) {
                int mapBefore = result.size();
                resolveWaypointMap(session, dimKey, result);
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " waypoint-map waypoints: {}", result.size() - mapBefore);
            }

            if (session != null) {
                int htBefore = result.size();
                resolveCustomWaypointsHashtable(session, dimKey, result);
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " hashtable waypoints: {}", result.size() - htBefore);
            }

            if (dimKey instanceof ResourceKey<?> rk) {
                int fileBefore = result.size();
                resolveFileWaypoints(level, rk, result);
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " file waypoints: {}", result.size() - fileBefore);
            }
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " failed to read Xaero waypoints", e);
        }

        return result;
    }

    private static Object resolveSession() {
        if (cachedSession != null) return cachedSession;
        try {
            if (cachedMinimapModule == null) {
                Class<?> clazz = Class.forName("xaero.hud.minimap.BuiltInHudModules");
                dumpClass(clazz, "BuiltInHudModules");

                try {
                    Field f = clazz.getDeclaredField("MINIMAP");
                    f.setAccessible(true);
                    cachedMinimapModule = f.get(null);
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " BuiltInHudModules.MINIMAP via field = {}", cachedMinimapModule);
                } catch (NoSuchFieldException ef) {
                    Object[] constants = clazz.getEnumConstants();
                    if (constants != null && constants.length > 0) {
                        cachedMinimapModule = constants[0];
                        AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " BuiltInHudModules fallback to constants[0] = {}", cachedMinimapModule);
                    }
                }
            }

            if (cachedMinimapModule == null) {
                AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " cannot get minimap module");
                return null;
            }

            if (cachedGetSession == null) {
                dumpClass(cachedMinimapModule.getClass(), "MinimapModule (runtime)");
                cachedGetSession = findMethod(cachedMinimapModule.getClass(),
                        "getCurrentSession", "getSession");
            }
            if (cachedGetSession == null) {
                AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " no getCurrentSession on {}",
                        cachedMinimapModule.getClass());
                return null;
            }
            cachedSession = cachedGetSession.invoke(cachedMinimapModule);
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " session = {}", cachedSession);
            return cachedSession;
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " resolveSession failed", e);
            return null;
        }
    }

    private static Object resolveCurrentWorld(Object session) {
        if (cachedCurrentWorld != null) return cachedCurrentWorld;
        try {
            dumpClass(session.getClass(), "Session");

            if (cachedGetWorldManager == null) {
                cachedGetWorldManager = findMethod(session.getClass(),
                        "getWorldManager", "getWorldManager");
            }
            if (cachedGetWorldManager == null) {
                AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " no getWorldManager on {}", session.getClass());
                return null;
            }
            cachedWorldManager = cachedGetWorldManager.invoke(session);
            if (cachedWorldManager == null) return null;
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " worldManager = {}", cachedWorldManager.getClass());
            dumpClass(cachedWorldManager.getClass(), "WorldManager");

            if (cachedGetCurrentWorld == null) {
                cachedGetCurrentWorld = findMethod(cachedWorldManager.getClass(),
                        "getCurrentWorld", "getWorld", "getMinimapWorld");
            }
            if (cachedGetCurrentWorld == null) {
                AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " no getCurrentWorld on {}", cachedWorldManager.getClass());
                return null;
            }
            cachedCurrentWorld = cachedGetCurrentWorld.invoke(cachedWorldManager);
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " currentWorld = {}", cachedCurrentWorld != null ? cachedCurrentWorld.getClass() : "null");
            if (cachedCurrentWorld != null) {
                dumpClass(cachedCurrentWorld.getClass(), "MinimapWorld");
            }
            return cachedCurrentWorld;
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " resolveCurrentWorld failed", e);
            return null;
        }
    }

    private static Object resolveDimKey(Object world) {
        if (cachedDimKey != null) return cachedDimKey;
        try {
            Method m = findMethod(world.getClass(), "getDimId", "getDimension", "getDimensionKey");
            if (m == null) return null;
            cachedDimKey = m.invoke(world);
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " dimKey = {}", cachedDimKey);
            return cachedDimKey;
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " resolveDimKey failed", e);
            return null;
        }
    }

    private static Iterable<?> resolveWaypointSets(Object world) {
        try {
            if (cachedGetWaypointSets == null) {
                cachedGetWaypointSets = findMethod(world.getClass(),
                        "getIterableWaypointSets", "getWaypointSets", "getSets",
                        "getAllWaypointSets", "getWaypointSetList");
            }
            if (cachedGetWaypointSets == null) {
                AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " no waypoint-set method on {}", world.getClass());
                return null;
            }
            Object raw = cachedGetWaypointSets.invoke(world);
            if (raw instanceof Iterable<?> iter) {
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " waypoint sets = {}", raw.getClass());
                return iter;
            }
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " waypoint sets returned single object: {}", raw != null ? raw.getClass() : "null");
            return raw != null ? List.of(raw) : null;
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.error(LOG_PREFIX + " resolveWaypointSets failed", e);
            return null;
        }
    }

    private static void resolveCustomWaypoints(Object worldManager, Object dimKey, List<WaypointData> out) {
        try {
            if (dimKey instanceof ResourceKey<?> rk) {
                try {
                    Method m = worldManager.getClass().getMethod("getCustomWaypoints", ResourceLocation.class);
                    Object map = m.invoke(worldManager, rk.location());
                    if (map instanceof it.unimi.dsi.fastutil.ints.Int2ObjectMap<?> i2o) {
                        if (i2o.isEmpty()) {
                            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " custom Waypoints Int2ObjectMap: empty");
                        } else {
                            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " custom Waypoints Int2ObjectMap has {} entries", i2o.size());
                        }
                        for (Object wp : i2o.values()) {
                            WaypointData data = readWaypoint(wp);
                            if (data != null) out.add(data);
                        }
                        if (!i2o.isEmpty()) return;
                    }
                } catch (NoSuchMethodException e) {
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " getCustomWaypoints(ResourceLocation) not found");
                }
            }

            try {
                Method m = worldManager.getClass().getMethod("getCustomWaypoints");
                Object raw = m.invoke(worldManager);
                if (raw instanceof Iterable<?> iter) {
                    int count = 0;
                    for (Object wp : iter) {
                        count++;
                        if (matchesWaypointDim(wp, dimKey)) {
                            WaypointData data = readWaypoint(wp);
                            if (data != null) out.add(data);
                        }
                    }
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " custom waypoints iterable had {} entries", count);
                }
            } catch (NoSuchMethodException e) {
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " getCustomWaypoints() not found");
            }
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " resolveCustomWaypoints failed", e);
        }
    }

    private static boolean matchesWaypointDim(Object wp, Object dimKey) {
        try {
            Method m = findMethod(wp.getClass(), "getDimension", "getDimId", "getWorld");
            if (m != null) {
                Object wpDim = m.invoke(wp);
                return dimKey.equals(wpDim) || dimKey.toString().equals(wpDim.toString());
            }
        } catch (Exception ignored) {}
        return true;
    }

    private static void resolveSessionWaypoints(Object session, Object dimKey, List<WaypointData> out) {
        try {
            if (cachedWorldManager != null) {
                try {
                    Method hasM = cachedWorldManager.getClass().getMethod("hasCustomWaypoints");
                    boolean has = (boolean) hasM.invoke(cachedWorldManager);
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " MinimapWorldManager.hasCustomWaypoints() = {}", has);
                } catch (NoSuchMethodException e) {
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " hasCustomWaypoints() not found");
                }
            }

            Method getWp = findMethod(session.getClass(), "getWaypoints", "getCurrentWaypoints");
            if (getWp != null) {
                Object wpSet = getWp.invoke(session);
                if (wpSet != null) {
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " session.getWaypoints() returned {}", wpSet.getClass());
                    List<?> list = readWaypointList(wpSet);
                    if (list != null && !list.isEmpty()) {
                        AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " session WaypointSet has {} entries", list.size());
                        for (Object wp : list) {
                            WaypointData data = readWaypoint(wp);
                            if (data != null) out.add(data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " resolveSessionWaypoints failed", e);
        }
    }

    private static void resolveWaypointMap(Object session, Object dimKey, List<WaypointData> out) {
        try {
            Method m = findMethod(session.getClass(), "getWaypointMap", "getWaypointsMap");
            if (m != null) {
                Object map = m.invoke(session);
                if (map instanceof java.util.Map<?, ?> mp) {
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " session.getWaypointMap() has {} entries", mp.size());
                    for (Object wp : mp.values()) {
                        WaypointData data = readWaypoint(wp);
                        if (data != null) out.add(data);
                    }
                }
            }
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " resolveWaypointMap failed", e);
        }
    }

    private static void resolveCustomWaypointsHashtable(Object session, Object dimKey, List<WaypointData> out) {
        try {
            Field f = session.getClass().getDeclaredField("customWaypoints");
            f.setAccessible(true);
            Object raw = f.get(session);
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " session.customWaypoints field = {}", raw != null ? raw.getClass() : "null");

            if (raw instanceof java.util.Hashtable<?, ?> top) {
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " session.customWaypoints has {} top-level keys", top.size());
                for (var entry : top.entrySet()) {
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + "   key={}", entry.getKey());
                    Object val = entry.getValue();
                    if (val instanceof java.util.Hashtable<?, ?> mid) {
                        for (var sub : mid.entrySet()) {
                            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + "     sub-key={}", sub.getKey());
                            Object wpSet = sub.getValue();
                            if (wpSet != null) {
                                List<?> list = readWaypointList(wpSet);
                                if (list != null && !list.isEmpty()) {
                                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + "       -> {} waypoints", list.size());
                                    for (Object wp : list) {
                                        WaypointData data = readWaypoint(wp);
                                        if (data != null) out.add(data);
                                    }
                                }
                            }
                        }
                    } else if (val != null) {
                        List<?> list = readWaypointList(val);
                        if (list != null && !list.isEmpty()) {
                            for (Object wp : list) {
                                WaypointData data = readWaypoint(wp);
                                if (data != null) out.add(data);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " resolveCustomWaypointsHashtable failed", e);
        }
    }

    private static void resolveFileWaypoints(Level level, ResourceKey<?> dimKey, List<WaypointData> out) {
        try {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            String worldName = "unknown";
            if (level.getServer() != null) {
                worldName = level.getServer().getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
            }
            String dimId = "dim%0";
            if (dimKey.equals(Level.NETHER)) dimId = "dim%-1";
            else if (dimKey.equals(Level.END)) dimId = "dim%1";

            Path wpFile = gameDir.resolve("xaero").resolve("minimap").resolve(worldName).resolve(dimId).resolve("waypoints.txt");
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " trying file: {}", wpFile);

            if (!wpFile.toFile().exists()) {
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " file not found, trying all subdirs in xaero/minimap");
                Path minimapDir = gameDir.resolve("xaero").resolve("minimap");
                if (minimapDir.toFile().isDirectory()) {
                    for (String dir : minimapDir.toFile().list()) {
                        Path candidate = minimapDir.resolve(dir).resolve(dimId).resolve("waypoints.txt");
                        if (candidate.toFile().exists()) {
                            wpFile = candidate;
                            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " found file: {}", wpFile);
                            break;
                        }
                    }
                }
                if (!wpFile.toFile().exists()) {
                    AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " no waypoints.txt found on disk");
                    return;
                }
            }

            int count = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(wpFile.toFile()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (!line.startsWith("waypoint:")) continue;

                    String[] parts = line.split(":", -1);
                    // waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination
                    if (parts.length < 7) continue;
                    String name = parts[1];
                    String initials = parts[2];
                    int x = parseIntSafe(parts[3], 0);
                    int y = parseIntSafe(parts[4], 64);
                    int z = parseIntSafe(parts[5], 0);
                    int color = parseIntSafe(parts[6], 0xFF4488CC);

                    out.add(new WaypointData(name, initials, x, y, z, color));
                    count++;
                }
            }
            AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " read {} waypoints from file", count);
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " resolveFileWaypoints failed", e);
        }
    }

    private static int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static List<?> readWaypointList(Object set) {
        dumpClass(set.getClass(), "WaypointSet");
        String[] fieldNames = {"list", "waypoints", "waypointList", "elements", "waypointListField"};
        try {
            for (Method m : set.getClass().getMethods()) {
                if (m.getReturnType() == List.class && m.getParameterCount() == 0
                        && m.getName().startsWith("get")) {
                    Object val = m.invoke(set);
                    if (val instanceof List<?> list) {
                        if (!list.isEmpty()) {
                            String elemName = list.get(0).getClass().getSimpleName();
                            if (elemName.equals("Waypoint") || elemName.contains("Waypoint")) {
                                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " found waypoint list via {} ({} entries)", m.getName(), list.size());
                                return list;
                            }
                        }
                    }
                }
            }

            for (String name : fieldNames) {
                try {
                    Field f = set.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    Object val = f.get(set);
                    if (val instanceof List<?> list) {
                        AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " found waypoint list via field '{}' ({} entries)", name, list.size());
                        return list;
                    }
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " readWaypointList failed for {}", set.getClass(), e);
        }
        return null;
    }

    private static WaypointData readWaypoint(Object wp) {
        try {
            if (cachedWaypointClass == null) {
                cachedWaypointClass = wp.getClass();
                dumpClass(cachedWaypointClass, "Waypoint");
            }
            if (cachedGetName == null) cachedGetName = findMethod(cachedWaypointClass, "getName", "getWaypointName");
            if (cachedGetInitials == null) cachedGetInitials = findMethod(cachedWaypointClass, "getInitials", "getSymbol", "getShortName");
            if (cachedGetY == null) cachedGetY = findMethod(cachedWaypointClass, "getY", "getYCoordinate");

            if (cachedGetX == null) {
                try { cachedGetX = cachedWaypointClass.getMethod("getX", double.class);
                } catch (NoSuchMethodException e) { cachedGetX = findMethod(cachedWaypointClass, "getX"); }
            }
            if (cachedGetZ == null) {
                try { cachedGetZ = cachedWaypointClass.getMethod("getZ", double.class);
                } catch (NoSuchMethodException e) { cachedGetZ = findMethod(cachedWaypointClass, "getZ"); }
            }

            if (cachedGetName == null || cachedGetX == null || cachedGetZ == null || cachedGetY == null) {
                AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " missing waypoint accessors");
                return null;
            }

            String name = (String) cachedGetName.invoke(wp);
            String initials = cachedGetInitials != null ? (String) cachedGetInitials.invoke(wp) : "";

            int x, z;
            if (cachedGetX.getParameterCount() > 0) {
                x = (int) cachedGetX.invoke(wp, 1.0);
                z = (int) cachedGetZ.invoke(wp, 1.0);
            } else {
                x = (int) cachedGetX.invoke(wp);
                z = (int) cachedGetZ.invoke(wp);
            }
            int y = (int) cachedGetY.invoke(wp);

            int color = 0xFF4488CC;
            try {
                if (cachedGetWaypointColor == null)
                    cachedGetWaypointColor = findMethod(cachedWaypointClass, "getWaypointColor", "getColor");
                if (cachedGetWaypointColor != null) {
                    Object wpColor = cachedGetWaypointColor.invoke(wp);
                    if (wpColor != null) {
                        if (cachedGetColor == null) {
                            cachedGetColor = findMethod(wpColor.getClass(), "getColor", "getRGB");
                            if (cachedGetColor == null) {
                                try {
                                    cachedColorField = wpColor.getClass().getDeclaredField("color");
                                    cachedColorField.setAccessible(true);
                                } catch (NoSuchFieldException ignored) {}
                            }
                        }
                        if (cachedGetColor != null) color = (int) cachedGetColor.invoke(wpColor);
                        else if (cachedColorField != null) color = cachedColorField.getInt(wpColor);
                    }
                }
            } catch (Exception e) {
                AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " failed to read waypoint color", e);
            }

            return new WaypointData(name, initials, x, y, z, color);
        } catch (Exception e) {
            AeronauticsJoyofcreation.LOGGER.warn(LOG_PREFIX + " readWaypoint failed", e);
            return null;
        }
    }

    private static boolean matchesDimension(Object dimKey, Level level) {
        if (dimKey instanceof ResourceKey<?> rk) {
            return rk.location().equals(level.dimension().location());
        }
        String dimStr = dimKey.toString();
        ResourceLocation loc = level.dimension().location();
        return dimStr.equals(loc.toString()) || dimStr.equals(loc.getPath());
    }

    private static void dumpClass(Class<?> clazz, String label) {
        StringBuilder sb = new StringBuilder();
        sb.append(LOG_PREFIX).append(" dump [").append(label).append("] ").append(clazz.getName()).append("\n");
        sb.append(LOG_PREFIX).append("   Methods:\n");
        for (Method m : clazz.getMethods()) {
            sb.append(LOG_PREFIX).append("     ").append(m.getReturnType().getSimpleName()).append(" ")
                    .append(m.getName()).append("(");
            Class<?>[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i].getSimpleName());
            }
            sb.append(")\n");
        }
        sb.append(LOG_PREFIX).append("   Declared fields:\n");
        for (Field f : clazz.getDeclaredFields()) {
            sb.append(LOG_PREFIX).append("     ").append(f.getType().getSimpleName()).append(" ").append(f.getName()).append("\n");
        }
        AeronauticsJoyofcreation.LOGGER.info(sb.toString());
    }

    private static Method findMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                Method m = clazz.getMethod(name);
                AeronauticsJoyofcreation.LOGGER.info(LOG_PREFIX + " found method {}.{}()", clazz.getSimpleName(), name);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    public static void invalidateCache() {
        cachedMinimapModule = null;
        cachedGetSession = null;
        cachedSession = null;
        cachedGetWorldManager = null;
        cachedWorldManager = null;
        cachedGetCurrentWorld = null;
        cachedCurrentWorld = null;
        cachedDimKey = null;
        cachedGetWaypointSets = null;
        cachedWaypointClass = null;
        cachedGetName = null;
        cachedGetInitials = null;
        cachedGetX = null;
        cachedGetZ = null;
        cachedGetY = null;
        cachedGetWaypointColor = null;
        cachedGetColor = null;
        cachedColorField = null;
        cachedSetListFieldName = null;
    }
}
