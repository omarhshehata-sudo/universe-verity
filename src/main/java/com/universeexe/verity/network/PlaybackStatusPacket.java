package com.universeexe.verity.network;

import com.universeexe.verity.voice.VerityVoiceDirector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server playback lifecycle acknowledgement. */
public record PlaybackStatusPacket(int sessionId, Status status) {
    public enum Status {
        STARTED,
        FINISHED,
        INTERRUPTED
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeEnum(status);
    }

    public static PlaybackStatusPacket decode(FriendlyByteBuf buf) {
        return new PlaybackStatusPacket(buf.readVarInt(), buf.readEnum(Status.class));
    }

    public static void handle(PlaybackStatusPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> VerityVoiceDirector.onPlaybackStatus(player, packet.sessionId, packet.status));
        context.setPacketHandled(true);
    }
}
