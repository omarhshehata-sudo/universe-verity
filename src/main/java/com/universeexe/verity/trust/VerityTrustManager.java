package com.universeexe.verity.trust;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityDemonEntity;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.registry.VerityEntities;
import com.universeexe.verity.util.VerityDebug;
import com.universeexe.verity.voice.VerityVoiceContext;
import com.universeexe.verity.voice.VerityVoiceDirector;
import com.universeexe.verity.voice.VerityVoiceManifest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-only trust / mood / countdown / transform authority.
 * Clients never set trust — they only render synced mood on {@link VerityEntity}.
 */
public final class VerityTrustManager {
    private VerityTrustManager() {
    }

    public static int getTrust(ServerPlayer player) {
        return clamp(VerityPlayerData.get(player).getInt(VerityTrustKeys.TRUST));
    }

    public static int getTrust(ServerPlayer player, @Nullable VerityEntity verity) {
        return getTrust(player);
    }

    public static MoodState determineMood(int trust) {
        return MoodState.fromTrust(trust);
    }

    public static MoodState getMood(ServerPlayer player) {
        if (VerityPlayerData.get(player).getBoolean(VerityTrustKeys.TRANSFORMED)) {
            return MoodState.MONSTER;
        }
        CompoundTag tag = VerityPlayerData.get(player);
        if (tag.contains(VerityTrustKeys.MOOD)) {
            return MoodState.fromName(tag.getString(VerityTrustKeys.MOOD));
        }
        return determineMood(getTrust(player));
    }

    public static boolean canApplyTrustReason(ServerPlayer player, TrustReason reason) {
        return canApplyInternal(player, reason, true) == null;
    }

    @Nullable
    private static String canApplyInternal(ServerPlayer player, TrustReason reason, boolean log) {
        CompoundTag tag = VerityPlayerData.get(player);
        long gameTime = player.serverLevel().getGameTime();
        ensureDailyBucket(player, tag);

        return switch (reason) {
            case OPENED_BOX -> tag.getBoolean(VerityTrustKeys.OPENED_BOX_TRUST) ? "already_applied" : null;
            case FIRST_GREETING -> tag.getBoolean(VerityTrustKeys.FIRST_GREETING_TRUST) ? "already_applied" : null;
            case POLITE_GREETING -> dailyCount(tag, reason) >= 1 ? "daily_limit" : null;
            case THANKED_AFTER_HELP -> {
                long until = tag.getLong(VerityTrustKeys.HELP_PENDING_UNTIL);
                yield until <= 0 || gameTime > until ? "no_pending_help" : null;
            }
            case COMPLETED_VERITY_QUEST -> null; // checked with quest id
            case GENTLE_PICKUP -> tag.getBoolean(VerityTrustKeys.GENTLE_PICKUP_DONE) ? "already_applied" : null;
            case GAVE_LIKED_ITEM -> dailyCount(tag, reason) >= 1 ? "daily_limit" : null;
            case POSITIVE_RESPONSE -> dailyCount(tag, reason) >= 3 ? "daily_limit" : null;
            case HIT_VERITY -> {
                long last = tag.getLong(VerityTrustKeys.LAST_HIT_TRUST_GAME_TIME);
                yield gameTime - last < VerityTrustKeys.HIT_COOLDOWN_TICKS ? "hit_cooldown" : null;
            }
            case DANGEROUS_DROP, STORED_IN_CONTAINER, INSULTED_VERITY ->
                    cooldownReady(tag, reason, gameTime, 200L) ? null : "cooldown";
            case COMMAND_SPAM -> cooldownReady(tag, reason, gameTime, VerityTrustKeys.COMMAND_SPAM_WINDOW_TICKS)
                    ? null : "cooldown";
            case ABANDONED_VERITY -> dailyCount(tag, reason) >= 1 ? "daily_limit" : null;
            case GREEDY_REQUEST_SPAM -> dailyCount(tag, reason) >= 1 ? "daily_limit" : null;
            case DEBUG_SET, DEBUG_ADD -> null;
        };
    }

    public static boolean addTrust(ServerPlayer player, @Nullable VerityEntity verity, int amount, TrustReason reason) {
        if (player.level().isClientSide) {
            return false;
        }
        String block = canApplyInternal(player, reason, true);
        if (block != null && reason != TrustReason.DEBUG_ADD && reason != TrustReason.DEBUG_SET) {
            VerityDebug.log("Trust blocked player={} reason={} cause={}",
                    player.getUUID(), reason, block);
            return false;
        }
        int old = getTrust(player);
        int neu = clamp(old + amount);
        return applyTrust(player, verity, old, neu, reason);
    }

    public static boolean addTrustDefault(ServerPlayer player, @Nullable VerityEntity verity, TrustReason reason) {
        return addTrust(player, verity, reason.defaultDelta(), reason);
    }

    public static boolean setTrust(ServerPlayer player, @Nullable VerityEntity verity, int value, TrustReason reason) {
        if (player.level().isClientSide) {
            return false;
        }
        int old = getTrust(player);
        int neu = clamp(value);
        return applyTrust(player, verity, old, neu, reason);
    }

    private static boolean applyTrust(
            ServerPlayer player,
            @Nullable VerityEntity verity,
            int oldTrust,
            int newTrust,
            TrustReason reason
    ) {
        CompoundTag tag = VerityPlayerData.get(player);
        MoodState oldMood = getMood(player);
        if (tag.getBoolean(VerityTrustKeys.TRANSFORMED)) {
            oldMood = MoodState.MONSTER;
        }

        tag.putInt(VerityTrustKeys.TRUST, newTrust);
        markReasonApplied(player, tag, reason);

        MoodState newMood = tag.getBoolean(VerityTrustKeys.TRANSFORMED)
                ? MoodState.MONSTER
                : determineMood(newTrust);
        tag.putString(VerityTrustKeys.MOOD, newMood.name());

        VerityDebug.log(
                "Trust change player={} verity={} old={} new={} reason={} mood={}→{}",
                player.getUUID(),
                verity != null ? verity.getUUID() : "null",
                oldTrust,
                newTrust,
                reason,
                oldMood,
                newMood
        );

        if (verity != null && !tag.getBoolean(VerityTrustKeys.TRANSFORMED)) {
            syncMoodToTrackingClients(verity, newMood);
        }

        if (newMood != oldMood && newMood != MoodState.MONSTER && !tag.getBoolean(VerityTrustKeys.TRANSFORMED)) {
            maybePlayMoodLine(player, verity, newMood);
        }

        if (!tag.getBoolean(VerityTrustKeys.COUNTDOWN_STARTED)
                && !tag.getBoolean(VerityTrustKeys.TRANSFORMED)
                && newTrust <= -8) {
            beginCountdown(player, verity);
        }

        return oldTrust != newTrust || reason == TrustReason.DEBUG_SET;
    }

    public static void syncMoodToTrackingClients(VerityEntity verity, MoodState mood) {
        if (verity.level().isClientSide) {
            return;
        }
        verity.setMoodState(mood);
        if (mood != MoodState.MONSTER && !verity.isTalking() && !"hurt".equalsIgnoreCase(verity.getFaceVariant())) {
            verity.setFaceVariant("auto");
        }
    }

    public static void beginCountdown(ServerPlayer player, @Nullable VerityEntity verity) {
        CompoundTag tag = VerityPlayerData.get(player);
        if (tag.getBoolean(VerityTrustKeys.COUNTDOWN_STARTED) || tag.getBoolean(VerityTrustKeys.TRANSFORMED)) {
            VerityDebug.log("Countdown already started/transformed for {}", player.getUUID());
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        tag.putBoolean(VerityTrustKeys.COUNTDOWN_STARTED, true);
        tag.putLong(VerityTrustKeys.COUNTDOWN_START_GAME_TIME, now);
        tag.putLong(VerityTrustKeys.TRANSFORM_GAME_TIME, now + VerityTrustKeys.COUNTDOWN_TICKS);
        tag.putBoolean(VerityTrustKeys.TWO_DAY_LINE_PLAYED, false);
        tag.putBoolean(VerityTrustKeys.ONE_DAY_LINE_PLAYED, false);
        tag.putBoolean(VerityTrustKeys.STORY_MUTE_RANDOM, true);
        tag.putString(VerityTrustKeys.MOOD, MoodState.CRITICAL.name());

        VerityDebug.log("Countdown START player={} start={} transformAt={}",
                player.getUUID(), now, now + VerityTrustKeys.COUNTDOWN_TICKS);

        if (verity != null) {
            syncMoodToTrackingClients(verity, MoodState.CRITICAL);
            verity.getNavigation().stop();
            verity.stopFollowing();
            verity.lookAt(player, 360f, 360f);
            startWarningSequence(player, verity);
        }
    }

    private static void startWarningSequence(ServerPlayer player, VerityEntity verity) {
        CompoundTag tag = VerityPlayerData.get(player);
        tag.putBoolean(VerityTrustKeys.WARNING_SEQUENCE_PLAYING, true);
        tag.putInt(VerityTrustKeys.WARNING_SEQUENCE_STEP, 3);
        VerityVoiceContext ctx = trustVoiceContext(player, verity);
        VerityVoiceDirector.requestConversation(player, "countdown_warning_sequence", ctx);
    }

    public static void tickCountdown(ServerLevel level, ServerPlayer player, @Nullable VerityEntity verity) {
        CompoundTag tag = VerityPlayerData.get(player);
        if (tag.getBoolean(VerityTrustKeys.TRANSFORMED)) {
            tickMonsterVoice(level, player);
            return;
        }
        if (!tag.getBoolean(VerityTrustKeys.COUNTDOWN_STARTED)) {
            return;
        }

        long now = level.getGameTime();
        long start = tag.getLong(VerityTrustKeys.COUNTDOWN_START_GAME_TIME);
        long transformAt = tag.getLong(VerityTrustKeys.TRANSFORM_GAME_TIME);

        if (tag.getBoolean(VerityTrustKeys.WARNING_SEQUENCE_PLAYING) && verity != null) {
            if (!VerityVoiceDirector.isPlayerBusy(player.getUUID())) {
                tag.putBoolean(VerityTrustKeys.WARNING_SEQUENCE_PLAYING, false);
            }
        }

        if (!tag.getBoolean(VerityTrustKeys.TWO_DAY_LINE_PLAYED) && now >= start + VerityTrustKeys.DAY_TICKS) {
            tag.putBoolean(VerityTrustKeys.TWO_DAY_LINE_PLAYED, true);
            if (verity != null) {
                VerityVoiceDirector.requestPool(player, "countdown_two_days", trustVoiceContext(player, verity));
            }
            VerityDebug.log("Countdown two-days line player={}", player.getUUID());
        }
        if (!tag.getBoolean(VerityTrustKeys.ONE_DAY_LINE_PLAYED) && now >= start + VerityTrustKeys.DAY_TICKS * 2) {
            tag.putBoolean(VerityTrustKeys.ONE_DAY_LINE_PLAYED, true);
            if (verity != null) {
                VerityVoiceDirector.requestPool(player, "countdown_one_day", trustVoiceContext(player, verity));
            }
            VerityDebug.log("Countdown one-day line player={}", player.getUUID());
        }

        if (now >= transformAt) {
            transformIntoMonster(level, player, verity);
        }
    }

    public static void transformIntoMonster(ServerLevel level, ServerPlayer player, @Nullable VerityEntity verity) {
        CompoundTag tag = VerityPlayerData.get(player);
        if (tag.getBoolean(VerityTrustKeys.TRANSFORMED)) {
            VerityDebug.log("Transform skipped — already transformed {}", player.getUUID());
            return;
        }
        // Persist before spawn to prevent duplicates on crash mid-transform.
        tag.putBoolean(VerityTrustKeys.TRANSFORMED, true);
        tag.putBoolean(VerityTrustKeys.WARNING_SEQUENCE_PLAYING, false);
        tag.putString(VerityTrustKeys.MOOD, MoodState.MONSTER.name());

        Vec3 pos = verity != null ? verity.position() : player.position();
        float yRot = verity != null ? verity.getYRot() : player.getYRot();

        if (verity != null) {
            VerityVoiceDirector.requestPool(player, "countdown_arrival", trustVoiceContext(player, verity));
            VerityVoiceContext transformCtx = VerityVoiceContext.atEntity(
                    player,
                    verity.getId(),
                    verity.getX(),
                    verity.getY(),
                    verity.getZ(),
                    SoundSource.HOSTILE,
                    false
            );
            VerityVoiceDirector.requestPool(player, "countdown_transform", transformCtx);
        }

        VerityDemonEntity monster = VerityEntities.VERITY_DEMON.get().create(level);
        if (monster == null) {
            UniverseVerity.LOGGER.error("[VerityTrust] Failed to create demon entity for {}", player.getUUID());
            return;
        }
        monster.moveTo(pos.x, pos.y, pos.z, yRot, 0);
        monster.setOwnerUUID(player.getUUID());
        monster.setPersistenceRequired();
        level.addFreshEntity(monster);
        tag.putUUID(VerityTrustKeys.ACTIVE_MONSTER_UUID, monster.getUUID());

        if (verity != null) {
            VerityPlayerData.setVerityUuid(player, null);
            verity.discard();
        }

        VerityDebug.log("Transformed player={} monster={}", player.getUUID(), monster.getUUID());
    }

    public static void markHelpCompleted(ServerPlayer player, String conversationId) {
        CompoundTag tag = VerityPlayerData.get(player);
        tag.putLong(VerityTrustKeys.HELP_PENDING_UNTIL,
                player.serverLevel().getGameTime() + VerityTrustKeys.HELP_THANKS_WINDOW_TICKS);
        tag.putString(VerityTrustKeys.HELP_CONVERSATION_ID, conversationId == null ? "" : conversationId);
    }

    public static boolean completeQuestTrust(ServerPlayer player, @Nullable VerityEntity verity, String questId) {
        if (questId == null || questId.isBlank()) {
            return false;
        }
        CompoundTag tag = VerityPlayerData.get(player);
        ListTag list = tag.getList(VerityTrustKeys.COMPLETED_QUESTS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (questId.equals(list.getString(i))) {
                return false;
            }
        }
        list.add(StringTag.valueOf(questId));
        tag.put(VerityTrustKeys.COMPLETED_QUESTS, list);
        return addTrustDefault(player, verity, TrustReason.COMPLETED_VERITY_QUEST);
    }

    public static void noteVoiceCommand(ServerPlayer player, String category) {
        if (category == null || category.isBlank()) {
            return;
        }
        CompoundTag tag = VerityPlayerData.get(player);
        long now = player.serverLevel().getGameTime();
        String cat = category.toLowerCase(Locale.ROOT);
        String windowCat = tag.getString(VerityTrustKeys.COMMAND_WINDOW_CATEGORY);
        long windowStart = tag.getLong(VerityTrustKeys.COMMAND_WINDOW_START);
        int count = tag.getInt(VerityTrustKeys.COMMAND_WINDOW_COUNT);

        if (!cat.equals(windowCat) || now - windowStart > VerityTrustKeys.COMMAND_SPAM_WINDOW_TICKS) {
            windowStart = now;
            count = 1;
            windowCat = cat;
        } else {
            count++;
        }
        tag.putString(VerityTrustKeys.COMMAND_WINDOW_CATEGORY, windowCat);
        tag.putLong(VerityTrustKeys.COMMAND_WINDOW_START, windowStart);
        tag.putInt(VerityTrustKeys.COMMAND_WINDOW_COUNT, count);

        if (count >= VerityTrustKeys.COMMAND_SPAM_THRESHOLD) {
            Optional<VerityEntity> verity = findOwnedVerity(player);
            addTrustDefault(player, verity.orElse(null), TrustReason.COMMAND_SPAM);
            tag.putInt(VerityTrustKeys.COMMAND_WINDOW_COUNT, 0);
        }

        if (isGreedyCategory(cat)) {
            ensureDailyBucket(player, tag);
            int greedy = tag.getInt(VerityTrustKeys.GREEDY_COUNT_DAY) + 1;
            tag.putInt(VerityTrustKeys.GREEDY_COUNT_DAY, greedy);
            if (greedy >= VerityTrustKeys.GREEDY_THRESHOLD) {
                Optional<VerityEntity> verity = findOwnedVerity(player);
                addTrustDefault(player, verity.orElse(null), TrustReason.GREEDY_REQUEST_SPAM);
                tag.putInt(VerityTrustKeys.GREEDY_COUNT_DAY, 0);
            }
        }
    }

    public static boolean isInsultPhrase(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        String t = normalized.toLowerCase(Locale.ROOT);
        boolean addresses = t.contains("verity") || t.startsWith("you ") || t.contains(" you ");
        if (!addresses) {
            return false;
        }
        return t.contains("useless")
                || t.contains("stupid")
                || t.contains("shut up")
                || t.contains("go away")
                || t.contains("i hate you")
                || t.contains("hate you");
    }

    public static boolean isPoliteGreeting(String normalized) {
        if (normalized == null) {
            return false;
        }
        String t = normalized.toLowerCase(Locale.ROOT).trim();
        if (!t.contains("verity")) {
            return false;
        }
        return t.contains("hello") || t.contains("hi ") || t.equals("hi verity")
                || t.startsWith("hi verity") || t.contains("hey")
                || t.contains("good morning") || t.contains("good afternoon")
                || t.contains("good evening");
    }

    public static boolean isThanksPhrase(String normalized) {
        if (normalized == null) {
            return false;
        }
        String t = normalized.toLowerCase(Locale.ROOT).trim();
        return t.equals("thank you") || t.equals("thanks")
                || t.equals("thank you verity") || t.equals("thanks verity")
                || t.startsWith("thank you ") || t.startsWith("thanks ");
    }

    public static Optional<VerityEntity> findOwnedVerity(ServerPlayer player) {
        Optional<UUID> id = VerityPlayerData.getVerityUuid(player);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        Entity e = player.serverLevel().getEntity(id.get());
        if (e instanceof VerityEntity verity && player.getUUID().equals(verity.getOwnerUUID())) {
            return Optional.of(verity);
        }
        return player.serverLevel().getEntitiesOfClass(VerityEntity.class, player.getBoundingBox().inflate(96),
                        v -> player.getUUID().equals(v.getOwnerUUID()))
                .stream().findFirst();
    }

    public static void resetStory(ServerPlayer player) {
        CompoundTag tag = VerityPlayerData.get(player);
        tag.putInt(VerityTrustKeys.TRUST, VerityTrustKeys.START_TRUST);
        tag.putString(VerityTrustKeys.MOOD, MoodState.MEH.name());
        tag.putBoolean(VerityTrustKeys.COUNTDOWN_STARTED, false);
        tag.putLong(VerityTrustKeys.COUNTDOWN_START_GAME_TIME, 0);
        tag.putLong(VerityTrustKeys.TRANSFORM_GAME_TIME, 0);
        tag.putBoolean(VerityTrustKeys.TWO_DAY_LINE_PLAYED, false);
        tag.putBoolean(VerityTrustKeys.ONE_DAY_LINE_PLAYED, false);
        tag.putBoolean(VerityTrustKeys.TRANSFORMED, false);
        tag.putBoolean(VerityTrustKeys.WARNING_SEQUENCE_PLAYING, false);
        tag.remove(VerityTrustKeys.ACTIVE_MONSTER_UUID);
        tag.putBoolean(VerityTrustKeys.STORY_MUTE_RANDOM, false);
        tag.putBoolean(VerityTrustKeys.OPENED_BOX_TRUST, false);
        tag.putBoolean(VerityTrustKeys.GENTLE_PICKUP_DONE, false);
        tag.put(VerityTrustKeys.COMPLETED_QUESTS, new ListTag());
        tag.put(VerityTrustKeys.TRUST_COOLDOWNS, new CompoundTag());
        tag.put(VerityTrustKeys.DAILY_COUNTERS, new CompoundTag());
        Optional<VerityEntity> verity = findOwnedVerity(player);
        verity.ifPresent(v -> syncMoodToTrackingClients(v, MoodState.MEH));
    }

    public static String statusText(ServerPlayer player) {
        CompoundTag tag = VerityPlayerData.get(player);
        long now = player.serverLevel().getGameTime();
        long transformAt = tag.getLong(VerityTrustKeys.TRANSFORM_GAME_TIME);
        long remaining = tag.getBoolean(VerityTrustKeys.COUNTDOWN_STARTED) && !tag.getBoolean(VerityTrustKeys.TRANSFORMED)
                ? Math.max(0, transformAt - now) : -1;
        return "trust=" + getTrust(player)
                + " mood=" + getMood(player)
                + " countdown=" + tag.getBoolean(VerityTrustKeys.COUNTDOWN_STARTED)
                + " transformed=" + tag.getBoolean(VerityTrustKeys.TRANSFORMED)
                + " remainingTicks=" + remaining
                + " monster=" + (tag.hasUUID(VerityTrustKeys.ACTIVE_MONSTER_UUID)
                ? tag.getUUID(VerityTrustKeys.ACTIVE_MONSTER_UUID).toString() : "none");
    }

    private static void maybePlayMoodLine(ServerPlayer player, @Nullable VerityEntity verity, MoodState mood) {
        CompoundTag tag = VerityPlayerData.get(player);
        long now = player.serverLevel().getGameTime();
        long last = tag.getLong(VerityTrustKeys.LAST_TRUST_VOICE_GAME_TIME);
        if (now - last < VerityTrustKeys.MOOD_VOICE_COOLDOWN_TICKS) {
            return;
        }
        tag.putLong(VerityTrustKeys.LAST_TRUST_VOICE_GAME_TIME, now);
        String poolId = moodPoolId(mood);
        if (poolId != null && verity != null) {
            VerityVoiceDirector.requestPool(player, poolId, trustVoiceContext(player, verity));
        }
    }

    @Nullable
    private static String moodPoolId(MoodState mood) {
        return switch (mood) {
            case HAPPY -> "trust_happy";
            case FRIENDLY -> "trust_friendly";
            case MEH -> "trust_meh";
            case ANGRY -> "trust_angry";
            case FURIOUS, CRITICAL -> "trust_furious";
            case MONSTER -> null;
        };
    }

    private static void tickMonsterVoice(ServerLevel level, ServerPlayer player) {
        CompoundTag tag = VerityPlayerData.get(player);
        if (!tag.hasUUID(VerityTrustKeys.ACTIVE_MONSTER_UUID)) {
            return;
        }
        long now = level.getGameTime();
        long last = tag.getLong(VerityTrustKeys.LAST_MONSTER_VOICE_GAME_TIME);
        // Rare: ~every 3–8 minutes of game time.
        long cooldown = 3600L + level.random.nextInt(6000);
        if (now - last < cooldown) {
            return;
        }
        Entity e = level.getEntity(tag.getUUID(VerityTrustKeys.ACTIVE_MONSTER_UUID));
        if (!(e instanceof VerityDemonEntity monster)) {
            return;
        }
        if (monster.distanceTo(player) > 48) {
            return;
        }
        tag.putLong(VerityTrustKeys.LAST_MONSTER_VOICE_GAME_TIME, now);
        VerityVoiceContext ctx = VerityVoiceContext.atEntity(
                player,
                monster.getId(),
                monster.getX(),
                monster.getY(),
                monster.getZ(),
                SoundSource.HOSTILE,
                true
        );
        VerityVoiceDirector.requestPool(player, "monster_ambient", ctx);
    }

    private static VerityVoiceContext trustVoiceContext(ServerPlayer player, VerityEntity verity) {
        return VerityVoiceContext.atEntity(
                player,
                verity.getId(),
                verity.getX(),
                verity.getY(),
                verity.getZ(),
                SoundSource.NEUTRAL,
                true
        );
    }

    private static void markReasonApplied(ServerPlayer player, CompoundTag tag, TrustReason reason) {
        long now = player.serverLevel().getGameTime();
        ensureDailyBucket(player, tag);
        switch (reason) {
            case OPENED_BOX -> tag.putBoolean(VerityTrustKeys.OPENED_BOX_TRUST, true);
            case FIRST_GREETING -> tag.putBoolean(VerityTrustKeys.FIRST_GREETING_TRUST, true);
            case GENTLE_PICKUP -> tag.putBoolean(VerityTrustKeys.GENTLE_PICKUP_DONE, true);
            case HIT_VERITY -> tag.putLong(VerityTrustKeys.LAST_HIT_TRUST_GAME_TIME, now);
            case THANKED_AFTER_HELP -> {
                tag.putLong(VerityTrustKeys.HELP_PENDING_UNTIL, 0);
                tag.putString(VerityTrustKeys.HELP_CONVERSATION_ID, "");
            }
            case POLITE_GREETING, GAVE_LIKED_ITEM, POSITIVE_RESPONSE,
                    ABANDONED_VERITY, GREEDY_REQUEST_SPAM -> incrementDaily(tag, reason);
            case DANGEROUS_DROP, STORED_IN_CONTAINER, INSULTED_VERITY, COMMAND_SPAM ->
                    putCooldown(tag, reason, now);
            default -> {
            }
        }
    }

    private static void ensureDailyBucket(ServerPlayer player, CompoundTag tag) {
        long day = player.serverLevel().getDayTime() / VerityTrustKeys.DAY_TICKS;
        long stored = tag.getLong(VerityTrustKeys.DAILY_DAY);
        if (stored != day) {
            tag.putLong(VerityTrustKeys.DAILY_DAY, day);
            tag.put(VerityTrustKeys.DAILY_COUNTERS, new CompoundTag());
            tag.putInt(VerityTrustKeys.GREEDY_COUNT_DAY, 0);
        }
    }

    private static int dailyCount(CompoundTag tag, TrustReason reason) {
        CompoundTag daily = tag.getCompound(VerityTrustKeys.DAILY_COUNTERS);
        return daily.getInt(reason.name());
    }

    private static void incrementDaily(CompoundTag tag, TrustReason reason) {
        CompoundTag daily = tag.getCompound(VerityTrustKeys.DAILY_COUNTERS);
        daily.putInt(reason.name(), daily.getInt(reason.name()) + 1);
        tag.put(VerityTrustKeys.DAILY_COUNTERS, daily);
    }

    private static boolean cooldownReady(CompoundTag tag, TrustReason reason, long now, long minGap) {
        CompoundTag cds = tag.getCompound(VerityTrustKeys.TRUST_COOLDOWNS);
        long last = cds.getLong(reason.name());
        return now - last >= minGap;
    }

    private static void putCooldown(CompoundTag tag, TrustReason reason, long now) {
        CompoundTag cds = tag.getCompound(VerityTrustKeys.TRUST_COOLDOWNS);
        cds.putLong(reason.name(), now);
        tag.put(VerityTrustKeys.TRUST_COOLDOWNS, cds);
    }

    private static boolean isGreedyCategory(String cat) {
        return cat.contains("diamond") || cat.contains("village") || cat.contains("ore")
                || cat.contains("resource") || cat.contains("find_nearest");
    }

    public static int clamp(int value) {
        return Math.max(VerityTrustKeys.MIN_TRUST, Math.min(VerityTrustKeys.MAX_TRUST, value));
    }
}
