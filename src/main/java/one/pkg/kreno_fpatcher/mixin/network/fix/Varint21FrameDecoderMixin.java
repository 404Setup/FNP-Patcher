package one.pkg.kreno_fpatcher.mixin.network.fix;

import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.VarInt;
import net.minecraft.network.Varint21FrameDecoder;
import one.pkg.kreno_fpatcher.ModConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(value = Varint21FrameDecoder.class, priority = 1500)
public class Varint21FrameDecoderMixin {
    @Unique
    private static final ExecutorService kreno_fpatcher$executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    @Shadow
    @Final
    private BandwidthDebugMonitor monitor;

    @Inject(method = "decode", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void kreno_fpatcher$decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out,
                                    CallbackInfo ci, @Local(name = "length") int length) {
        if (this.monitor != null) kreno_fpatcher$execute(length);
    }

    @Unique
    private void kreno_fpatcher$execute(int l) {
        if (ModConfig.Fix.Issues128.isSync()) this.monitor.onReceive(l + VarInt.getByteSize(l));
        else kreno_fpatcher$executor.execute(() -> this.monitor.onReceive(l + VarInt.getByteSize(l)));
    }
}
