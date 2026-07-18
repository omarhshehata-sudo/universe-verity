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
    public static final int CURRENT_PROGRESSION_VERSION = 1;

    private VerityIntroDataKeys() {
    }
}
