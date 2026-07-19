package com.universeexe.verity.voice;

/**
 * Server-side interrupt priority. Higher values may preempt lower ones.
 * Idle never interrupts. Countdown interrupts all except an active transform line.
 */
public enum VerityVoicePriority {
    TRANSFORM(100),
    COUNTDOWN(100),
    QUEST(90),
    DANGER(80),
    COMMAND(70),
    ACK(65),
    TRUST_MOOD(60),
    PLAYER_INTERACTION(50),
    ENV(45),
    MEMORY(40),
    IDLE(20);

    private final int level;

    VerityVoicePriority(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    /**
     * @return true when {@code incoming} may preempt {@code current}.
     */
    public static boolean canInterrupt(VerityVoiceCategory current, VerityVoiceCategory incoming) {
        if (incoming == VerityVoiceCategory.IDLE) {
            return false;
        }
        if (current == VerityVoiceCategory.TRANSFORM && incoming == VerityVoiceCategory.COUNTDOWN) {
            return false;
        }
        return incoming.priority().level() > current.priority().level();
    }
}
