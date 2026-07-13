package one.pkg.kreno_fpatcher.mixin.network.microopt;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import one.pkg.kreno_fpatcher.ModConfig;
import one.pkg.kreno_fpatcher.mixin.accessor.ClientboundMoveEntityPacketAccessor;
import one.pkg.kreno_fpatcher.util.culling.IKrenoTrackedEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Mixin(ServerEntity.class)
public class ServerEntitySendChanges {
    @Shadow
    private boolean wasOnGround;

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private ServerEntity.Synchronizer synchronizer;

    @Unique
    private boolean kreno$shouldSkipPacket() {
        return (ModConfig.Mixin.isTrackedEntityOpt() || ModConfig.Culling.isEntityEnabled())
                && !((IKrenoTrackedEntity) this.synchronizer).kreno$hasTrackingPlayers();
    }

    /**
     * Skips allocating ClientboundSetPassengersPacket when there are no tracking players.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;"
            )
    )
    private ClientboundSetPassengersPacket kreno$cancelUselessPassengersPacket(
            Entity vehicle,
            Operation<ClientboundSetPassengersPacket> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        return original.call(vehicle);
    }

    /**
     * Rotation-only updates are also skipped if they don't actually change the state.
     * <p>
     * If the rotation and on-ground status haven't changed, we return null.
     * This prevents the server from sending redundant 'Rot' packets when an entity
     * is effectively stationary and its orientation is unchanged.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(IBBZ)Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket$Rot;"
            )
    )
    private ClientboundMoveEntityPacket.Rot kreno$cancelUselessRotPacket(
            int id, byte yRot, byte xRot,
            boolean onGround,
            Operation<ClientboundMoveEntityPacket.Rot> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        if (!ModConfig.Mixin.isServerEntityMoveOpt()) {
            return original.call(id, yRot, xRot, onGround);
        }
        if (this.wasOnGround == this.entity.onGround() && yRot == 0 && xRot == 0) {
            return null;
        }
        return original.call(id, yRot, xRot, onGround);
    }

    /**
     * There's no point in sending a movement update if the entity hasn't actually moved.
     * <p>
     * If the displacement (xa, ya, za) is zero, we abort the packet creation by returning null.
     * This saves both CPU time on the server and bandwidth on the network by skipping useless zero-delta updates.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(ISSSZ)Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket$Pos;"
            )
    )
    private ClientboundMoveEntityPacket.Pos kreno$cancelUselessPosPacket(
            int id,
            short xa,
            short ya,
            short za,
            boolean onGround,
            Operation<ClientboundMoveEntityPacket.Pos> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        if (!ModConfig.Mixin.isServerEntityMoveOpt()) {
            return original.call(id, xa, ya, za, onGround);
        }
        if (xa == 0 && ya == 0 && za == 0) {
            return null;
        }
        return original.call(id, xa, ya, za, onGround);
    }

    /**
     * Skips allocating PosRot packet when there are no tracking players.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(ISSSBBZ)Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket$PosRot;"
            )
    )
    private ClientboundMoveEntityPacket.PosRot kreno$cancelUselessPosRotPacket(
            int id, short xa, short ya, short za, byte yRot, byte xRot, boolean onGround,
            Operation<ClientboundMoveEntityPacket.PosRot> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        return original.call(id, xa, ya, za, yRot, xRot, onGround);
    }

    /**
     * Skips allocating ClientboundEntityPositionSyncPacket when there are no tracking players.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundEntityPositionSyncPacket;of(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/protocol/game/ClientboundEntityPositionSyncPacket;"
            )
    )
    private ClientboundEntityPositionSyncPacket kreno$cancelUselessPositionSyncPacket(
            Entity entity,
            Operation<ClientboundEntityPositionSyncPacket> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        return original.call(entity);
    }

    /**
     * Skips allocating SetEntityMotionPacket when there are no tracking players.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket;"
            )
    )
    private ClientboundSetEntityMotionPacket kreno$cancelUselessMotionPacket(
            Entity entity,
            Operation<ClientboundSetEntityMotionPacket> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        return original.call(entity);
    }

    /**
     * Skips allocating SetEntityMotionPacket (with Vec3) when there are no tracking players.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(ILnet/minecraft/world/phys/Vec3;)Lnet/minecraft/network/protocol/game/ClientboundSetEntityMotionPacket;"
            )
    )
    private ClientboundSetEntityMotionPacket kreno$cancelUselessMotionPacket2(
            int id, Vec3 movement,
            Operation<ClientboundSetEntityMotionPacket> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        return original.call(id, movement);
    }

    /**
     * Skips allocating ClientboundProjectilePowerPacket when there are no tracking players.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(ID)Lnet/minecraft/network/protocol/game/ClientboundProjectilePowerPacket;"
            )
    )
    private ClientboundProjectilePowerPacket kreno$cancelUselessProjectilePowerPacket(
            int id, double accelerationPower,
            Operation<ClientboundProjectilePowerPacket> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        return original.call(id, accelerationPower);
    }

    /**
     * Skips allocating RotateHeadPacket when there are no tracking players.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/entity/Entity;B)Lnet/minecraft/network/protocol/game/ClientboundRotateHeadPacket;"
            )
    )
    private ClientboundRotateHeadPacket kreno$cancelUselessRotateHeadPacket(
            Entity entity, byte yHeadRot,
            Operation<ClientboundRotateHeadPacket> original
    ) {
        if (kreno$shouldSkipPacket()) {
            return null;
        }
        return original.call(entity, yHeadRot);
    }

    /**
     * Redirects List.of to filter out nulls safely, avoiding NullPointerException.
     */
    @Redirect(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"
            )
    )
    private List<?> kreno$cleanListOf(Object e1, Object e2) {
        if (e1 == null && e2 == null) {
            return Collections.emptyList();
        }
        if (e1 == null) {
            return Collections.singletonList(e2);
        }
        if (e2 == null) {
            return Collections.singletonList(e1);
        }
        return List.of(e1, e2);
    }

    /**
     * Skips allocating ClientboundBundlePacket when empty or when skipping packets.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/Iterable;)Lnet/minecraft/network/protocol/game/ClientboundBundlePacket;"
            )
    )
    private ClientboundBundlePacket kreno$cancelBundlePacket(
            Iterable<?> packets,
            Operation<ClientboundBundlePacket> original
    ) {
        if (kreno$shouldSkipPacket() || (packets instanceof List<?> list && list.isEmpty()) ||
                !packets.iterator().hasNext()) {
            return null;
        }
        return original.call(packets);
    }

    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void kreno$sendChangesPacketGuard(ServerEntity.Synchronizer synchronizer, Packet<?> packet,
                                              Operation<Void> original) {
        if (packet == null) return;
        if (!ModConfig.Mixin.isServerEntityMoveOpt()) {
            original.call(synchronizer, packet);
            return;
        }
        if (packet instanceof ClientboundMoveEntityPacket.PosRot posRot) {
            ClientboundMoveEntityPacketAccessor accessor = (ClientboundMoveEntityPacketAccessor) posRot;
            if (posRot.getXa() == 0 && posRot.getYa() == 0 && posRot.getZa() == 0) {
                if (this.wasOnGround == this.entity.onGround() && accessor.kreno$getYRot() == 0 && accessor.kreno$getXRot() == 0) {
                    return;
                }
                packet = new ClientboundMoveEntityPacket.Rot(accessor.getEntityId(), accessor.kreno$getYRot(),
                        accessor.kreno$getXRot(), posRot.isOnGround());
            }
        }
        original.call(synchronizer, packet);
    }

    /**
     * Guards sendToTrackingPlayersFiltered against null packets.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersFiltered(Lnet/minecraft/network/protocol/Packet;Ljava/util/function/Predicate;)V"
            )
    )
    private void kreno$sendChangesPacketGuardFiltered(ServerEntity.Synchronizer synchronizer, Packet<?> packet,
                                                      Predicate<?> predicate,
                                                      Operation<Void> original) {
        if (packet == null) return;
        original.call(synchronizer, packet, predicate);
    }

    /**
     * Guards sendToTrackingPlayersAndSelf against null packets.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersAndSelf(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void kreno$sendChangesPacketGuardAndSelf(ServerEntity.Synchronizer synchronizer, Packet<?> packet,
                                                     Operation<Void> original) {
        if (packet == null) return;
        original.call(synchronizer, packet);
    }

    /**
     * Prevents the server from sending redundant 0-velocity packets.
     * When both the current movement and the last sent movement are small enough
     * to be quantized as exactly 0 by LpVec3 (abs max < 3.051944088384301E-5),
     * we pretend the distance to the last movement is exactly 0.0.
     * This avoids waking up tracking clients with identical zero-velocity updates.
     */
    @Redirect(
            method = {"sendChanges", "handleMinecartPosRot"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
            )
    )
    private double kreno$optimizeRedundantMotion(Vec3 currentMovement, Vec3 vec) {
        double diff = currentMovement.distanceToSqr(vec);
        if (diff == 0.0) {
            return 0.0;
        }

        double maxCurr = Math.max(Math.abs(currentMovement.x), Math.max(Math.abs(currentMovement.y),
                Math.abs(currentMovement.z)));
        double maxLast = Math.max(Math.abs(vec.x), Math.max(Math.abs(vec.y), Math.abs(vec.z)));

        if (maxCurr < 3.051944088384301E-5 && maxLast < 3.051944088384301E-5) {
            return 0.0;
        }

        return diff;
    }

}
