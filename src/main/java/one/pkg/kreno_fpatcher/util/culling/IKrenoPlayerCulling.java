package one.pkg.kreno_fpatcher.util.culling;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public interface IKrenoPlayerCulling {
    Int2ObjectOpenHashMap<ServerCullingManager.CullingState> kreno$getVisibilityCache();

    ServerCullingManager.ParticleCullCache kreno$getParticleCache();

    ServerCullingManager.EntityCullCache kreno$getEntityCache();

    long kreno$getLastLookTick();

    void kreno$setLook(double x, double y, double z, long tickCount);

    double kreno$getLookX();

    double kreno$getLookY();

    double kreno$getLookZ();
}
