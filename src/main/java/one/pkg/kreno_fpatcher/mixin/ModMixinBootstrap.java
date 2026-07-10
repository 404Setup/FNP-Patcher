package one.pkg.kreno_fpatcher.mixin;

import net.fabricmc.loader.api.FabricLoader;
import one.pkg.kreno_fpatcher.ModConfig;
import one.pkg.libsl.api.loader.JavaLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ModMixinBootstrap implements IMixinConfigPlugin {
    private final Logger logger = LoggerFactory.getLogger("ModMixinBootstrap");

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        CONFIG config = CONFIG.find(mixinClassName);
        if (config != null) {
            var b = config.isEnabled();
            logger.info("Mixin {} {}", mixinClassName, b ? "enabled" : "disabled");
            return b;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    enum CONFIG {
        BestVarLong("one.pkg.kreno_fpatcher.mixin.network.microopt.VarLongMixin", ModConfig.Mixin::isBestVarLong),
        TextFilter_VT("one.pkg.kreno_fpatcher.mixin.network.thread.ServerTextFilterMixin", ModConfig.Mixin::isTextFilterVT),
        Util_VT("one.pkg.kreno_fpatcher.mixin.network.thread.UtilMixin", ModConfig.Mixin::isUtilVT),
        RconClient("one.pkg.kreno_fpatcher.mixin.network.experimental", ModConfig.Mixin::isRconClient),
        ServerEntitySendChanges("one.pkg.kreno_fpatcher.mixin.network.microopt.ServerEntitySendChanges", ModConfig.Mixin::isServerEntityMoveOpt),
        ParticlePacketOpt("one.pkg.kreno_fpatcher.mixin.network.microopt.particle.EntityMixin", () -> ModConfig.Mixin.isParticlePacketOpt() && !JavaLoader.INSTANCE.isClient()),
        ParticlePacketOpt2("one.pkg.kreno_fpatcher.mixin.network.microopt.particle.LivingEntityMixin", () -> ModConfig.Mixin.isParticlePacketOpt() && !JavaLoader.INSTANCE.isClient()),
        ParticlePacketOpt3("one.pkg.kreno_fpatcher.mixin.network.microopt.particle.ServerEntityMixin", () -> ModConfig.Mixin.isParticlePacketOpt() && !JavaLoader.INSTANCE.isClient()),
        ClientEncrypt("one.pkg.kreno_fpatcher.mixin.network.pipeline.encryption.ClientLoginMixin", ModConfig.Mixin::isClientEncrypt, "krypton"),
        KryptonFix128("one.pkg.kreno_fpatcher.mixin.network.fix.Varint21FrameDecoderMixin", ModConfig.Fix.Issues128::isEnabled, "krypton");
        public final String CLASS;
        public final Supplier<Boolean> configTarget;
        public final boolean hasMod;

        CONFIG(String clazz, @NotNull Supplier<Boolean> configTarget) {
            this(clazz, configTarget, null);
        }

        CONFIG(String clazz, @NotNull Supplier<Boolean> configTarget, String modid) {
            this.CLASS = clazz;
            this.configTarget = configTarget;
            this.hasMod = modid != null ? FabricLoader.getInstance().isModLoaded(modid) : true;
        }

        @Nullable
        public static CONFIG find(String clazz) {
            for (CONFIG config : values()) {
                if (config.CLASS.equals(clazz)) {
                    return config;
                }
            }
            return null;
        }

        public boolean isEnabled() {
            return this.hasMod && this.configTarget.get();
        }
    }
}
