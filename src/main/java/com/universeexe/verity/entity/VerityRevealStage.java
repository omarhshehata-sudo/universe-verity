package com.universeexe.verity.entity;

public enum VerityRevealStage {
    SEALED,
    REVEAL_STARTING,
    BOX_REACTING,
    BOX_OPENING,
    VERITY_SPAWNING,
    GREETING_DELAY,
    GREETING_PLAYING,
    REVEAL_COMPLETE,
    ERROR_RECOVERY;

    public static VerityRevealStage fromName(String name) {
        if (name == null || name.isEmpty()) {
            return SEALED;
        }
        try {
            return VerityRevealStage.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return SEALED;
        }
    }

    public boolean isRevealActive() {
        return this != SEALED && this != REVEAL_COMPLETE && this != ERROR_RECOVERY;
    }
}
