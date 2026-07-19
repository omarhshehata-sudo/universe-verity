package com.universeexe.verity.network;

import com.universeexe.verity.UniverseVerity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client voice director debug overlay sync (off by default).
 */
public record VoiceDebugSyncPacket(
        boolean enabled,
        String currentLine,
        String category,
        int priority,
        int queueSize,
        int trust,
        String mood,
        String recentLine,
        String contextSummary
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeUtf(currentLine == null ? "" : currentLine);
        buf.writeUtf(category == null ? "" : category);
        buf.writeVarInt(priority);
        buf.writeVarInt(queueSize);
        buf.writeVarInt(trust);
        buf.writeUtf(mood == null ? "" : mood);
        buf.writeUtf(recentLine == null ? "" : recentLine);
        buf.writeUtf(contextSummary == null ? "" : contextSummary);
    }

    public static VoiceDebugSyncPacket decode(FriendlyByteBuf buf) {
        return new VoiceDebugSyncPacket(
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf()
        );
    }

    public static void handle(VoiceDebugSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                com.universeexe.verity.client.voice.VoiceDirectorDebugOverlay.apply(packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
