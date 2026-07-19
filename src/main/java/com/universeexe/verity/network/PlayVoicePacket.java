package com.universeexe.verity.network;

import com.universeexe.verity.UniverseVerity;
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
import net.minecraftforge.registries.RegistryObject;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            UniverseVerity.LOGGER.warn("[VerityVoice] PlayVoicePacket dropped — no level/player (sound={})", packet.soundId);
            return;
        }

        String resolvedSoundId = VeritySounds.normalizeSoundId(packet.soundId);
        RegistryObject<SoundEvent> registered = VeritySounds.byId(resolvedSoundId);
        if (registered != null && registered.isPresent()) {
            SoundSource source = SoundSource.values()[Math.floorMod(packet.soundSourceOrdinal, SoundSource.values().length)];
            mc.level.playLocalSound(
                    packet.x, packet.y, packet.z,
                    registered.get(), source, packet.volume, packet.pitch, false);
        } else {
            UniverseVerity.LOGGER.error("[VerityVoice] Unregistered sound id '{}' — audio skipped", resolvedSoundId);
        }

        String subtitleKey = packet.subtitleKey();
        if (subtitleKey == null || subtitleKey.isBlank()) {
            subtitleKey = VeritySounds.subtitleKeyFor(resolvedSoundId);
        }
        if (subtitleKey != null && !subtitleKey.isBlank() && VeritySubtitles.isVerityDialogueKey(subtitleKey)) {
            Component text = VeritySubtitles.fromKey(subtitleKey);
            int duration = packet.durationTicks() > 0 ? packet.durationTicks() : 80;
            VeritySubtitleHud.show(text, duration);
        } else if (subtitleKey != null && !subtitleKey.isBlank()) {
            UniverseVerity.LOGGER.warn("[VeritySubtitles] Non-dialogue key skipped: {}", subtitleKey);
        } else {
            UniverseVerity.LOGGER.warn("[VeritySubtitles] No subtitle key for sound {}", resolvedSoundId);
        }

        VerityVoiceClientPlayback.onStarted(packet.sessionId, packet.durationTicks, packet.entityId);
    }
}
