package one.pkg.kreno_fpatcher;

import net.fabricmc.loader.api.FabricLoader;
import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.metadata.ConfigMeta;
import one.pkg.libsl.api.loader.JavaLoader;

@ConfigEntry("kreno_fpatcher")
public class ModConfig {
    public static final SewliaConfig config;
    @ConfigTarget(group = "fix.issues128", value = "enabled", comment = "Fix Traffic Statistics")
    private static boolean fixIssues128Enabled = false;
    @ConfigTarget(group = "fix.issues128", value = "sync", comment = "Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.")
    private static boolean fixIssues128Sync = true;
    @ConfigTarget(group = "mixin", value = "textFilterVT", comment = "Replace text filter thread with virtual thread")
    private static boolean textFilterVT = true;
    @ConfigTarget(group = "mixin", value = "utilVT", comment = "Replace download thread with virtual thread")
    private static boolean utilVT = true;
    @ConfigTarget(group = "mixin", value = "bestVarLong", comment = "Optimized VarLong implementation")
    private static boolean bestVarLong = true;
    @ConfigTarget(group = "mixin", value = "clientEncrypt", comment = "Enable new encryption optimizations on the client side")
    private static boolean clientEncrypt = true;
    @ConfigTarget(group = "mixin", value = "rconClient", comment = "Optimized RconClient implementation")
    private static boolean rconClient = false;
    @ConfigTarget(group = "mixin", value = "serverEntityMoveOpt", comment = "Skips sending movement packets if the entity hasn't moved, and downgrades position+rotation packets to just rotation if the entity only turned")
    private static boolean serverEntityMoveOpt = false;
    @ConfigTarget(group = "mixin", value = "packetProcessorOpt", comment = "Halves concurrent queue operations when draining queued packets on the main thread")
    private static boolean packetProcessorOpt = true;
    @ConfigTarget(group = "mixin", value = "particlePacketOpt", comment = "Reduces some potentially useless particle packets. This configuration only takes effect on the server side.")
    private static boolean particlePacketOpt = true;
    @ConfigTarget(group = "mixin", value = "trackedEntityOpt", comment = "Optimizes entity packet broadcasting and integrates with server-side entity culling")
    private static boolean trackedEntityOpt = true;
    @ConfigTarget(group = "gui", value = "oreui", comment = "Replace Minecraft style KReno UI with a newly designed OreUI")
    private static boolean guiUseOreUITheme = false;
    @ConfigTarget(group = "culling", value = "particle", comment = "Smart particle culling on server side")
    private static boolean cullingParticle = true;
    @ConfigTarget(group = "culling", value = "entity", comment = "Smart entity culling on server side")
    private static boolean cullingEntity = true;
    @ConfigTarget(group = "culling", value = "asyncMode", comment = "Asynchronous execution mode for Cuttings system")
    private static boolean cullingAsyncMode = true;

    static {
        config = new SewliaConfig(ConfigMeta.of(
                ModConfig.class,
                FabricLoader.getInstance().getConfigDir().resolve("kreno_fpatcher.yaml"))
        );
    }

    private ModConfig() {
    }

    public static class Fix {
        public static class Issues128 {
            public static boolean isEnabled() {
                return fixIssues128Enabled;
            }

            public static boolean isSync() {
                return fixIssues128Sync;
            }
        }
    }

    public static class GUI {
        public static boolean isOreUI() {
            return guiUseOreUITheme;
        }
    }

    public static class Mixin {
        public static boolean isTextFilterVT() {
            return textFilterVT;
        }

        public static boolean isUtilVT() {
            return utilVT;
        }

        public static boolean isBestVarLong() {
            return bestVarLong;
        }

        public static boolean isClientEncrypt() {
            return clientEncrypt;
        }

        public static boolean isRconClient() {
            return rconClient;
        }

        public static boolean isServerEntityMoveOpt() {
            return serverEntityMoveOpt;
        }

        public static boolean isPacketProcessorOpt() {
            return packetProcessorOpt;
        }

        public static boolean isParticlePacketOpt() {
            return particlePacketOpt;
        }

        public static boolean isTrackedEntityOpt() {
            return trackedEntityOpt;
        }
    }

    public static class Culling {
        public static boolean isParticleEnabled() {
            return !JavaLoader.INSTANCE.isClient() && cullingParticle;
        }

        public static boolean isEntityEnabled() {
            return !JavaLoader.INSTANCE.isClient() && cullingEntity;
        }

        public static boolean isAsyncMode() {
            return cullingAsyncMode;
        }
    }
}

