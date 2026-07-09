package one.pkg.kreno_fpatcher;

import net.fabricmc.loader.api.FabricLoader;
import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.metadata.ConfigMeta;

@ConfigEntry("kreno_fpatcher")
public class ModConfig {
    public static final SewliaConfig config;
    @ConfigTarget(group = "fix.issues128", value = "enabled", comment = "Fix Traffic Statistics")
    private static boolean var1 = false;
    @ConfigTarget(group = "fix.issues128", value = "sync", comment = "Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.")
    private static boolean var2 = true;
    @ConfigTarget(group = "mixin", value = "textFilterVT", comment = "Replace text filter thread with virtual thread")
    private static boolean var4 = true;
    @ConfigTarget(group = "mixin", value = "utilVT", comment = "Replace download thread with virtual thread")
    private static boolean var5 = true;
    @ConfigTarget(group = "mixin", value = "bestVarLong", comment = "Optimized VarLong implementation")
    private static boolean var6 = true;
    @ConfigTarget(group = "mixin", value = "clientEncrypt", comment = "Enable new encryption optimizations on the client side")
    private static boolean var7 = true;

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
                return var1;
            }

            public static boolean isSync() {
                return var2;
            }
        }
    }

    public static class Mixin {
        public static boolean isTextFilterVT() {
            return var4;
        }

        public static boolean isUtilVT() {
            return var5;
        }

        public static boolean isBestVarLong() {
            return var6;
        }

        public static boolean isClientEncrypt() {
            return var7;
        }
    }
}

