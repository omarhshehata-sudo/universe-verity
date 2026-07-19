package com.universeexe.verity.trust;

import javax.annotation.Nullable;

/**
 * Server-derived mood from trust. Clients only render the synced mood; they never compute trust.
 */
public enum MoodState {
    HAPPY,
    FRIENDLY,
    MEH,
    ANGRY,
    FURIOUS,
    CRITICAL,
    MONSTER;

    public static MoodState fromTrust(int trust) {
        if (trust >= 4) {
            return HAPPY;
        }
        if (trust >= 1) {
            return FRIENDLY;
        }
        if (trust >= -2) {
            return MEH;
        }
        if (trust >= -5) {
            return ANGRY;
        }
        if (trust >= -7) {
            return FURIOUS;
        }
        return CRITICAL;
    }

    public String faceTextureName() {
        return switch (this) {
            case HAPPY -> "face_happy";
            case FRIENDLY -> "face_friendly";
            case MEH -> "face_meh";
            case ANGRY -> "face_angry";
            case FURIOUS -> "face_furious";
            case CRITICAL -> "face_critical";
            case MONSTER -> "face_monster";
        };
    }

    /**
     * Real verity-5.7.3 sphere face PNG (without {@code .png}) for synced trust mood.
     * Client resolves {@code auto} face variant from {@link #DATA_MOOD} via this mapping.
     */
    public String legacyFaceVariant() {
        return switch (this) {
            case HAPPY -> "happy";
            case FRIENDLY -> "happy";
            case MEH -> "neutral";
            case ANGRY -> "smiling_evil";
            case FURIOUS -> "evil";
            case CRITICAL -> "crazy";
            case MONSTER -> "verity_demon";
        };
    }

    public static MoodState fromName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return MEH;
        }
        try {
            return MoodState.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEH;
        }
    }
}
