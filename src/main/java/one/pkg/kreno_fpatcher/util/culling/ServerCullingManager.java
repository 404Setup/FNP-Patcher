package one.pkg.kreno_fpatcher.util.culling;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import one.pkg.kreno_fpatcher.ModConfig;

import java.util.Arrays;

public class ServerCullingManager {
    public static final double NEAR_DISTANCE_SQ = 64.0;
    private static final float DEG_TO_RAD = (float) Math.PI / 180F;
    private static final long CHECK_INTERVAL_TICKS = 10;
    private static final long HIDE_DELAY_TICKS = 20;

    public static void updatePlayerLook(ServerPlayer player, long tickCount) {
        IKrenoPlayerCulling p = (IKrenoPlayerCulling) player;
        if (p.kreno$getLastLookTick() != tickCount) {
            float rotX = player.getXRot();
            float rotY = player.getYRot();
            float f = rotX * DEG_TO_RAD;
            float g = -rotY * DEG_TO_RAD;
            float cosG = Mth.cos(g);
            float sinG = Mth.sin(g);
            float cosF = Mth.cos(f);
            float sinF = Mth.sin(f);
            p.kreno$setLook(sinG * cosF, -sinF, cosG * cosF, tickCount);
        }
    }

    public static boolean isEntityVisible(ServerPlayer player, Entity entity, long tickCount) {
        if (!ModConfig.Culling.isEntityEnabled() || !player.level().getServer().isDedicatedServer()) return true;

        CullingState state = getEntityCullingState(player, entity);

        float ex = (float) player.getX();
        float ey = (float) player.getEyeY();
        float ez = (float) player.getZ();
        float cx = (float) entity.getX();
        float cy = (float) (entity.getY() + entity.getBbHeight() / 2.0);
        float cz = (float) entity.getZ();
        float rotX = player.getXRot();
        float rotY = player.getYRot();

        if (checkAndUpdateStatePosition(state, ex, ey, ez, cx, cy, cz, rotX, rotY)) {
            return state.isCurrentlyVisible;
        }

        float dx = cx - ex;
        float dy = cy - ey;
        float dz = cz - ez;
        float distanceSq = dx * dx + dy * dy + dz * dz;
        state.lastDistanceSq = distanceSq;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            state.lastRaytraceResult = true;
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        IKrenoPlayerCulling cullPlayer = (IKrenoPlayerCulling) player;
        updatePlayerLook(player, tickCount);
        EntityCullCache hashCache = cullPlayer.kreno$getEntityCache();
        hashCache.clearIfNewTick(tickCount);

        long gridKey = toGridKey(cx, cy, cz);

        if (hashCache.grid.contains(gridKey)) {
            state.isCurrentlyVisible = true;
            state.hiddenSince = 0;
            return true;
        }

        double lookX = cullPlayer.kreno$getLookX();
        double lookY = cullPlayer.kreno$getLookY();
        double lookZ = cullPlayer.kreno$getLookZ();
        double dot = lookX * dx + lookY * dy + lookZ * dz;
        boolean inFOV = dot >= 0.0;

        if (inFOV) {
            boolean justEnteredFOV = !state.wasInFOV;
            state.wasInFOV = true;
            if (justEnteredFOV || tickCount - state.lastCheckTick > CHECK_INTERVAL_TICKS) {
                state.lastCheckTick = tickCount;
                try {
                    state.lastRaytraceResult = checkAABBVisibleInflated(player.level(), ex, ey, ez, entity.getBoundingBox(), 0.5);
                } catch (Exception e) {
                    state.lastRaytraceResult = true;
                }
            }
        } else {
            state.wasInFOV = false;
        }

        boolean isVisible = inFOV && state.lastRaytraceResult;
        updateVisibilityState(state, isVisible, tickCount);

        if (state.isCurrentlyVisible) {
            hashCache.grid.add(gridKey);
        }

        return state.isCurrentlyVisible;
    }

    public static boolean getLastSentVisible(ServerPlayer player, Entity entity) {
        Int2ObjectOpenHashMap<CullingState> map = ((IKrenoPlayerCulling) player).kreno$getVisibilityCache();
        CullingState state = map.get(entity.getId());
        if (state == null) return true;
        return state.lastSentVisible;
    }

    public static void setLastSentVisible(ServerPlayer player, Entity entity, boolean visible) {
        CullingState state = getEntityCullingState(player, entity);
        state.lastSentVisible = visible;
    }

    public static void removePlayerEntityState(ServerPlayer player, Entity entity) {
        Int2ObjectOpenHashMap<CullingState> map = ((IKrenoPlayerCulling) player).kreno$getVisibilityCache();
        map.remove(entity.getId());
    }

    private static CullingState getEntityCullingState(ServerPlayer player, Entity entity) {
        Int2ObjectOpenHashMap<CullingState> map = ((IKrenoPlayerCulling) player).kreno$getVisibilityCache();
        int entityId = entity.getId();
        CullingState state = map.get(entityId);
        if (state == null) {
            state = new CullingState();
            map.put(entityId, state);
        }
        return state;
    }

    private static boolean checkAndUpdateStatePosition(CullingState state, float ex, float ey, float ez, float cx, float cy, float cz, float rotX, float rotY) {
        if (Math.abs(state.lastPx - ex) < 0.1f && Math.abs(state.lastPy - ey) < 0.1f && Math.abs(state.lastPz - ez) < 0.1f &&
                Math.abs(state.lastTx - cx) < 0.1f && Math.abs(state.lastTy - cy) < 0.1f && Math.abs(state.lastTz - cz) < 0.1f &&
                Math.abs(state.lastRotX - rotX) < 1.0f && Math.abs(state.lastRotY - rotY) < 1.0f) {
            return true;
        }

        state.lastPx = ex;
        state.lastPy = ey;
        state.lastPz = ez;
        state.lastTx = cx;
        state.lastTy = cy;
        state.lastTz = cz;
        state.lastRotX = rotX;
        state.lastRotY = rotY;
        return false;
    }

    private static void updateVisibilityState(CullingState state, boolean isVisible, long tickCount) {
        if (!isVisible) {
            if (state.hiddenSince == 0) {
                state.hiddenSince = tickCount;
            } else if (tickCount - state.hiddenSince > HIDE_DELAY_TICKS) {
                state.isCurrentlyVisible = false;
            }
        } else {
            state.hiddenSince = 0;
            state.isCurrentlyVisible = true;
        }
    }

    public static boolean checkAABBVisibleInflated(Level level, double sx, double sy, double sz, AABB aabb, double inflate) {
        double minX = aabb.minX - inflate;
        double minY = aabb.minY - inflate;
        double minZ = aabb.minZ - inflate;
        double maxX = aabb.maxX + inflate;
        double maxY = aabb.maxY + inflate;
        double maxZ = aabb.maxZ + inflate;

        double cx = minX + (maxX - minX) * 0.5;
        double cy = minY + (maxY - minY) * 0.5;
        double cz = minZ + (maxZ - minZ) * 0.5;

        if (isLineOfSightClear(level, sx, sy, sz, cx, cy, cz)) return true;
        return isLineOfSightClear(level, sx, sy, sz, cx, maxY, cz);
    }

    public static boolean isLineOfSightClear(Level level, double sx, double sy, double sz, double ex, double ey, double ez) {
        try {
            ClipContext ctx = new ClipContext(
                    new Vec3(sx, sy, sz),
                    new Vec3(ex, ey, ez),
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            );
            return level.clip(ctx).getType() == HitResult.Type.MISS;
        } catch (Throwable t) {
            return true;
        }
    }

    public static void removeEntity(Entity entity) {
        int id = entity.getId();
        if (entity.level() != null && entity.level().getServer() != null &&
                entity.level().getServer().getPlayerList() != null) {
            for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
                ((IKrenoPlayerCulling) player).kreno$getVisibilityCache().remove(id);
            }
        }
    }

    private static long toGridKey(float cx, float cy, float cz) {
        int gridX = Mth.floor(cx * 0.125f);
        int gridY = Mth.floor(cy * 0.125f);
        int gridZ = Mth.floor(cz * 0.125f);
        return ((long) (gridX & 0x3FFFFF) << 42) | ((long) (gridY & 0xFFFFF) << 22) | (gridZ & 0x3FFFFF);
    }

    public static boolean isParticleVisible(ServerPlayer player, double x, double y, double z) {
        if (!ModConfig.Culling.isParticleEnabled() || !player.level().getServer().isDedicatedServer()) return true;

        long tickCount = player.level().getServer().getTickCount();
        updatePlayerLook(player, tickCount);
        IKrenoPlayerCulling cullPlayer = (IKrenoPlayerCulling) player;
        ParticleCullCache cache = cullPlayer.kreno$getParticleCache();

        double eyeX = player.getX();
        double eyeY = player.getEyeY();
        double eyeZ = player.getZ();
        double dx = x - eyeX;
        double dy = y - eyeY;
        double dz = z - eyeZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        if (distanceSq < NEAR_DISTANCE_SQ) {
            return true;
        }

        double lookX = cullPlayer.kreno$getLookX();
        double lookY = cullPlayer.kreno$getLookY();
        double lookZ = cullPlayer.kreno$getLookZ();
        double dot = lookX * dx + lookY * dy + lookZ * dz;
        boolean inFOV = dot >= 0.0;

        if (!inFOV) return false;

        int bx = Mth.floor(x);
        int by = Mth.floor(y);
        int bz = Mth.floor(z);
        long key = ((long) bx & 0x3FFFFFFL) | (((long) by & 0xFFFL) << 26) | (((long) bz & 0x3FFFFFFL) << 38);

        int cachedVal = cache.get(key, tickCount);
        if (cachedVal != -1) {
            return cachedVal == 1;
        }

        boolean result = isLineOfSightClear(player.level(), eyeX, eyeY, eyeZ, x, y, z);
        cache.put(key, result, tickCount);

        return result;
    }

    public static class ParticleCullCache {
        private static final int CACHE_SIZE = 128;
        private final long[] keys = new long[CACHE_SIZE];
        private final boolean[] values = new boolean[CACHE_SIZE];
        private final long[] times = new long[CACHE_SIZE];

        public ParticleCullCache() {
            Arrays.fill(keys, -1L);
        }

        public int get(long key, long tickCount) {
            int index = (int) (key & 127);
            if (keys[index] == key && tickCount - times[index] < 20) {
                return values[index] ? 1 : 0;
            }
            return -1;
        }

        public void put(long key, boolean value, long tickCount) {
            int index = (int) (key & 127);
            keys[index] = key;
            values[index] = value;
            times[index] = tickCount;
        }
    }

    public static class EntityCullCache {
        public final LongOpenHashSet grid = new LongOpenHashSet();
        private long lastTick = -1;

        public void clearIfNewTick(long tickCount) {
            if (this.lastTick != tickCount) {
                this.grid.clear();
                this.lastTick = tickCount;
            }
        }
    }

    public static class CullingState {
        public boolean lastRaytraceResult = true;
        public boolean isCurrentlyVisible = true;
        public long lastCheckTick = 0;
        public long hiddenSince = 0;
        public float lastDistanceSq = 0;
        public boolean lastSentVisible = true;
        public float lastPx = Float.MAX_VALUE;
        public float lastPy = Float.MAX_VALUE;
        public float lastPz = Float.MAX_VALUE;
        public float lastTx = Float.MAX_VALUE;
        public float lastTy = Float.MAX_VALUE;
        public float lastTz = Float.MAX_VALUE;
        public float lastRotX = Float.MAX_VALUE;
        public float lastRotY = Float.MAX_VALUE;
        public boolean wasInFOV = false;
    }
}
