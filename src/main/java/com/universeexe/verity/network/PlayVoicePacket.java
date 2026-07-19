package com.universeexe.verity.network;

import com.universeexe.verity.client.subtitle.VeritySubtitles;
import com.universeexe.verity.client.subtitle.VeritySubtitleHud;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client authoritative voice playback. Sound id must be registered in {@link VeritySounds}.
 */
public record PlayVoicePacket(
        int sessionId,
        String soundId,
        double x,
        double y,
        double z,
        float volume,
        float pitch,
        String subtitleKey,
        int entityId,
        int durationTicks,
        boolean actionBarMessage,
        int soundSourceOrdinal
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeUtf(soundId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeUtf(subtitleKey == null ? "" : subtitleKey);
        buf.writeVarInt(entityId);
        buf.writeVarInt(durationTicks);
        buf.writeBoolean(actionBarMessage);
        buf.writeVarInt(soundSourceOrdinal);
    }

    public static PlayVoicePacket decode(FriendlyByteBuf buf) {
        return new PlayVoicePacket(
                buf.readVarInt(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt()
        );
    }

    public static void handle(PlayVoicePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet)));
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(PlayVoicePacket packet) {
        if (VeritySounds.byId(packet.soundId) == null) {
            return;
        }
        SoundEvent sound = VeritySounds.byId(packet.soundId).get();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        SoundSource source = SoundSource.values()[Math.floorMod(packet.soundSourceOrdinal, SoundSource.values().length)];
        mc.level.playLocalSound(packet.x, packet.y, packet.z, sound, source, packet.volume, packet.pitch, false);
        if (packet.subtitleKey != null && !packet.subtitleKey.isBlank()) {
            Component text = VeritySubtitles.fromKey(packet.subtitleKey);
            if (packet.actionBarMessage) {
                mc.gui.setOverlayMessage(text, false);
            } else {
                mc.gui.getChat().addMessage(text);
            }
            if (VeritySubtitles.isVerityDialogueKey(packet.subtitleKey)) {
                VeritySubtitleHud.show(text, packet.durationTicks);
            }
        }
        VerityVoiceClientPlayback.onStarted(packet.sessionId, packet.durationTicks, packet.entityId);
    }
}
