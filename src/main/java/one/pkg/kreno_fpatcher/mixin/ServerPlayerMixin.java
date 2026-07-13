package one.pkg.kreno_fpatcher.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import one.pkg.kreno_fpatcher.util.culling.IKrenoPlayerCulling;
import one.pkg.kreno_fpatcher.util.culling.ServerCullingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements IKrenoPlayerCulling {
    @Unique
    private final Int2ObjectOpenHashMap<ServerCullingManager.CullingState> kreno$visibilityCache = new Int2ObjectOpenHashMap<>();
    @Unique
    private final ServerCullingManager.ParticleCullCache kreno$particleCache = new ServerCullingManager.ParticleCullCache();
    @Unique
    private final ServerCullingManager.EntityCullCache kreno$entityCache = new ServerCullingManager.EntityCullCache();

    @Unique
    private long kreno$lastLookTick = -1;
    @Unique
    private double kreno$lookX;
    @Unique
    private double kreno$lookY;
    @Unique
    private double kreno$lookZ;

    @Override
    public Int2ObjectOpenHashMap<ServerCullingManager.CullingState> kreno$getVisibilityCache() {
        return this.kreno$visibilityCache;
    }

    @Override
    public ServerCullingManager.ParticleCullCache kreno$getParticleCache() {
        return this.kreno$particleCache;
    }

    @Override
    public ServerCullingManager.EntityCullCache kreno$getEntityCache() {
        return this.kreno$entityCache;
    }

    @Override
    public long kreno$getLastLookTick() {
        return this.kreno$lastLookTick;
    }

    @Override
    public void kreno$setLook(double x, double y, double z, long tickCount) {
        this.kreno$lookX = x;
        this.kreno$lookY = y;
        this.kreno$lookZ = z;
        this.kreno$lastLookTick = tickCount;
    }

    @Override
    public double kreno$getLookX() {
        return this.kreno$lookX;
    }

    @Override
    public double kreno$getLookY() {
        return this.kreno$lookY;
    }

    @Override
    public double kreno$getLookZ() {
        return this.kreno$lookZ;
    }
}
