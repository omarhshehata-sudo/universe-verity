package com.universeexe.verity.data;

public final class VerityIntroDataKeys {
    public static final String ROOT = "universe_verity";
    public static final String INTRO_STARTED = "VerityIntroStarted";
    public static final String INTRO_COMPLETED = "VerityIntroCompleted";
    public static final String BOX_UUID = "VerityBoxUUID";
    public static final String INTRO_WORLD = "VerityIntroWorld";
    public static final String INTRO_STAGE = "VerityIntroStage";
    public static final String REVEAL_STARTED = "VerityRevealStarted";
    public static final String REVEAL_COMPLETED = "VerityRevealCompleted";
    public static final String ENTITY_UUID = "VerityEntityUUID";
    public static final String GREETING_PLAYED = "VerityGreetingPlayed";
    public static final String GREETING_COMPLETED = "VerityGreetingCompleted";
    /** One-time whisper follow-up after the first accepted HELLO_VERITY intent. */
    public static final String HELLO_WHISPER_PLAYED = "VerityHelloWhisperPlayed";
    public static final String PROGRESSION_VERSION = "VerityProgressionVersion";
    public static final String PENDING_SPAWN = "VerityPendingSpawn";
    public static final String SPAWN_RETRY_COUNT = "VeritySpawnRetryCount";
    public static final int CURRENT_PROGRESSION_VERSION = 2;

    /** Quest 1 complete — Verity revealed and intro finished. */
    public static final String VERITY_REVEALED = "verity_revealed";
    public static final String VERITY_RELATIONSHIP = "verity_relationship";
    /** Quest 2 complete — player spoke hello to Verity. */
    public static final String VERITY_GREETED = "verity_greeted";
    public static final String VERITY_VOICE_KNOWLEDGE_QUESTIONED = "verity_voice_knowledge_questioned";
    public static final String VERITY_FAMILIARITY = "verity_familiarity";
    /** Bitmask of sealed-box waiting lines already played (Quest 1). */
    public static final String Q1_BOX_LINES_PLAYED = "VerityQ1BoxLinesPlayed";
    /** Bitmask of rare sealed-box pairs already played. */
    public static final String Q1_RARE_PAIRS_PLAYED = "VerityQ1RarePairsPlayed";
    public static final String Q1_QUEST_COMPLETE = "VerityQ1QuestComplete";
    public static final String Q2_QUEST_COMPLETE = "VerityQ2QuestComplete";
    /** Game time deadline for optional Q2 knowledge follow-up window. */
    public static final String Q2_FOLLOWUP_UNTIL = "VerityQ2FollowupUntil";
    public static final String Q2_LAST_REPEAT_GAME_TIME = "VerityQ2LastRepeatGameTime";
    /** Quest 3 complete — player finished make-a-sound quest. */
    public static final String VERITY_SOUND_QUEST_COMPLETE = "verity_sound_quest_complete";
    public static final String VERITY_MADE_SOUND = "verity_made_sound";
    public static final String Q3_QUEST_COMPLETE = "VerityQ3QuestComplete";
    public static final String Q3_LAST_RARE_REQUEST_GAME_TIME = "VerityQ3LastRareRequestGameTime";
    public static final String VERITY_HEARD_UNKNOWN_SOUND = "verity_heard_unknown_sound";

    private VerityIntroDataKeys() {
    }
}
