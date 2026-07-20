package com.universeexe.verity.entity;

/**
 * Server-side box-open emergence: fall → bounce → quest greeting (verity-5.7.3 JAR timing).
 */
public enum VerityIntroPhase {
    /** Normal gameplay — not in box-open cinematic. */
    NONE,
    /** Legacy save tag — treated as {@link #FALLING} on load. */
    FLOATING,
    /** Gravity drop from box lid height (JAR {@code triggerBoxDrop}). */
    FALLING,
    /** First landing; physics bounce in progress. */
    BOUNCING,
    /** Brief hurt face after the bounce settles (~1 s). */
    HURT_FACE,
    /** Quest 1 voice intro — personal helper monologue only (no oh/found_opening). */
    GREETING,
    /** Cinematic finished; mood-based idle face. */
    DONE;

    public static VerityIntroPhase fromName(String name) {
        if (name == null || name.isEmpty()) {
            return NONE;
        }
        try {
            return VerityIntroPhase.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }

    public boolean isBoxOpenCinematicActive() {
        return this != NONE && this != DONE;
    }
}
