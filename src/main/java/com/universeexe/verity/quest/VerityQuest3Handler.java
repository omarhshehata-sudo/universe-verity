package com.universeexe.verity.quest;

import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.trust.TrustReason;
import com.universeexe.verity.trust.VerityTrustManager;
import com.universeexe.verity.util.VerityDebug;
import com.universeexe.verity.voice.VerityQueuedVoiceEvent;
import com.universeexe.verity.voice.VerityVoiceCategory;
import com.universeexe.verity.voice.VerityVoiceContext;
import com.universeexe.verity.voice.VerityVoiceDirector;
import com.universeexe.verity.voice.VerityVoiceVariant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quest 3 — MAKE A SOUND: first-time intro + demos + response windows, then repeatable demos.
 */
public final class VerityQuest3Handler {
    public enum DemoType {
        BEEP(20),
        CAT(20),
        CAVE(20),
        BOOM(15),
        SQUEAK(15),
        IMITATION(10);

        final int weight;

        DemoType(int weight) {
            this.weight = weight;
        }
    }

    public enum AwaitingResponse {
        NONE,
        CAVE,
        EVALUATION
    }

    private enum PlayerResponse {
        POSITIVE,
        NEGATIVE,
        ANOTHER,
        CAVE_YES,
        CAVE_NO,
        UNRECOGNIZED
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public VerityQuest3Handler() {
    }

    public static boolean isQuest3Complete(ServerPlayer player) {
        return VerityPlayerData.isQuest3Complete(player);
    }

    public static void handleMakeSoundIntent(ServerPlayer player, VerityEntity verity, @Nullable String phrase) {
        if (player.level().isClientSide || !VerityQuestManager.isQuest2Complete(player)) {
            return;
        }
        Session existing = SESSIONS.get(player.getUUID());
        if (existing != null && existing.awaiting != AwaitingResponse.NONE) {
            tryHandleResponsePhrase(player, verity, phrase == null ? "" : phrase);
            return;
        }
        if (existing != null || VerityVoiceDirector.isPlayerBusy(player.getUUID())) {
            return;
        }
        if (!isQuest3Complete(player)) {
            playFirstMakeSound(player, verity);
        } else {
            beginRepeat(player, verity);
        }
    }

    /** First-time MAKE A SOUND: manifest intro conversation, then weighted demo branch. */
    public static void playFirstMakeSound(ServerPlayer player, VerityEntity verity) {
        beginFirstTime(player, verity);
    }

    public static boolean tryHandleResponsePhrase(ServerPlayer player, VerityEntity verity, String phrase) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.awaiting == AwaitingResponse.NONE) {
            return false;
        }
        if (session.verityId != verity.getId()) {
            return false;
        }
        long now = player.serverLevel().getGameTime();
        if (now > session.responseDeadline) {
            return false;
        }
        PlayerResponse response = classifyResponse(phrase, session);
        if (response == PlayerResponse.UNRECOGNIZED) {
            return false;
        }
        session.awaiting = AwaitingResponse.NONE;
        session.responseDeadline = 0L;
        playResponseBranch(player, verity, session, response);
        return true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (Session session : SESSIONS.values()) {
            if (session.awaiting == AwaitingResponse.NONE || session.responseDeadline <= 0) {
                continue;
            }
            ServerPlayer player = session.player();
            if (player == null) {
                continue;
            }
            if (player.serverLevel().getGameTime() <= session.responseDeadline) {
                continue;
            }
            onResponseWindowExpired(player, session);
        }
    }

    private static void onResponseWindowExpired(ServerPlayer player, Session session) {
        AwaitingResponse was = session.awaiting;
        session.awaiting = AwaitingResponse.NONE;
        session.responseDeadline = 0L;
        VerityEntity verity = findVerity(player, session.verityId);
        if (verity == null) {
            clearSession(player);
            return;
        }
        if (was == AwaitingResponse.CAVE) {
            playClosingLines(player, verity, session,
                    new String[]{VerityQuest3Sounds.CAVE_SILENCE_01},
                    new int[]{0},
                    true,
                    null);
            return;
        }
        finishFirstTimeSilence(player, verity, session);
    }

    private static void beginFirstTime(ServerPlayer player, VerityEntity verity) {
        DemoType demo = pickDemo(player, null);
        Session session = new Session(player.getUUID(), verity.getId(), true, demo);
        SESSIONS.put(player.getUUID(), session);
        verity.clearVoiceCueQueue();
        VerityVoiceContext ctx = voiceContext(player, verity);
        if (VerityVoiceDirector.requestConversation(player, "quest_03_first_make_sound", ctx,
                () -> playFirstTimeDemo(player, verity, session))) {
            return;
        }
        playSequence(player, verity, "quest:verity_make_a_sound_intro", steps -> {
            steps.add(step(VerityQuest3Sounds.INTRO_01, VerityQuest3Sounds.PAUSE_700MS));
            steps.add(step(VerityQuest3Sounds.INTRO_02, VerityQuest3Sounds.PAUSE_1S));
        }, () -> playFirstTimeDemo(player, verity, session));
    }

    private static void beginRepeat(ServerPlayer player, VerityEntity verity) {
        DemoType demo = pickDemo(player, null);
        Session session = new Session(player.getUUID(), verity.getId(), false, demo);
        SESSIONS.put(player.getUUID(), session);
        verity.clearVoiceCueQueue();
        String opener = pickRepeatOpener(player);
        playSequence(player, verity, "quest:verity_make_a_sound_repeat", steps -> {
            steps.add(step(opener, VerityQuest3Sounds.PAUSE_800MS));
        }, () -> playRepeatDemo(player, verity, session));
    }

    private static void playFirstTimeDemo(ServerPlayer player, VerityEntity verity, Session session) {
        switch (session.demo) {
            case BEEP -> playSequence(player, verity, "quest:verity_q03_demo_beep", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_TINY_BEEP, 0));
                steps.add(step(VerityQuest3Sounds.BEEP_01, VerityQuest3Sounds.PAUSE_700MS));
                steps.add(step(VerityQuest3Sounds.BEEP_02, VerityQuest3Sounds.PAUSE_800MS));
            }, () -> openEvaluationWindow(player, verity, session));
            case CAT -> playSequence(player, verity, "quest:verity_q03_demo_cat", steps -> {
                steps.add(step(VerityQuest3Sounds.CAT_01, VerityQuest3Sounds.PAUSE_1500MS));
                addIfPresent(steps, VerityQuest3Sounds.CAT_02, VerityQuest3Sounds.PAUSE_800MS);
            }, () -> openEvaluationWindow(player, verity, session));
            case CAVE -> playSequence(player, verity, "quest:verity_q03_demo_cave", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_CAVE, VerityQuest3Sounds.PAUSE_800MS));
                steps.add(step(VerityQuest3Sounds.CAVE_01, 0));
            }, () -> openCaveWindow(player, verity, session));
            case BOOM -> playSequence(player, verity, "quest:verity_q03_demo_boom", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_BOOM, VerityQuest3Sounds.PAUSE_1S));
                steps.add(step(VerityQuest3Sounds.BOOM_01, VerityQuest3Sounds.PAUSE_800MS));
            }, () -> openEvaluationWindow(player, verity, session));
            case SQUEAK -> playSequence(player, verity, "quest:verity_q03_demo_squeak", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_SQUEAK, VerityQuest3Sounds.PAUSE_1S));
                steps.add(step(VerityQuest3Sounds.SQUEAK_01, VerityQuest3Sounds.PAUSE_800MS));
            }, () -> openEvaluationWindow(player, verity, session));
            case IMITATION -> playSequence(player, verity, "quest:verity_q03_demo_imitation", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_FAKE_IMITATION, VerityQuest3Sounds.PAUSE_900MS));
                addIfPresent(steps, VerityQuest3Sounds.IMITATION_01, VerityQuest3Sounds.PAUSE_800MS);
            }, () -> openEvaluationWindow(player, verity, session));
        }
    }

    private static void playRepeatDemo(ServerPlayer player, VerityEntity verity, Session session) {
        if (session.demo == DemoType.CAVE && shouldPlayRareNightCave(player, session)) {
            playRareNightCave(player, verity, session);
            return;
        }
        switch (session.demo) {
            case BEEP -> playSequence(player, verity, "quest:verity_q03_repeat_beep", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_TINY_BEEP, 0));
                steps.add(step(VerityQuest3Sounds.BEEP_01, VerityQuest3Sounds.PAUSE_700MS));
                addIfPresent(steps, VerityQuest3Sounds.REPEAT_BEEP_01, 0);
            }, () -> finishRepeat(player, verity, session));
            case CAT -> playSequence(player, verity, "quest:verity_q03_repeat_cat", steps -> {
                steps.add(step(VerityQuest3Sounds.CAT_01, VerityQuest3Sounds.PAUSE_1500MS));
                steps.add(step(VerityQuest3Sounds.REPEAT_CAT_01, 0));
            }, () -> finishRepeat(player, verity, session));
            case CAVE -> playSequence(player, verity, "quest:verity_q03_repeat_cave", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_CAVE, VerityQuest3Sounds.PAUSE_800MS));
                steps.add(step(VerityQuest3Sounds.REPEAT_CAVE_01, 0));
            }, () -> finishRepeat(player, verity, session));
            case BOOM -> playSequence(player, verity, "quest:verity_q03_repeat_boom", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_BOOM, VerityQuest3Sounds.PAUSE_1S));
                steps.add(step(VerityQuest3Sounds.BOOM_01, VerityQuest3Sounds.PAUSE_800MS));
                steps.add(step(VerityQuest3Sounds.REPEAT_BOOM_01, 0));
            }, () -> finishRepeat(player, verity, session));
            case SQUEAK -> playSequence(player, verity, "quest:verity_q03_repeat_squeak", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_SQUEAK, VerityQuest3Sounds.PAUSE_1S));
                steps.add(step(VerityQuest3Sounds.SQUEAK_01, VerityQuest3Sounds.PAUSE_800MS));
                steps.add(step(VerityQuest3Sounds.REPEAT_SQUEAK_01, 0));
            }, () -> finishRepeat(player, verity, session));
            case IMITATION -> playSequence(player, verity, "quest:verity_q03_repeat_imitation", steps -> {
                steps.add(step(VerityQuest3Sounds.SFX_FAKE_IMITATION, VerityQuest3Sounds.PAUSE_900MS));
                addIfPresent(steps, VerityQuest3Sounds.IMITATION_01, VerityQuest3Sounds.PAUSE_800MS);
                steps.add(step(VerityQuest3Sounds.REPEAT_IMITATION_01, 0));
            }, () -> finishRepeat(player, verity, session));
        }
    }

    private static void playRareNightCave(ServerPlayer player, VerityEntity verity, Session session) {
        VerityPlayerData.setHeardUnknownSound(player, true);
        playSequence(player, verity, "quest:verity_q03_repeat_cave_rare", steps -> {
            steps.add(step(VerityQuest3Sounds.SFX_CAVE, VerityQuest3Sounds.PAUSE_800MS));
            steps.add(step(VerityQuest3Sounds.REPEAT_CAVE_RARE_01, 0));
        }, () -> {
            lookTowardDarkArea(player, verity);
            clearSession(player);
        });
    }

    private static void openEvaluationWindow(ServerPlayer player, VerityEntity verity, Session session) {
        playSequence(player, verity, "quest:verity_q03_evaluation", steps -> {
            steps.add(step(VerityQuest3Sounds.EVALUATION_01, 0));
        }, () -> {
            session.awaiting = AwaitingResponse.EVALUATION;
            session.responseDeadline = player.serverLevel().getGameTime() + VerityQuest3Sounds.EVAL_RESPONSE_WINDOW;
        });
    }

    private static void openCaveWindow(ServerPlayer player, VerityEntity verity, Session session) {
        session.awaiting = AwaitingResponse.CAVE;
        session.responseDeadline = player.serverLevel().getGameTime() + VerityQuest3Sounds.CAVE_RESPONSE_WINDOW;
    }

    private static void playResponseBranch(ServerPlayer player, VerityEntity verity, Session session, PlayerResponse response) {
        if (session.firstTime && session.demo == DemoType.CAT && response == PlayerResponse.NEGATIVE) {
            playClosingLines(player, verity, session,
                    new String[]{VerityQuest3Sounds.CAT_DEFENSE_01, VerityQuest3Sounds.CAT_DEFENSE_02},
                    new int[]{VerityQuest3Sounds.PAUSE_700MS, 0},
                    true,
                    null);
            return;
        }
        switch (response) {
            case POSITIVE -> playClosingLines(player, verity, session,
                    new String[]{VerityQuest3Sounds.RESPONSE_POSITIVE_01},
                    new int[]{0},
                    true,
                    session.firstTime ? () -> VerityTrustManager.addTrustDefault(player, verity, TrustReason.POSITIVE_RESPONSE) : null);
            case NEGATIVE -> playClosingLines(player, verity, session,
                    new String[]{VerityQuest3Sounds.RESPONSE_NEGATIVE_01, VerityQuest3Sounds.RESPONSE_NEGATIVE_02},
                    new int[]{VerityQuest3Sounds.PAUSE_1200MS, 0},
                    true,
                    null);
            case ANOTHER -> playClosingLines(player, verity, session,
                    new String[]{VerityQuest3Sounds.RESPONSE_ANOTHER_01, VerityQuest3Sounds.RESPONSE_ANOTHER_02},
                    new int[]{VerityQuest3Sounds.PAUSE_800MS, 0},
                    true,
                    null);
            case CAVE_YES -> playClosingLines(player, verity, session,
                    new String[]{VerityQuest3Sounds.CAVE_YES_01, VerityQuest3Sounds.CAVE_YES_02},
                    new int[]{VerityQuest3Sounds.PAUSE_800MS, 0},
                    true,
                    null);
            case CAVE_NO -> playClosingLines(player, verity, session,
                    new String[]{VerityQuest3Sounds.CAVE_NO_01},
                    new int[]{0},
                    true,
                    null);
            default -> {
            }
        }
    }

    private static void playClosingLines(
            ServerPlayer player,
            VerityEntity verity,
            Session session,
            String[] sounds,
            int[] pauses,
            boolean completeQuest,
            @Nullable Runnable after
    ) {
        playLineChain(player, verity, 0, sounds, pauses, () -> {
            if (after != null) {
                after.run();
            }
            if (completeQuest && session.firstTime) {
                VerityQuestManager.completeQuest3(player, verity);
            } else {
                clearSession(player);
            }
        });
    }

    private static void playLineChain(
            ServerPlayer player,
            VerityEntity verity,
            int index,
            String[] sounds,
            int[] pauses,
            Runnable onComplete
    ) {
        if (index >= sounds.length) {
            onComplete.run();
            return;
        }
        int pause = index < pauses.length ? pauses[index] : 0;
        playSequence(player, verity, "quest:verity_q03_response_" + index, steps -> steps.add(step(sounds[index], pause)), () ->
                playLineChain(player, verity, index + 1, sounds, pauses, onComplete));
    }

    private static void finishFirstTimeSilence(ServerPlayer player, VerityEntity verity, Session session) {
        verity.resetExpression();
        VerityQuestManager.completeQuest3(player, verity);
    }

    private static void finishRepeat(ServerPlayer player, VerityEntity verity, Session session) {
        clearSession(player);
    }

    static void clearSession(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    private static DemoType pickDemo(ServerPlayer player, @Nullable DemoType exclude) {
        int total = 0;
        for (DemoType type : DemoType.values()) {
            if (type != exclude) {
                total += type.weight;
            }
        }
        int roll = player.getRandom().nextInt(Math.max(1, total));
        int cursor = 0;
        for (DemoType type : DemoType.values()) {
            if (type == exclude) {
                continue;
            }
            cursor += type.weight;
            if (roll < cursor) {
                return type;
            }
        }
        return DemoType.BEEP;
    }

    private static String pickRepeatOpener(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        long lastRare = VerityPlayerData.getQ3LastRareRequestGameTime(player);
        boolean rareReady = now - lastRare >= VerityQuest3Sounds.REPEAT_REQUEST_RARE_COOLDOWN;
        int roll = player.getRandom().nextInt(100);
        if (rareReady && roll < 5) {
            VerityPlayerData.setQ3LastRareRequestGameTime(player, now);
            return VerityQuest3Sounds.REPEAT_REQUEST_RARE_01;
        }
        if (roll < 40) {
            return VerityQuest3Sounds.REPEAT_REQUEST_01;
        }
        if (roll < 75) {
            return VerityQuest3Sounds.REPEAT_REQUEST_02;
        }
        return VerityQuest3Sounds.REPEAT_REQUEST_03;
    }

    private static boolean shouldPlayRareNightCave(ServerPlayer player, Session session) {
        if (session.demo != DemoType.CAVE) {
            return false;
        }
        if (VerityPlayerData.hasHeardUnknownSound(player)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (level.isDay()) {
            return false;
        }
        return player.getRandom().nextFloat() < 0.25f;
    }

    private static void lookTowardDarkArea(ServerPlayer player, VerityEntity verity) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = verity.position();
        Vec3 best = origin;
        int bestLight = level.getMaxLocalRawBrightness(verity.blockPosition());
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                var pos = verity.blockPosition().offset(dx, 0, dz);
                int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
                int skyLight = level.getBrightness(LightLayer.SKY, pos);
                int combined = Math.max(blockLight, skyLight);
                if (combined < bestLight) {
                    bestLight = combined;
                    best = Vec3.atCenterOf(pos);
                }
            }
        }
        verity.getLookControl().setLookAt(best.x, best.y, best.z);
    }

    private static PlayerResponse classifyResponse(String phrase, Session session) {
        String normalized = normalize(phrase);
        if (normalized.isBlank()) {
            return PlayerResponse.UNRECOGNIZED;
        }
        if (session.awaiting == AwaitingResponse.CAVE) {
            if (matchesAny(normalized, "yes", "yeah", "yep", "a little", "that was creepy", "that scared me", "kind of", "little bit")) {
                return PlayerResponse.CAVE_YES;
            }
            if (matchesAny(normalized, "no", "not really", "that was not scary", "that did not scare me", "not scary", "nope")) {
                return PlayerResponse.CAVE_NO;
            }
            return PlayerResponse.UNRECOGNIZED;
        }
        if (matchesAnother(normalized)) {
            return PlayerResponse.ANOTHER;
        }
        if (matchesPositive(normalized)) {
            return PlayerResponse.POSITIVE;
        }
        if (matchesNegative(normalized)) {
            return PlayerResponse.NEGATIVE;
        }
        return PlayerResponse.UNRECOGNIZED;
    }

    private static boolean matchesAnother(String phrase) {
        return phrase.contains("another")
                || phrase.contains("one more")
                || phrase.contains("do it again")
                || phrase.contains("again")
                || phrase.contains("do another");
    }

    private static boolean matchesPositive(String phrase) {
        return phrase.equals("yes")
                || phrase.equals("yeah")
                || phrase.equals("yep")
                || phrase.contains("that was good")
                || phrase.contains("i liked it")
                || phrase.contains("that was funny")
                || phrase.contains("good job")
                || phrase.contains("nice")
                || phrase.contains("love it");
    }

    private static boolean matchesNegative(String phrase) {
        return phrase.equals("no")
                || phrase.equals("nope")
                || phrase.contains("not really")
                || phrase.contains("that was bad")
                || phrase.contains("that was terrible")
                || phrase.contains("did not like")
                || phrase.contains("don't like")
                || phrase.contains("do not like")
                || phrase.equals("bad");
    }

    private static boolean matchesAny(String phrase, String... needles) {
        for (String needle : needles) {
            if (phrase.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String phrase) {
        return phrase == null ? "" : phrase.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    @FunctionalInterface
    private interface StepBuilder {
        void build(ArrayList<VerityQueuedVoiceEvent.ResolvedStep> steps);
    }

    private static void playSequence(
            ServerPlayer player,
            VerityEntity verity,
            String requestId,
            StepBuilder builder,
            @Nullable Runnable onComplete
    ) {
        var steps = new ArrayList<VerityQueuedVoiceEvent.ResolvedStep>();
        builder.build(steps);
        if (steps.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        VerityVoiceContext ctx = voiceContext(player, verity);
        var built = VerityQueuedVoiceEvent.builder(requestId, VerityVoiceCategory.QUEST, ctx).onComplete(onComplete);
        for (VerityQueuedVoiceEvent.ResolvedStep resolved : steps) {
            built.add(resolved.variant(), resolved.pauseAfterTicks());
        }
        if (!VerityVoiceDirector.requestEvent(player, built.build())) {
            VerityDebug.warn("Quest 3 voice failed for {} ({})", player.getGameProfile().getName(), requestId);
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private static VerityQueuedVoiceEvent.ResolvedStep step(String soundId, int pauseAfter) {
        return new VerityQueuedVoiceEvent.ResolvedStep(variant(soundId), pauseAfter);
    }

    private static void addIfPresent(ArrayList<VerityQueuedVoiceEvent.ResolvedStep> steps, String soundId, int pauseAfter) {
        if (!VerityQuest3Sounds.isAssetMissing(soundId)) {
            steps.add(step(soundId, pauseAfter));
        }
    }

    private static VerityVoiceVariant variant(String soundId) {
        return VerityVoiceVariant.simple(
                soundId,
                soundId,
                VerityQuest3Sounds.durationTicks(soundId),
                VerityQuest3Sounds.subtitleKey(soundId),
                soundId.startsWith("verity.q03.sfx_") ? 0.85f : 0.92f,
                1.0f
        );
    }

    private static VerityVoiceContext voiceContext(ServerPlayer player, VerityEntity verity) {
        return VerityVoiceContext.atEntity(
                player,
                verity.getId(),
                verity.getX(),
                verity.getY(),
                verity.getZ(),
                SoundSource.NEUTRAL,
                false
        );
    }

    @Nullable
    private static VerityEntity findVerity(ServerPlayer player, int entityId) {
        if (!(player.level().getEntity(entityId) instanceof VerityEntity verity)) {
            return null;
        }
        return verity;
    }

    private static final class Session {
        private final UUID playerId;
        private final int verityId;
        private final boolean firstTime;
        private final DemoType demo;
        private AwaitingResponse awaiting = AwaitingResponse.NONE;
        private long responseDeadline;

        private Session(UUID playerId, int verityId, boolean firstTime, DemoType demo) {
            this.playerId = playerId;
            this.verityId = verityId;
            this.firstTime = firstTime;
            this.demo = demo;
        }

        @Nullable
        private ServerPlayer player() {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return null;
            }
            return server.getPlayerList().getPlayer(playerId);
        }
    }
}
