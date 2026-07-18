package com.universeexe.verity.entity;

public enum VerityBoxStage {
    WAITING_FOR_PLAYER,
    INITIAL_SILENCE,
    FIRST_MOVEMENT,
    FIRST_CALL,
    KNOCKING,
    SECOND_CALL,
    MORE_MOVEMENT,
    THIRD_CALL,
    FOURTH_CALL,
    FIFTH_CALL,
    IDLE_CALLING,
    INTRO_COMPLETE;

    public static VerityBoxStage fromName(String name) {
        if (name == null || name.isEmpty()) {
            return INITIAL_SILENCE;
        }
        try {
            return VerityBoxStage.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return INITIAL_SILENCE;
        }
    }
}
