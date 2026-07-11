package one.pkg.kreno_fpatcher.mixin.network.experimental;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.server.rcon.thread.RconClient;
import org.spongepowered.asm.mixin.*;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Mixin(RconClient.class)
public class RconClientMixin {
    @Unique
    private static final int CHUNK_SIZE = 4096;
    @Unique
    private static final int PACKET_OVERHEAD = 10;

    @Shadow
    @Final
    private Socket client;

    @Shadow
    private void closeSocket() {
    }

    /**
     * @author 404
     * @reason Optimize send method to accept byte[] directly, reducing string conversions and allocations.
     */
    @Overwrite
    private void send(int id, int type, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        this.send(id, type, bytes, 0, bytes.length);
    }

    @Unique
    private void send(int id, int type, byte[] messageBytes, int offset, int length) throws IOException {
        int packetLength = length + PACKET_OVERHEAD;
        byte[] buf = new byte[packetLength + 4];

        buf[0] = (byte) (packetLength & 0xFF);
        buf[1] = (byte) ((packetLength >>> 8) & 0xFF);
        buf[2] = (byte) ((packetLength >>> 16) & 0xFF);
        buf[3] = (byte) ((packetLength >>> 24) & 0xFF);

        buf[4] = (byte) (id & 0xFF);
        buf[5] = (byte) ((id >>> 8) & 0xFF);
        buf[6] = (byte) ((id >>> 16) & 0xFF);
        buf[7] = (byte) ((id >>> 24) & 0xFF);

        buf[8] = (byte) (type & 0xFF);
        buf[9] = (byte) ((type >>> 8) & 0xFF);
        buf[10] = (byte) ((type >>> 16) & 0xFF);
        buf[11] = (byte) ((type >>> 24) & 0xFF);

        System.arraycopy(messageBytes, offset, buf, 12, length);

        this.client.getOutputStream().write(buf);
    }

    /**
     * @author 404
     * @reason Optimize sendCmdResponse to split on byte boundaries instead of characters, reducing allocations and fixing potential encoding issues.
     */
    @Overwrite
    private void sendCmdResponse(int id, String message) throws IOException {
        byte[] fullBytes = message.getBytes(StandardCharsets.UTF_8);
        int len = fullBytes.length;

        if (len <= CHUNK_SIZE) {
            this.send(id, 0, fullBytes, 0, len);
        } else {
            int offset = 0;
            while (offset < len) {
                int chunkSize = Math.min(CHUNK_SIZE, len - offset);
                this.send(id, 0, fullBytes, offset, chunkSize);
                offset += chunkSize;
            }
        }
    }
}