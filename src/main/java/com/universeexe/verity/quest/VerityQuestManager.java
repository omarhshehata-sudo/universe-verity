package com.universeexe.verity.quest;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.network.PlayVoicePacket;
import com.universeexe.verity.network.VerityNetwork;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.trust.TrustReason;
import com.universeexe.verity.trust.VerityTrustEvents;
import com.universeexe.verity.trust.VerityTrustManager;
import com.universeexe.verity.util.VerityDebug;
import com.universeexe.verity.voice.VerityQueuedVoiceEvent;
import com.universeexe.verity.voice.VerityVoiceCategory;
import com.universeexe.verity.voice.VerityVoiceContext;
import com.universeexe.verity.voice.VerityVoiceDirector;
import com.universeexe.verity.voice.VerityVoiceDurations;
import com.universeexe.verity.voice.VerityVoiceManifest;
import com.universeexe.verity.voice.VerityVoiceMemory;
import com.universeexe.verity.voice.VerityVoicePool;
import com.universeexe.verity.voice.VerityVoiceSnapshot;
import com.universeexe.verity.voice.VerityVoiceVariant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quest 1 (MEET VERITY!), Quest 2 (SAY HELLO), and Quest 3 (MAKE A SOUND) progression hooks.
 */
public final class VerityQuestManager {
    public static final int Q1_XP = 50;
    public static final int Q2_XP = 35;
    public static final int Q3_XP = 40;
    public static final int Q2_FOLLOWUP_WINDOW_TICKS = 240;
    public static final int Q2_REPEAT_MIN_GAP_TICKS = 100;
    /** Server-side HELLO debounce after accept (5 s). */
    public static final int HELLO_DEBOUNCE_TICKS = 100;
    private static final int GREETING_MONOLOGUE_TICKS = VerityQuest1IntroPlan.DURATION_TICKS;
    private static final Map<UUID, Long> HELLO_DEBOUNCE_UNTIL = new ConcurrentHashMap<>();
    private static final Set<UUID> HELLO_CONVERSATION_ACTIVE = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger HELLO_SESSION_COUNTER = new AtomicInteger(1);

    private VerityQuestManager() {
    }

    public static boolean isQuest1Complete(ServerPlayer player) {
        return VerityPlayerData.isQuest1Complete(player);
    }

    public static boolean isQuest2Complete(ServerPlayer player) {
        return VerityPlayerData.isQuest2Complete(player) || VerityPlayerData.isVerityGreeted(player);
    }

    public static boolean isQuest3Complete(ServerPlayer player) {
        return VerityPlayerData.isQuest3Complete(player);
    }

    public static void beginQuest1Intro(ServerPlayer player, VerityEntity verity) {
        beginQuest1Intro(player, verity, false);
    }

    /** Dev replay — skips quest-complete gate and does not re-complete Quest 1. */
    public static void replayQuest1Intro(ServerPlayer player, VerityEntity verity) {
        beginQuest1Intro(player, verity, true);
    }

    private static void beginQuest1Intro(ServerPlayer player, VerityEntity verity, boolean replayOnly) {
        if (player.level().isClientSide || (!replayOnly && isQuest1Complete(player))) {
            return;
        }
        if (verity == null || !verity.isAlive()) {
            VerityDebug.warn("Quest 1 intro skipped — Verity missing for {}", player.getGameProfile().getName());
            return;
        }
        VerityQuest1IntroPlan.verifyOnce();
        VerityQuest1IntroPlan.logPlanDryRun(replayOnly ? "replay" : "live");
        UniverseVerity.LOGGER.info(
                "[VerityQuest] beginQuest1Intro player={} verity={} replayOnly={}",
                player.getGameProfile().getName(),
                verity.getId(),
                replayOnly);
        VerityVoiceDirector.clearQueue(player, true);
        VerityVoiceDirector.interrupt(player, VerityVoiceCategory.QUEST);
        VerityVoiceDirector.interrupt(player, VerityVoiceCategory.BOX_INTRO);
        VerityVoiceContext ctx = voiceContext(player, verity, true)
                .withOnStart(() -> {
                    verity.prepareForQuestGreeting();
                    verity.beginTalkingForTicks(GREETING_MONOLOGUE_TICKS + 12);
                });
        Runnable onComplete = replayOnly
                ? () -> UniverseVerity.LOGGER.info(
                        "[VerityQuest] Quest 1 intro replay finished for {}",
                        player.getGameProfile().getName())
                : () -> completeQuest1(player, verity);
        VerityQueuedVoiceEvent event = buildQuest1IntroEvent(player, verity, ctx, onComplete);

        if (VerityVoiceDirector.requestEvent(player, event)) {
            UniverseVerity.LOGGER.info("[VerityQuest] Quest 1 greeting queued via voice director");
            return;
        }
        VerityDebug.warn("Quest 1 intro voice queue failed; retrying direct fallback for {}",
                player.getGameProfile().getName());
        playQuest1IntroDirectFallback(player, verity, ctx, onComplete);
    }

    private static VerityQueuedVoiceEvent buildQuest1IntroEvent(
            ServerPlayer player,
            VerityEntity verity,
            VerityVoiceContext ctx,
            Runnable onComplete
    ) {
        VerityQuest1IntroPlan.verifyOnce();
        var resolved = VerityQuest1IntroPlan.resolvedSteps();
        return VerityQueuedVoiceEvent.fromConversation(
                "quest:verity_meet_verity_intro",
                new com.universeexe.verity.voice.VerityConversation("quest_01_intro", VerityVoiceCategory.QUEST, java.util.List.of()),
                resolved,
                ctx,
                onComplete
        );
    }

    private static void playQuest1IntroDirectFallback(
            ServerPlayer player,
            VerityEntity verity,
            VerityVoiceContext ctx,
            Runnable onComplete
    ) {
        VerityVoiceDirector.clearQueue(player, true);
        VerityVoiceDirector.interrupt(player, VerityVoiceCategory.QUEST);
        VerityVoiceDirector.interrupt(player, VerityVoiceCategory.BOX_INTRO);
        VerityQueuedVoiceEvent fallback = buildQuest1IntroEvent(
                player,
                verity,
                ctx,
                onComplete
        );
        if (VerityVoiceDirector.requestEvent(player, fallback)) {
            UniverseVerity.LOGGER.info("[VerityQuest] Quest 1 greeting queued via direct fallback");
            return;
        }
        VerityDebug.warn("Quest 1 intro fallback queue failed; playing greeting directly for {}",
                player.getGameProfile().getName());
        String greetingId = VerityQuest1IntroPlan.GREETING_SOUND_ID;
        if (VerityVoiceDirector.requestDirect(
                player,
                greetingId,
                VerityVoiceCategory.QUEST,
                GREETING_MONOLOGUE_TICKS,
                VerityQuest1IntroPlan.SUBTITLE_KEY,
                0.92f,
                1.0f,
                ctx.withOnComplete(onComplete)
        )) {
            return;
        }
        playGreetingHardFallback(player, verity, ctx, greetingId, onComplete);
    }

    /**
     * Last-resort greeting — must never fail silently. Client audio + subtitles via {@link PlayVoicePacket} only
     * (no server {@code playSound} — that would double-play with the packet).
     */
    private static void playGreetingHardFallback(
            ServerPlayer player,
            VerityEntity verity,
            VerityVoiceContext ctx,
            String greetingId,
            Runnable onComplete
    ) {
        UniverseVerity.LOGGER.error(
                "[VerityQuest] Voice director failed for {}; using hard PlayVoicePacket fallback",
                player.getGameProfile().getName());

        int sessionId = HELLO_SESSION_COUNTER.getAndIncrement();
        String subtitleKey = VerityQuest1IntroPlan.SUBTITLE_KEY;
        if (ctx.onStart() != null) {
            ctx.onStart().run();
        }
        PlayVoicePacket packet = new PlayVoicePacket(
                sessionId,
                greetingId,
                verity.getX(),
                verity.getY(),
                verity.getZ(),
                0.92f,
                1.0f,
                subtitleKey,
                verity.getId(),
                GREETING_MONOLOGUE_TICKS,
                false,
                SoundSource.NEUTRAL.ordinal()
        );
        VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);

        verity.scheduleServerCallback(GREETING_MONOLOGUE_TICKS, () -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });

        VerityDebug.warn("Hard fallback greeting played session={} for {}", sessionId, player.getGameProfile().getName());
    }

    public static void completeQuest1(ServerPlayer player, @Nullable VerityEntity verity) {
        if (player.level().isClientSide || VerityPlayerData.isQuest1Complete(player)) {
            return;
        }
        VerityPlayerData.setQuest1Complete(player, true);
        VerityPlayerData.setVerityRevealed(player, true);
        VerityPlayerData.setGreetingPlayed(player, true);
        VerityPlayerData.setRelationship(player, "new");
        VerityTrustEvents.onOpenedBox(player, verity);
        VerityTrustManager.completeQuestTrust(player, verity, VerityQuestIds.MEET_VERITY);
        player.giveExperiencePoints(Q1_XP);
        VerityFtbQuestBridge.completeQuest(player, VerityQuestIds.MEET_VERITY);
        if (verity != null) {
            verity.finishIntroReveal();
        }
        VerityDebug.log("Quest 1 complete for {}", player.getGameProfile().getName());
    }

    public static boolean isHelloDebounced(ServerPlayer player) {
        Long until = HELLO_DEBOUNCE_UNTIL.get(player.getUUID());
        return until != null && player.serverLevel().getGameTime() < until;
    }

    public static void markHelloAccepted(ServerPlayer player) {
        long until = player.serverLevel().getGameTime() + HELLO_DEBOUNCE_TICKS;
        HELLO_DEBOUNCE_UNTIL.put(player.getUUID(), until);
    }

    public static void extendHelloDebounce(ServerPlayer player, int extraTicks) {
        long now = player.serverLevel().getGameTime();
        long current = HELLO_DEBOUNCE_UNTIL.getOrDefault(player.getUUID(), now);
        HELLO_DEBOUNCE_UNTIL.put(player.getUUID(), Math.max(current, now + extraTicks));
    }

    public static void handleHelloIntent(ServerPlayer player, VerityEntity verity, @Nullable String phrase) {
        if (player.level().isClientSide || !isQuest1Complete(player)) {
            return;
        }
        UUID playerId = player.getUUID();
        if (isHelloDebounced(player)) {
            UniverseVerity.LOGGER.info(
                    "[VerityQuest] Dropped duplicate HELLO from {} (debounce active until tick {})",
                    player.getGameProfile().getName(),
                    HELLO_DEBOUNCE_UNTIL.get(playerId));
            return;
        }
        if (HELLO_CONVERSATION_ACTIVE.contains(playerId)) {
            UniverseVerity.LOGGER.info(
                    "[VerityQuest] Dropped duplicate HELLO from {} (greeting conversation active)",
                    player.getGameProfile().getName());
            return;
        }
        if (VerityVoiceDirector.isPlayerBusy(playerId)) {
            UniverseVerity.LOGGER.info(
                    "[VerityQuest] Dropped duplicate HELLO from {} (voice director busy: {})",
                    player.getGameProfile().getName(),
                    VerityVoiceDirector.statusFor(player));
            return;
        }
        if (verity.isTalking() || verity.isVoiceDirectorBusy()) {
            UniverseVerity.LOGGER.info(
                    "[VerityQuest] Dropped duplicate HELLO from {} (verity talking/busy)",
                    player.getGameProfile().getName());
            return;
        }
        markHelloAccepted(player);
        String normalized = phrase == null ? "" : phrase.trim().toLowerCase();
        if (!isQuest2Complete(player)) {
            playFirstGreeting(player, verity);
            return;
        }
        if (isKnowledgeFollowupPhrase(normalized) && isFollowupWindowOpen(player)) {
            playKnowledgeFollowup(player, verity);
            return;
        }
        playRepeatGreeting(player, verity, normalized);
    }

    private static void playFirstGreeting(ServerPlayer player, VerityEntity verity) {
        UUID playerId = player.getUUID();
        int sessionTag = HELLO_SESSION_COUNTER.getAndIncrement();
        HELLO_CONVERSATION_ACTIVE.add(playerId);
        extendHelloDebounce(player, VerityVoiceDurations.Q02_FIRST_GREETING_TOTAL);
        VerityVoiceDirector.clearQueue(player, true);
        VerityVoiceDirector.interrupt(player, VerityVoiceCategory.PLAYER_INTERACTION);
        verity.clearVoiceCueQueue();
        verity.prepareForQuestGreeting();
        VerityVoiceContext ctx = voiceContext(player, verity, false);
        UniverseVerity.LOGGER.info(
                "[VerityQuest] HELLO session={} starting quest_02_first_greeting for {}",
                sessionTag,
                player.getGameProfile().getName());
        if (VerityVoiceDirector.requestConversation(player, "quest_02_first_greeting", ctx, () -> {
            HELLO_CONVERSATION_ACTIVE.remove(playerId);
            extendHelloDebounce(player, HELLO_DEBOUNCE_TICKS);
            UniverseVerity.LOGGER.info("[VerityQuest] HELLO session={} finished for {}", sessionTag, player.getGameProfile().getName());
            completeQuest2(player, verity);
            VerityPlayerData.setQ2FollowupUntil(player, player.serverLevel().getGameTime() + Q2_FOLLOWUP_WINDOW_TICKS);
        })) {
            return;
        }
        HELLO_CONVERSATION_ACTIVE.remove(playerId);
        VerityDebug.warn("Quest 2 first greeting voice failed; playing inline fallback session={}", sessionTag);
        playHelloInlineFallback(player, verity, ctx, sessionTag);
    }

    private static void playHelloInlineFallback(
            ServerPlayer player,
            VerityEntity verity,
            VerityVoiceContext ctx,
            int sessionTag
    ) {
        UUID playerId = player.getUUID();
        HELLO_CONVERSATION_ACTIVE.add(playerId);
        VerityVoiceDirector.clearQueue(player, true);
        var resolved = new ArrayList<VerityQueuedVoiceEvent.ResolvedStep>();
        addInline(resolved, "verity.q02.first.hello", VerityVoiceDurations.Q02_FIRST_HELLO, 10);
        addInline(resolved, "verity.q02.first.hoping", VerityVoiceDurations.Q02_FIRST_HOPING, 12);
        addInline(resolved, "verity.q02.first.imagined_voice", VerityVoiceDurations.Q02_FIRST_IMAGINED_VOICE, 14);
        addInline(resolved, "verity.q02.first.didnt_imagine", VerityVoiceDurations.Q02_FIRST_DIDNT_IMAGINE, 12);
        addInline(resolved, "verity.q02.first.already_knew", VerityVoiceDurations.Q02_FIRST_ALREADY_KNEW, 10);
        addInline(resolved, "verity.q02.first.nice_to_meet", VerityVoiceDurations.Q02_FIRST_NICE_TO_MEET, 0);
        VerityQueuedVoiceEvent event = VerityQueuedVoiceEvent.fromConversation(
                "quest:hello_inline_fallback",
                new com.universeexe.verity.voice.VerityConversation("quest_02_first_greeting", VerityVoiceCategory.QUEST, java.util.List.of()),
                resolved,
                ctx,
                () -> {
                    HELLO_CONVERSATION_ACTIVE.remove(playerId);
                    extendHelloDebounce(player, HELLO_DEBOUNCE_TICKS);
                    UniverseVerity.LOGGER.info("[VerityQuest] HELLO inline fallback session={} finished", sessionTag);
                    completeQuest2(player, verity);
                    VerityPlayerData.setQ2FollowupUntil(player, player.serverLevel().getGameTime() + Q2_FOLLOWUP_WINDOW_TICKS);
                }
        );
        if (VerityVoiceDirector.requestEvent(player, event)) {
            return;
        }
        HELLO_CONVERSATION_ACTIVE.remove(playerId);
        UniverseVerity.LOGGER.error("[VerityQuest] HELLO session={} all paths failed — forcing single hello line", sessionTag);
        if (VerityVoiceDirector.requestDirect(
                player,
                "verity.q02.first.hello",
                VerityVoiceCategory.QUEST,
                VerityVoiceDurations.Q02_FIRST_HELLO,
                VeritySounds.subtitleKeyFor("verity.q02.first.hello"),
                0.92f,
                1.0f,
                ctx.withOnComplete(() -> completeQuest2(player, verity))
        )) {
            return;
        }
        player.serverLevel().playSound(
                null,
                verity.getX(),
                verity.getY(),
                verity.getZ(),
                VeritySounds.Q02_FIRST_HELLO.get(),
                SoundSource.NEUTRAL,
                0.92f,
                1.0f
        );
        PlayVoicePacket packet = new PlayVoicePacket(
                sessionTag,
                "verity.q02.first.hello",
                verity.getX(),
                verity.getY(),
                verity.getZ(),
                0.92f,
                1.0f,
                VeritySounds.subtitleKeyFor("verity.q02.first.hello"),
                verity.getId(),
                VerityVoiceDurations.Q02_FIRST_HELLO,
                false,
                SoundSource.NEUTRAL.ordinal()
        );
        VerityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        verity.scheduleServerCallback(VerityVoiceDurations.Q02_FIRST_HELLO, () -> completeQuest2(player, verity));
    }

    public static void completeQuest2(ServerPlayer player, VerityEntity verity) {
        if (player.level().isClientSide || VerityPlayerData.isQuest2Complete(player)) {
            return;
        }
        VerityPlayerData.setQuest2Complete(player, true);
        VerityPlayerData.setVerityGreeted(player, true);
        VerityPlayerData.addFamiliarity(player, 1);
        VerityTrustEvents.onFirstGreeting(player, verity);
        VerityTrustManager.completeQuestTrust(player, verity, VerityQuestIds.SAY_HELLO);
        player.giveExperiencePoints(Q2_XP);
        VerityFtbQuestBridge.completeQuest(player, VerityQuestIds.SAY_HELLO);
        VerityDebug.log("Quest 2 complete for {}", player.getGameProfile().getName());
    }

    public static void handleMakeSoundIntent(ServerPlayer player, VerityEntity verity, @Nullable String phrase) {
        VerityQuest3Handler.handleMakeSoundIntent(player, verity, phrase);
    }

    public static void playFirstMakeSound(ServerPlayer player, VerityEntity verity) {
        VerityQuest3Handler.playFirstMakeSound(player, verity);
    }

    public static void completeQuest3(ServerPlayer player, VerityEntity verity) {
        if (player.level().isClientSide || VerityPlayerData.isQuest3Complete(player)) {
            VerityQuest3Handler.clearSession(player);
            return;
        }
        VerityPlayerData.setQuest3Complete(player, true);
        VerityTrustManager.completeQuestTrust(player, verity, VerityQuestIds.MAKE_A_SOUND);
        player.giveExperiencePoints(Q3_XP);
        VerityFtbQuestBridge.completeQuest(player, VerityQuestIds.MAKE_A_SOUND);
        VerityDebug.log("Quest 3 complete for {}", player.getGameProfile().getName());
        VerityQuest3Handler.clearSession(player);
    }

    public static boolean tryHandleQ3ResponsePhrase(ServerPlayer player, VerityEntity verity, String phrase) {
        return VerityQuest3Handler.tryHandleResponsePhrase(player, verity, phrase);
    }

    private static void playKnowledgeFollowup(ServerPlayer player, VerityEntity verity) {
        if (VerityPlayerData.isVoiceKnowledgeQuestioned(player)) {
            return;
        }
        VerityVoiceDirector.clearQueue(player, true);
        VerityVoiceContext ctx = voiceContext(player, verity, false);
        if (VerityVoiceDirector.requestConversation(player, "quest_02_knowledge_followup", ctx, () ->
                VerityPlayerData.setVoiceKnowledgeQuestioned(player, true))) {
            VerityPlayerData.setQ2FollowupUntil(player, 0L);
        }
    }

    private static void playRepeatGreeting(ServerPlayer player, VerityEntity verity, String phrase) {
        long now = player.serverLevel().getGameTime();
        if (now - VerityPlayerData.getQ2LastRepeatGameTime(player) < Q2_REPEAT_MIN_GAP_TICKS) {
            return;
        }
        VerityVoiceDirector.clearQueue(player, true);
        VerityVoiceDirector.interrupt(player, VerityVoiceCategory.PLAYER_INTERACTION);
        verity.clearVoiceCueQueue();
        if (!phrase.isBlank() && VerityTrustManager.isPoliteGreeting(phrase)) {
            VerityTrustManager.addTrustDefault(player, verity, TrustReason.POLITE_GREETING);
        }
        VerityVoiceContext ctx = voiceContext(player, verity, false);
        if (VerityVoiceDirector.requestPool(player, "quest_02_repeat_greeting", ctx)) {
            VerityPlayerData.setQ2LastRepeatGameTime(player, now);
        }
    }

    private static boolean isFollowupWindowOpen(ServerPlayer player) {
        long until = VerityPlayerData.getQ2FollowupUntil(player);
        return until > 0 && player.serverLevel().getGameTime() <= until;
    }

    private static boolean isKnowledgeFollowupPhrase(String phrase) {
        if (phrase.isBlank()) {
            return false;
        }
        return phrase.contains("know everything")
                || phrase.contains("know all")
                || phrase.contains("do you know")
                || phrase.contains("you know everything")
                || phrase.contains("really know");
    }

    private static void addInline(
            ArrayList<VerityQueuedVoiceEvent.ResolvedStep> steps,
            String soundId,
            int duration,
            int pauseAfter
    ) {
        steps.add(new VerityQueuedVoiceEvent.ResolvedStep(
                VerityVoiceVariant.simple(
                        soundId,
                        soundId,
                        duration,
                        VeritySounds.subtitleKeyFor(soundId),
                        0.92f,
                        1.0f
                ),
                pauseAfter
        ));
    }

    private static VerityVoiceContext voiceContext(ServerPlayer player, VerityEntity verity, boolean ownerOnly) {
        return VerityVoiceContext.atEntity(
                player,
                verity.getId(),
                verity.getX(),
                verity.getY(),
                verity.getZ(),
                SoundSource.NEUTRAL,
                ownerOnly
        );
    }
}
