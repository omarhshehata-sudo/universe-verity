package com.universeexe.verity.trust;

/** NBT keys under {@code universe_verity} player persistent root. */
public final class VerityTrustKeys {
    public static final String TRUST = "VerityTrust";
    public static final String MOOD = "VerityMood";
    public static final String COUNTDOWN_STARTED = "VerityCountdownStarted";
    public static final String COUNTDOWN_START_GAME_TIME = "VerityCountdownStartGameTime";
    public static final String TRANSFORM_GAME_TIME = "VerityTransformGameTime";
    public static final String TWO_DAY_LINE_PLAYED = "VerityTwoDayLinePlayed";
    public static final String ONE_DAY_LINE_PLAYED = "VerityOneDayLinePlayed";
    public static final String TRANSFORMED = "VerityTransformed";
    public static final String WARNING_SEQUENCE_PLAYING = "VerityWarningSequencePlaying";
    public static final String WARNING_SEQUENCE_STEP = "VerityWarningSequenceStep";
    public static final String WARNING_SEQUENCE_NEXT_TICK = "VerityWarningSequenceNextTick";
    public static final String LAST_TRUST_VOICE_GAME_TIME = "VerityLastTrustVoiceGameTime";
    public static final String ACTIVE_MONSTER_UUID = "VerityActiveMonsterUuid";
    public static final String TRUST_COOLDOWNS = "VerityTrustCooldowns";
    public static final String DAILY_COUNTERS = "VerityTrustDailyCounters";
    public static final String DAILY_DAY = "VerityTrustDailyDay";
    public static final String COMPLETED_QUESTS = "VerityTrustCompletedQuests";
    public static final String HELP_PENDING_UNTIL = "VerityHelpPendingUntil";
    public static final String HELP_CONVERSATION_ID = "VerityHelpConversationId";
    public static final String GENTLE_PICKUP_DONE = "VerityGentlePickupDone";
    public static final String OPENED_BOX_TRUST = "VerityOpenedBoxTrust";
    public static final String FIRST_GREETING_TRUST = "VerityFirstGreetingTrust";
    public static final String LAST_HIT_TRUST_GAME_TIME = "VerityLastHitTrustGameTime";
    public static final String COMMAND_WINDOW_START = "VerityCommandWindowStart";
    public static final String COMMAND_WINDOW_COUNT = "VerityCommandWindowCount";
    public static final String COMMAND_WINDOW_CATEGORY = "VerityCommandWindowCategory";
    public static final String GREEDY_COUNT_DAY = "VerityGreedyCountDay";
    public static final String ABANDON_DAY_MARK = "VerityAbandonDayMark";
    public static final String LAST_MONSTER_VOICE_GAME_TIME = "VerityLastMonsterVoiceGameTime";
    public static final String STORY_MUTE_RANDOM = "VerityStoryMuteRandomSpeech";

    public static final int MIN_TRUST = -10;
    public static final int MAX_TRUST = 10;
    public static final int START_TRUST = 0;
    public static final long DAY_TICKS = 24000L;
    public static final long COUNTDOWN_TICKS = 72000L; // 3 Minecraft days
    public static final long HIT_COOLDOWN_TICKS = 100L; // 5 seconds
    public static final long MOOD_VOICE_COOLDOWN_TICKS = 1200L; // 60 seconds
    public static final long HELP_THANKS_WINDOW_TICKS = 400L; // 20 seconds
    public static final long COMMAND_SPAM_WINDOW_TICKS = 600L; // 30 seconds
    public static final int COMMAND_SPAM_THRESHOLD = 3;
    public static final int GREEDY_THRESHOLD = 4;
    public static final double ABANDON_DISTANCE = 128.0;

    private VerityTrustKeys() {
    }
}
