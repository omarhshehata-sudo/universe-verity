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
    /** Matches {@code greeting_personal_helper.ogg} (~5.87 s). */
    private static final int GREETING_MONOLOGUE_TICKS = 118;
    private static final Map<UUID, Long> HELLO_DEBOUNCE_UNTIL = new ConcurrentHashMap<>();
    private static final Set<UUID> HELLO_CONVERSATION_ACTIVE = ConcurrentHashMap.newKeySet();

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
        if (player.level().isClientSide || isQuest1Complete(player)) {
            return;
        }
        if (verity == null || !verity.isAlive()) {
            VerityDebug.warn("Quest 1 intro skipped — Verity missing for {}", player.getGameProfile().getName());
            return;
        }
        VerityVoiceDirector.clearQueue(player, false);
        verity.prepareForQuestGreeting();
        VerityVoiceContext ctx = voiceContext(player, verity, true)
                .withOnStart(() -> verity.beginTalkingForTicks(GREETING_MONOLOGUE_TICKS + 12));
        VerityQueuedVoiceEvent event = buildQuest1IntroEvent(player, verity, ctx, () -> completeQuest1(player, verity));

        if (VerityVoiceDirector.requestEvent(player, event)) {
            return;
        }
        VerityDebug.warn("Quest 1 intro voice queue failed; retrying direct fallback for {}",
                player.getGameProfile().getName());
        playQuest1IntroDirectFallback(player, verity, ctx);
    }

    private static VerityQueuedVoiceEvent buildQuest1IntroEvent(
            ServerPlayer player,
            VerityEntity verity,
            VerityVoiceContext ctx,
            Runnable onComplete
    ) {
        var resolved = new ArrayList<VerityQueuedVoiceEvent.ResolvedStep>();
        addInline(resolved, "verity.greeting.personal_helper", GREETING_MONOLOGUE_TICKS, 0);
        return VerityQueuedVoiceEvent.fromConversation(
                "quest:verity_meet_verity_intro",
                new com.universeexe.verity.voice.VerityConversation("quest_01_intro", VerityVoiceCategory.QUEST, java.util.List.of()),
                resolved,
                ctx,
                onComplete
        );
    }

    private static void playQuest1IntroDirectFallback(ServerPlayer player, VerityEntity verity, VerityVoiceContext ctx) {
        if (ctx.onStart() != null) {
            ctx.onStart().run();
        }
        VerityQueuedVoiceEvent fallback = buildQuest1IntroEvent(
                player,
                verity,
                ctx,
                () -> completeQuest1(player, verity)
        );
        if (VerityVoiceDirector.requestEvent(player, fallback)) {
            return;
        }
        VerityDebug.warn("Quest 1 intro fallback queue failed; playing greeting directly for {}",
                player.getGameProfile().getName());
        String greetingId = "verity.greeting.personal_helper";
        if (VerityVoiceDirector.requestDirect(
                player,
                greetingId,
                VerityVoiceCategory.QUEST,
                GREETING_MONOLOGUE_TICKS,
                com.universeexe.verity.registry.VeritySounds.subtitleKeyFor(greetingId),
                0.92f,
                1.0f,
                ctx
        )) {
            return;
        }
        playGreetingHardFallback(player, verity, ctx, greetingId);
    }

    /**
     * Last-resort greeting — must never fail silently. Plays OGG on server + sends PlayVoicePacket for subtitles.
     */
    private static void playGreetingHardFallback(
            ServerPlayer player,
            VerityEntity verity,
            VerityVoiceContext ctx,
            String greetingId
    ) {
        UniverseVerity.LOGGER.error(
                "[VerityQuest] Voice director failed for {}; using hard audio fallback",
                player.getGameProfile().getName());

        if (ctx.onStart() != null) {
            ctx.onStart().run();
        }

        String subtitleKey = VeritySounds.subtitleKeyFor(greetingId);
        PlayVoicePacket packet = new PlayVoicePacket(
                -1,
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
            if (ctx.onComplete() != null) {
                ctx.onComplete().run();
            } else {
                completeQuest1(player, verity);
            }
        });

        VerityDebug.warn("Hard fallback greeting played for {}", player.getGameProfile().getName());
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
        HELLO_DEBOUNCE_UNTIL.put(
                player.getUUID(),
                player.serverLevel().getGameTime() + HELLO_DEBOUNCE_TICKS
        );
    }

    public static void handleHelloIntent(ServerPlayer player, VerityEntity verity, @Nullable String phrase) {
        if (player.level().isClientSide || !isQuest1Complete(player)) {
            return;
        }
        UUID playerId = player.getUUID();
        if (isHelloDebounced(player)) {
            UniverseVerity.LOGGER.info(
                    "[VerityQuest] Dropped duplicate HELLO from {} (debounce active)",
                    player.getGameProfile().getName());
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
                    "[VerityQuest] Dropped duplicate HELLO from {} (voice director busy)",
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
        HELLO_CONVERSATION_ACTIVE.add(playerId);
        VerityVoiceDirector.clearQueue(player, true);
        verity.clearVoiceCueQueue();
        verity.prepareForQuestGreeting();
        VerityVoiceContext ctx = voiceContext(player, verity, false);
        if (VerityVoiceDirector.requestConversation(player, "quest_02_first_greeting", ctx, () -> {
            HELLO_CONVERSATION_ACTIVE.remove(playerId);
            completeQuest2(player, verity);
            VerityPlayerData.setQ2FollowupUntil(player, player.serverLevel().getGameTime() + Q2_FOLLOWUP_WINDOW_TICKS);
        })) {
            return;
        }
        HELLO_CONVERSATION_ACTIVE.remove(playerId);
        VerityDebug.warn("Quest 2 first greeting voice failed; completing with fallback");
        completeQuest2(player, verity);
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

    private static VerityVoiceVariant pickQuest1Ending(ServerPlayer player, VerityEntity verity) {
        VerityVoiceSnapshot snapshot = VerityVoiceSnapshot.capture(player, verity);
        Optional<VerityVoicePool> pool = VerityVoiceManifest.get().pool("quest_01_intro_ending");
        if (pool.isPresent()) {
            Optional<VerityVoiceVariant> pick = pool.get().pick(
                    player,
                    snapshot,
                    VerityVoiceMemory.forPlayer(player),
                    player.serverLevel().getRandom()
            );
            if (pick.isPresent()) {
                return pick.get();
            }
        }
        boolean important = player.getRandom().nextFloat() >= 0.85f;
        String sound = important ? "verity.q01.intro.everything_important" : "verity.q01.intro.know_everything";
        return VerityVoiceVariant.simple(
                sound,
                sound,
                important ? 38 : 36,
                com.universeexe.verity.registry.VeritySounds.subtitleKeyFor(sound),
                0.92f,
                1.0f
        );
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
                        com.universeexe.verity.registry.VeritySounds.subtitleKeyFor(soundId),
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
