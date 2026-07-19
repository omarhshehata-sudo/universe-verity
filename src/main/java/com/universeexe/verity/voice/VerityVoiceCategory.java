package com.universeexe.verity.voice;

/**
 * Logical voice categories mapped to {@link VerityVoicePriority} on the server.
 */
public enum VerityVoiceCategory {
    TRANSFORM,
    COUNTDOWN,
    QUEST,
    DANGER,
    COMMAND_SUCCESS,
    COMMAND_FAILURE,
    ACK,
    TRUST,
    MOOD,
    PLAYER_INTERACTION,
    ENV,
    MEMORY,
    IDLE,
    BOX_INTRO,
    MONSTER;

    public VerityVoicePriority priority() {
        return switch (this) {
            case TRANSFORM -> VerityVoicePriority.TRANSFORM;
            case COUNTDOWN -> VerityVoicePriority.COUNTDOWN;
            case QUEST, BOX_INTRO -> VerityVoicePriority.QUEST;
            case DANGER -> VerityVoicePriority.DANGER;
            case COMMAND_SUCCESS, COMMAND_FAILURE -> VerityVoicePriority.COMMAND;
            case ACK -> VerityVoicePriority.ACK;
            case TRUST, MOOD -> VerityVoicePriority.TRUST_MOOD;
            case PLAYER_INTERACTION -> VerityVoicePriority.PLAYER_INTERACTION;
            case ENV -> VerityVoicePriority.ENV;
            case MEMORY, MONSTER -> VerityVoicePriority.MEMORY;
            case IDLE -> VerityVoicePriority.IDLE;
        };
    }

    public static VerityVoiceCategory fromManifest(String raw) {
        if (raw == null || raw.isBlank()) {
            return PLAYER_INTERACTION;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PLAYER_INTERACTION;
        }
    }
}
