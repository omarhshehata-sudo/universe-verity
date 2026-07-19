package com.universeexe.verity.entity;

/**
 * Server-side box-open emergence: float → fall → bounce → brief hurt → quest greeting.
 */
public enum VerityIntroPhase {
    /** Normal gameplay — not in box-open cinematic. */
    NONE,
    /** Hover above the box opening while the lid opens. */
    FLOATING,
    /** Gravity enabled; falling to the ground beside the box. */
    FALLING,
    /** First landing; physics bounce in progress. */
    BOUNCING,
    /** Brief hurt face after the bounce settles (~1 s). */
    HURT_FACE,
    /** Quest 1 voice intro (oh / found opening / personal helper). */
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
