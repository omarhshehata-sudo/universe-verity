package com.universeexe.verity.voice;

/**
 * Persistent voice-memory flags stored in player NBT (survive reconnect).
 */
public enum VerityVoiceMemoryFlag {
    PLAYER_OPENED_BOX,
    PLAYER_FIRST_GREETING,
    PLAYER_GAVE_FLOWER,
    PLAYER_GAVE_COOKIE,
    PLAYER_THANKED_AFTER_HELP,
    PLAYER_DROPPED_VERITY,
    PLAYER_HIT_VERITY,
    PLAYER_ABANDONED_VERITY,
    PLAYER_RETURNED_AFTER_ABANDONMENT,
    PLAYER_SPAMMED_DIAMONDS,
    PLAYER_ASKED_FAVORITE_SONG,
    PLAYER_ASKED_IF_ANGRY,
    PLAYER_REACHED_HIGH_TRUST,
    PLAYER_REACHED_LOW_TRUST;

    public String nbtKey() {
        return "VoiceMem_" + name();
    }

    public String countKey() {
        return "VoiceMemCount_" + name();
    }

    public String lastKey() {
        return "VoiceMemLast_" + name();
    }

    public String callbackKey() {
        return "VoiceMemCallback_" + name();
    }
}
