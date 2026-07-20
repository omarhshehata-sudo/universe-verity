package com.universeexe.verity.network;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.client.subtitle.VeritySubtitleHud;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side tracking for voice session completion callbacks to the server.
 * Ensures only one Verity voice line plays at a time.
 */
@OnlyIn(Dist.CLIENT)
public final class VerityVoiceClientPlayback {
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger(1);
    private static final Map<Integer, PendingSession> PENDING = new ConcurrentHashMap<>();
    private static final Set<Integer> FINISHED_SENT = ConcurrentHashMap.newKeySet();

    @Nullable
    private static ResourceLocation activeSoundLocation;
    private static SoundSource activeSoundSource = SoundSource.NEUTRAL;
    private static int activeSessionId = -1;

    private VerityVoiceClientPlayback() {
    }

    public static boolean isDirectorPlaybackActive() {
        return activeSessionId >= 0 || !PENDING.isEmpty();
    }

    public static int nextSessionId() {
        return SESSION_COUNTER.getAndIncrement();
    }

    public static void playVoiceLine(
            int sessionId,
            String soundId,
            double x,
            double y,
            double z,
            float volume,
            float pitch,
            int soundSourceOrdinal,
            int durationTicks
    ) {
        stopActivePlayback(false);

        activeSessionId = sessionId;
        int paddedDuration = Math.max(durationTicks + 15, 30);
        PENDING.put(sessionId, new PendingSession(sessionId, paddedDuration));

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            UniverseVerity.LOGGER.warn("[VerityVoice] Client playback skipped — no level (session={} sound={})", sessionId, soundId);
            return;
        }

        String resolvedSoundId = VeritySounds.normalizeSoundId(soundId);
        RegistryObject<net.minecraft.sounds.SoundEvent> registered = VeritySounds.byId(resolvedSoundId);
        if (registered != null && registered.isPresent()) {
            SoundSource source = SoundSource.values()[Math.floorMod(soundSourceOrdinal, SoundSource.values().length)];
            activeSoundLocation = registered.get().getLocation();
            activeSoundSource = source;
            mc.level.playLocalSound(
                    x, y, z,
                    registered.get(), source, volume, pitch, false);
            UniverseVerity.LOGGER.info(
                    "[VerityVoice] Client session={} playing {} ({} ticks)",
                    sessionId,
                    resolvedSoundId,
                    durationTicks);
        } else {
            UniverseVerity.LOGGER.error(
                    "[VerityVoice] Unregistered sound id '{}' — audio skipped (session={})",
                    resolvedSoundId,
                    sessionId);
        }

        VerityNetwork.CHANNEL.sendToServer(new PlaybackStatusPacket(sessionId, PlaybackStatusPacket.Status.STARTED));
    }

    public static void onInterrupted(int sessionId) {
        if (sessionId == activeSessionId || sessionId <= 0) {
            stopActivePlayback(true);
        } else {
            PendingSession session = PENDING.remove(sessionId);
            if (session != null) {
                VerityNetwork.CHANNEL.sendToServer(new PlaybackStatusPacket(sessionId, PlaybackStatusPacket.Status.INTERRUPTED));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTick() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Iterator<Map.Entry<Integer, PendingSession>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PendingSession> entry = it.next();
            PendingSession session = entry.getValue();
            session.talkTicks--;
            if (session.talkTicks <= 0) {
                if (FINISHED_SENT.add(session.sessionId)) {
                    UniverseVerity.LOGGER.debug("[VerityVoice] Client session={} finished", session.sessionId);
                    VerityNetwork.CHANNEL.sendToServer(
                            new PlaybackStatusPacket(session.sessionId, PlaybackStatusPacket.Status.FINISHED));
                }
                if (session.sessionId == activeSessionId) {
                    activeSessionId = -1;
                    activeSoundLocation = null;
                }
                it.remove();
            }
        }
    }

    private static void stopActivePlayback(boolean notifyServer) {
        Minecraft mc = Minecraft.getInstance();
        if (activeSoundLocation != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stop(activeSoundLocation, activeSoundSource);
        }
        activeSoundLocation = null;
        if (notifyServer && activeSessionId > 0) {
            VerityNetwork.CHANNEL.sendToServer(
                    new PlaybackStatusPacket(activeSessionId, PlaybackStatusPacket.Status.INTERRUPTED));
        }
        if (activeSessionId > 0) {
            PENDING.remove(activeSessionId);
        }
        activeSessionId = -1;
        VeritySubtitleHud.clear();
    }

    private static final class PendingSession {
        private final int sessionId;
        private int talkTicks;

        private PendingSession(int sessionId, int durationTicks) {
            this.sessionId = sessionId;
            this.talkTicks = durationTicks;
        }
    }
}
