package com.universeexe.verity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: stop an active voice session. */
public record InterruptVoicePacket(int sessionId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
    }

    public static InterruptVoicePacket decode(FriendlyByteBuf buf) {
        return new InterruptVoicePacket(buf.readVarInt());
    }

    public static void handle(InterruptVoicePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                VerityVoiceClientPlayback.onInterrupted(packet.sessionId)));
        ctx.get().setPacketHandled(true);
    }
}
