package com.universeexe.verity.voice;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.entity.VerityBoxEntity;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.network.InterruptVoicePacket;
import com.universeexe.verity.network.PlayVoicePacket;
import com.universeexe.verity.network.PlaybackStatusPacket;
import com.universeexe.verity.network.VerityNetwork;
import com.universeexe.verity.network.VoiceDebugSyncPacket;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.util.VerityDebug;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-authoritative voice director. All gameplay voice playback routes through here.
 */
public final class VerityVoiceDirector {
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger(1);
    private static final Map<UUID, PlayerVoiceState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DEBUG_OVERLAY = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> DEBUG_REJECTIONS = new ConcurrentHashMap<>();

    private VerityVoiceDirector() {
    }

    @Nullable
    public static List<String> debugRejections(UUID playerId) {
        return DEBUG_OVERLAY.getOrDefault(playerId, false) ? DEBUG_REJECTIONS.computeIfAbsent(playerId, id -> new ArrayList<>()) : null;
    }

    public static boolean requestPool(ServerPlayer player, String poolId, VerityVoiceContext context) {
        Optional<VerityVoicePool> poolOpt = VerityVoiceManifest.get().pool(poolId);
        if (poolOpt.isEmpty() || poolOpt.get().isEmpty()) {
            VerityDebug.log("[VerityVoice] Pool missing or empty: {}", poolId);
            return false;
        }
        VerityVoicePool pool = poolOpt.get();
        ServerLevel level = player.serverLevel();
        VerityVoiceMemory memory = VerityVoiceMemory.forPlayer(player);
        VerityVoiceSnapshot snapshot = context.snapshot() != null
                ? context.snapshot()
                : VerityVoiceSnapshot.capture(player, findVerityEntity(level, context.anchorEntityId()));
        DEBUG_REJECTIONS.put(player.getUUID(), new ArrayList<>());
        Optional<VerityVoiceVariant> pick = pool.pick(player, snapshot, memory, level.getRandom());
        if (pick.isEmpty()) {
            return false;
        }
        VerityVoiceVariant variant = pick.get();
        if (variant.silent()) {
            memory.remember(player, variant.id(), pool.category());
            syncDebug(player);
            return true;
        }
        memory.remember(player, variant.id(), pool.category());
        boolean ok = enqueue(player, VerityQueuedVoiceEvent.single(
                "pool:" + poolId,
                pool.category(),
                variant,
                context
        ));
        syncDebug(player);
        return ok;
    }

    public static boolean requestConversation(ServerPlayer player, String conversationId, VerityVoiceContext context) {
        return requestConversation(player, conversationId, context, null);
    }

    public static boolean requestConversation(
            ServerPlayer player,
            String conversationId,
            VerityVoiceContext context,
            @Nullable Runnable onEventComplete
    ) {
        Optional<VerityConversation> convOpt = VerityVoiceManifest.get().conversation(conversationId);
        if (convOpt.isEmpty() || convOpt.get().isEmpty()) {
            VerityDebug.log("[VerityVoice] Conversation missing or empty: {}", conversationId);
            return false;
        }
        VerityConversation conversation = convOpt.get();
        ServerLevel level = player.serverLevel();
        VerityVoiceMemory memory = VerityVoiceMemory.forPlayer(player);
        VerityVoiceSnapshot snapshot = context.snapshot() != null
                ? context.snapshot()
                : VerityVoiceSnapshot.capture(player, findVerityEntity(level, context.anchorEntityId()));
        var resolved = new ArrayList<VerityQueuedVoiceEvent.ResolvedStep>();
        for (VerityConversationStep step : conversation.steps()) {
            Optional<VerityVoiceVariant> variant = resolveStep(player, level, step, snapshot, memory);
            if (variant.isEmpty()) {
                continue;
            }
            resolved.add(new VerityQueuedVoiceEvent.ResolvedStep(variant.get(), step.pauseAfterTicks()));
        }
        if (resolved.isEmpty()) {
            return false;
        }
        memory.rememberConversation(player, conversationId);
        boolean ok = enqueue(player, VerityQueuedVoiceEvent.fromConversation(
                "conversation:" + conversationId,
                conversation,
                resolved,
                context,
                onEventComplete
        ));
        syncDebug(player);
        return ok;
    }

    public static boolean requestDirect(
            ServerPlayer player,
            String soundId,
            VerityVoiceCategory category,
            int durationTicks,
            String subtitleKey,
            float volume,
            float pitch,
            VerityVoiceContext context
    ) {
        if (!isRegisteredSound(soundId)) {
            UniverseVerity.LOGGER.warn("[VerityVoice] Rejected unregistered sound {}", soundId);
            return false;
        }
        VerityVoiceVariant variant = VerityVoiceVariant.simple(
                soundId,
                soundId,
                durationTicks,
                subtitleKey == null ? "" : subtitleKey,
                volume,
                pitch
        );
        VerityVoiceMemory.forPlayer(player).remember(player, variant.id(), category);
        boolean ok = enqueue(player, VerityQueuedVoiceEvent.single("direct:" + soundId, category, variant, context));
        syncDebug(player);
        return ok;
    }

    public static boolean requestEvent(ServerPlayer player, VerityQueuedVoiceEvent event) {
        boolean ok = enqueue(player, event);
        syncDebug(player);
        return ok;
    }

    public static boolean playEvent(ServerPlayer player, String eventId, VerityVoiceContext context) {
        if (VerityVoiceManifest.get().pool(eventId).isPresent()) {
            return requestPool(player, eventId, context);
        }
        if (VerityVoiceManifest.get().conversation(eventId).isPresent()) {
            return requestConversation(player, eventId, context);
        }
        return false;
    }

    public static boolean isPlayerBusy(UUID playerId) {
        PlayerVoiceState state = STATES.get(playerId);
        return state != null && state.isBusy();
    }

    public static boolean isAnchorBusy(ServerLevel level, int entityId) {
        if (entityId < 0) {
            return false;
        }
        Entity entity = level.getEntity(entityId);
        if (entity instanceof VerityBoxEntity box) {
            return box.isVoiceBusy();
        }
        if (entity instanceof VerityEntity verity) {
            return verity.isTalking() || verity.isVoiceDirectorBusy();
        }
        return false;
    }

    public static int busyTicksRemaining(UUID playerId) {
        PlayerVoiceState state = STATES.get(playerId);
        return state == null ? 0 : state.busyTicksRemaining();
    }

    public static void interrupt(ServerPlayer player, @Nullable VerityVoiceCategory incomingCategory) {
        PlayerVoiceState state = STATES.get(player.getUUID());
        if (state == null || state.currentEvent == null) {
            return;
        }
        if (incomingCategory != null
                && !VerityVoicePriority.canInterrupt(state.currentEvent.category(), incomingCategory)) {
            return;
        }
        stopCurrent(player, state, true);
        syncDebug(player);
    }

    public static void clearQueue(ServerPlayer player, boolean interruptActive) {
        PlayerVoiceState state = STATES.computeIfAbsent(player.getUUID(), id -> new PlayerVoiceState());
        state.queue.clear();
        if (interruptActive) {
            stopCurrent(player, state, true);
        }
        syncDebug(player);
    }

    public static List<String> queueSummary(ServerPlayer player) {
        PlayerVoiceState state = STATES.get(player.getUUID());
        if (state == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (state.currentEvent != null) {
            lines.add("* " + state.currentEvent.requestId() + " (" + state.currentEvent.category() + ")");
        }
        for (VerityQueuedVoiceEvent event : state.queue) {
            lines.add("- " + event.requestId() + " (" + event.category() + ")");
        }
        return lines;
    }

    public static void setDebugOverlay(ServerPlayer player, boolean enabled) {
        DEBUG_OVERLAY.put(player.getUUID(), enabled);
        VerityDebug.setCommandOverride(enabled);
        if (!enabled) {
            VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new VoiceDebugSyncPacket(false, "", "", 0, 0, 0, "", "", ""));
        }
        syncDebug(player);
    }

    public static boolean isDebugOverlayEnabled(UUID playerId) {
        return DEBUG_OVERLAY.getOrDefault(playerId, false);
    }

    public static void onPlaybackStatus(ServerPlayer player, int sessionId, PlaybackStatusPacket.Status status) {
        PlayerVoiceState state = STATES.get(player.getUUID());
        if (state == null || state.currentEvent == null || state.currentEvent.sessionId() != sessionId) {
            return;
        }
        switch (status) {
            case STARTED -> VerityDebug.log("[VerityVoice] Started session={} player={}", sessionId, player.getUUID());
            case FINISHED -> finishStep(player, state);
            case INTERRUPTED -> stopCurrent(player, state, false);
            default -> {
            }
        }
        syncDebug(player);
    }

    public static void tickPlayer(ServerPlayer player) {
        PlayerVoiceState state = STATES.computeIfAbsent(player.getUUID(), id -> new PlayerVoiceState());
        if (state.currentEvent != null) {
            if (state.fallbackTicksRemaining > 0) {
                state.fallbackTicksRemaining--;
                if (state.fallbackTicksRemaining <= 0) {
                    finishStep(player, state);
                }
            } else if (state.currentEvent.pauseTicksRemaining() > 0) {
                state.currentEvent.tickPause();
                if (state.currentEvent.pauseTicksRemaining() <= 0) {
                    state.currentEvent.nextStepAfterPause().ifPresent(step -> startStep(player, state, step));
                }
            }
            return;
        }
        VerityQueuedVoiceEvent next = state.queue.poll();
        if (next != null) {
            startEvent(player, state, next);
        }
    }

    public static void clearPlayer(UUID playerId) {
        STATES.remove(playerId);
        DEBUG_OVERLAY.remove(playerId);
        DEBUG_REJECTIONS.remove(playerId);
        VerityVoiceMemory.clearPlayer(playerId);
        VerityVoiceSnapshot.invalidate(playerId);
    }

    public static String statusFor(ServerPlayer player) {
        PlayerVoiceState state = STATES.get(player.getUUID());
        if (state == null || state.currentEvent == null) {
            int queued = state == null ? 0 : state.queue.size();
            return "idle queued=" + queued;
        }
        return "playing request=" + state.currentEvent.requestId()
                + " category=" + state.currentEvent.category()
                + " session=" + state.currentEvent.sessionId()
                + " queued=" + state.queue.size();
    }

    public static String contextSummary(ServerPlayer player) {
        return VerityVoiceSnapshot.capture(player, findOwnedVerity(player)).debugSummary();
    }

    public static List<String> historySummary(ServerPlayer player) {
        VerityVoiceMemory memory = VerityVoiceMemory.forPlayer(player);
        List<String> lines = new ArrayList<>();
        lines.add("recent=" + memory.recentHistory());
        lines.add("idle=" + memory.recentIdleHistory());
        lines.add("conversations=" + memory.recentConversationHistory());
        return lines;
    }

    public static List<String> memoriesSummary(ServerPlayer player) {
        return VerityVoiceMemory.forPlayer(player).memoryFlagSummary(player);
    }

    public static List<String> cooldownsSummary(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        List<String> lines = new ArrayList<>();
        VerityVoiceMemory.forPlayer(player).cooldownSnapshot(player, now).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(20)
                .forEach(e -> lines.add(e.getKey() + " ticks_since=" + e.getValue()));
        lines.add("danger=" + com.universeexe.verity.data.VerityPlayerData.get(player)
                .getLong(VerityVoiceKeys.LAST_DANGER_VOICE));
        lines.add("idle=" + com.universeexe.verity.data.VerityPlayerData.get(player)
                .getLong(VerityVoiceKeys.LAST_IDLE_VOICE));
        return lines;
    }

    private static boolean enqueue(ServerPlayer player, VerityQueuedVoiceEvent event) {
        if (!event.hasSteps()) {
            return false;
        }
        PlayerVoiceState state = STATES.computeIfAbsent(player.getUUID(), id -> new PlayerVoiceState());
        if (state.currentEvent != null) {
            if (VerityVoicePriority.canInterrupt(state.currentEvent.category(), event.category())) {
                stopCurrent(player, state, true);
                state.queue.addFirst(event);
                tryStart(player, state);
                return true;
            }
            if (event.category() == VerityVoiceCategory.IDLE) {
                return false;
            }
            state.queue.addLast(event);
            return true;
        }
        state.queue.addLast(event);
        tryStart(player, state);
        return true;
    }

    private static void tryStart(ServerPlayer player, PlayerVoiceState state) {
        if (state.currentEvent != null) {
            return;
        }
        VerityQueuedVoiceEvent next = state.queue.poll();
        if (next != null) {
            startEvent(player, state, next);
        }
    }

    private static void startEvent(ServerPlayer player, PlayerVoiceState state, VerityQueuedVoiceEvent event) {
        state.currentEvent = event;
        event.currentStep().ifPresent(step -> startStep(player, state, step));
        syncDebug(player);
    }

    private static void startStep(ServerPlayer player, PlayerVoiceState state, VerityQueuedVoiceEvent.ResolvedStep step) {
        VerityQueuedVoiceEvent event = state.currentEvent;
        if (event == null) {
            return;
        }
        VerityVoiceVariant variant = step.variant();
        if (variant.silent()) {
            int sessionId = SESSION_COUNTER.getAndIncrement();
            event.setSessionId(sessionId);
            event.markStarted();
            state.lastFinishedSessionId = 0;
            state.fallbackTicksRemaining = variant.durationTicks();
            VerityDebug.log("[VerityVoice] Silent {} session={} player={}", variant.id(), sessionId, player.getUUID());
            syncDebug(player);
            return;
        }
        if (!isRegisteredSound(variant.soundId())) {
            finishStep(player, state);
            return;
        }

        int sessionId = SESSION_COUNTER.getAndIncrement();
        event.setSessionId(sessionId);
        event.markStarted();
        state.lastFinishedSessionId = 0;

        VerityVoiceContext ctx = event.context();
        applyAnchorBusy(player.serverLevel(), ctx, variant.durationTicks(), variant);

        if (ctx.onStart() != null) {
            ctx.onStart().run();
        }

        PlayVoicePacket packet = new PlayVoicePacket(
                sessionId,
                variant.soundId(),
                ctx.x(),
                ctx.y(),
                ctx.z(),
                variant.volume(),
                variant.pitch(),
                variant.subtitleKey(),
                ctx.anchorEntityId(),
                variant.durationTicks(),
                ctx.actionBarMessage(),
                ctx.soundSource().ordinal()
        );

        if (ctx.ownerOnly()) {
            VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        } else if (ctx.anchorEntityId() >= 0) {
            Entity anchor = player.serverLevel().getEntity(ctx.anchorEntityId());
            if (anchor != null) {
                VerityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> anchor), packet);
            } else {
                VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        } else {
            VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }

        state.fallbackTicksRemaining = variant.durationTicks() + 25;
        VerityDebug.log("[VerityVoice] Play {} session={} player={}", variant.soundId(), sessionId, player.getUUID());
        syncDebug(player);
    }

    private static void finishStep(ServerPlayer player, PlayerVoiceState state) {
        VerityQueuedVoiceEvent event = state.currentEvent;
        if (event == null) {
            return;
        }
        int sessionId = event.sessionId();
        if (sessionId > 0 && sessionId == state.lastFinishedSessionId) {
            VerityDebug.log("[VerityVoice] Ignored duplicate finish session={} player={}", sessionId, player.getUUID());
            return;
        }
        state.lastFinishedSessionId = sessionId;
        state.fallbackTicksRemaining = 0;
        VerityVoiceContext ctx = event.context();
        Optional<VerityQueuedVoiceEvent.ResolvedStep> next = event.advanceAfterPlayback();
        if (next.isPresent()) {
            if (event.pauseTicksRemaining() <= 0) {
                startStep(player, state, next.get());
            }
            state.fallbackTicksRemaining = 0;
            syncDebug(player);
            return;
        }
        if (!event.isComplete()) {
            state.fallbackTicksRemaining = 0;
            syncDebug(player);
            return;
        }
        clearAnchorBusy(player.serverLevel(), ctx);
        Runnable onComplete = event.onEventComplete();
        state.currentEvent = null;
        state.fallbackTicksRemaining = 0;
        tryStart(player, state);
        if (onComplete != null) {
            onComplete.run();
        }
        if (ctx.onComplete() != null) {
            ctx.onComplete().run();
        }
        syncDebug(player);
    }

    private static void stopCurrent(ServerPlayer player, PlayerVoiceState state, boolean notifyClient) {
        VerityQueuedVoiceEvent event = state.currentEvent;
        if (event == null) {
            return;
        }
        if (notifyClient && event.sessionId() > 0) {
            InterruptVoicePacket interrupt = new InterruptVoicePacket(event.sessionId());
            if (event.context().ownerOnly()) {
                VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), interrupt);
            } else if (event.context().anchorEntityId() >= 0) {
                Entity anchor = player.serverLevel().getEntity(event.context().anchorEntityId());
                if (anchor != null) {
                    VerityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> anchor), interrupt);
                }
            }
        }
        clearAnchorBusy(player.serverLevel(), event.context());
        state.currentEvent = null;
        state.fallbackTicksRemaining = 0;
    }

    private static void applyAnchorBusy(ServerLevel level, VerityVoiceContext ctx, int durationTicks, VerityVoiceVariant variant) {
        if (ctx.anchorEntityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(ctx.anchorEntityId());
        if (entity instanceof VerityBoxEntity box) {
            box.setVoiceBusyTicks(durationTicks);
            box.setLastVoiceLine(variant.id());
        } else if (entity instanceof VerityEntity verity) {
            verity.beginTalkingForTicks(durationTicks);
            verity.setVoiceDirectorBusy(true);
        }
    }

    private static void clearAnchorBusy(ServerLevel level, VerityVoiceContext ctx) {
        if (ctx.anchorEntityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(ctx.anchorEntityId());
        if (entity instanceof VerityBoxEntity box) {
            box.setVoiceBusyTicks(0);
        } else if (entity instanceof VerityEntity verity) {
            verity.setVoiceDirectorBusy(false);
        }
    }

    private static Optional<VerityVoiceVariant> resolveStep(
            ServerPlayer player,
            ServerLevel level,
            VerityConversationStep step,
            VerityVoiceSnapshot snapshot,
            VerityVoiceMemory memory
    ) {
        if (step.inlineVariant() != null) {
            return Optional.of(step.inlineVariant());
        }
        if (step.poolId() != null) {
            return VerityVoiceManifest.get().pool(step.poolId())
                    .flatMap(pool -> pool.pick(player, snapshot, memory, level.getRandom()));
        }
        return Optional.empty();
    }

    private static boolean isRegisteredSound(String soundId) {
        RegistryObject<net.minecraft.sounds.SoundEvent> ro = VeritySounds.byId(VeritySounds.normalizeSoundId(soundId));
        return ro != null && ro.isPresent();
    }

    @Nullable
    private static VerityEntity findVerityEntity(ServerLevel level, int entityId) {
        if (entityId < 0) {
            return null;
        }
        Entity entity = level.getEntity(entityId);
        return entity instanceof VerityEntity verity ? verity : null;
    }

    @Nullable
    private static VerityEntity findOwnedVerity(ServerPlayer player) {
        return com.universeexe.verity.trust.VerityTrustManager.findOwnedVerity(player).orElse(null);
    }

    private static void syncDebug(ServerPlayer player) {
        if (!DEBUG_OVERLAY.getOrDefault(player.getUUID(), false)) {
            return;
        }
        PlayerVoiceState state = STATES.get(player.getUUID());
        String current = state != null && state.currentEvent != null ? state.currentEvent.requestId() : "idle";
        String category = state != null && state.currentEvent != null ? state.currentEvent.category().name() : "-";
        int priority = state != null && state.currentEvent != null ? state.currentEvent.category().priority().level() : 0;
        int queued = state == null ? 0 : state.queue.size();
        VerityVoiceSnapshot snapshot = VerityVoiceSnapshot.capture(player, findOwnedVerity(player));
        List<String> recent = VerityVoiceMemory.forPlayer(player).recentHistory();
        String recentLine = recent.isEmpty() ? "-" : recent.get(recent.size() - 1);
        VoiceDebugSyncPacket packet = new VoiceDebugSyncPacket(
                true,
                current,
                category,
                priority,
                queued,
                snapshot.trust(),
                snapshot.mood().name(),
                recentLine,
                snapshot.debugSummary()
        );
        VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static final class PlayerVoiceState {
        private final Deque<VerityQueuedVoiceEvent> queue = new ArrayDeque<>();
        @Nullable
        private VerityQueuedVoiceEvent currentEvent;
        private int fallbackTicksRemaining;
        private int lastFinishedSessionId;

        private boolean isBusy() {
            return currentEvent != null || !queue.isEmpty();
        }

        private int busyTicksRemaining() {
            if (currentEvent == null) {
                return 0;
            }
            return Math.max(fallbackTicksRemaining, currentEvent.pauseTicksRemaining());
        }
    }
}
