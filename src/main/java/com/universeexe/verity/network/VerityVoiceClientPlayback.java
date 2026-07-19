package com.universeexe.verity.network;

import com.universeexe.verity.entity.VerityEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tracking for voice session completion callbacks to the server.
 */
@OnlyIn(Dist.CLIENT)
public final class VerityVoiceClientPlayback {
    private static final Map<Integer, PendingSession> PENDING = new ConcurrentHashMap<>();

    private VerityVoiceClientPlayback() {
    }

    public static void onStarted(int sessionId, int durationTicks, int entityId) {
        PENDING.put(sessionId, new PendingSession(sessionId, Math.max(1, durationTicks), entityId));
        VerityNetwork.CHANNEL.sendToServer(new PlaybackStatusPacket(sessionId, PlaybackStatusPacket.Status.STARTED));
    }

    public static void onInterrupted(int sessionId) {
        PendingSession session = PENDING.remove(sessionId);
        if (session != null) {
            VerityNetwork.CHANNEL.sendToServer(new PlaybackStatusPacket(sessionId, PlaybackStatusPacket.Status.INTERRUPTED));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Iterator<Map.Entry<Integer, PendingSession>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PendingSession> entry = it.next();
            PendingSession session = entry.getValue();
            if (session.entityId >= 0) {
                Entity entity = mc.level.getEntity(session.entityId);
                if (entity instanceof VerityEntity verity && session.talkTicks <= session.durationTicks) {
                    // Server drives talking state; client only tracks timing.
                }
            }
            session.talkTicks--;
            if (session.talkTicks <= 0) {
                VerityNetwork.CHANNEL.sendToServer(
                        new PlaybackStatusPacket(session.sessionId, PlaybackStatusPacket.Status.FINISHED));
                it.remove();
            }
        }
    }

    private static final class PendingSession {
        private final int sessionId;
        private final int durationTicks;
        private final int entityId;
        private int talkTicks;

        private PendingSession(int sessionId, int durationTicks, int entityId) {
            this.sessionId = sessionId;
            this.durationTicks = durationTicks;
            this.entityId = entityId;
            this.talkTicks = durationTicks;
        }
    }
}
