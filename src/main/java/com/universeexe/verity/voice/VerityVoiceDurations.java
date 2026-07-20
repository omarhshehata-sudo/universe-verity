package com.universeexe.verity.voice;

/**
 * OGG clip lengths measured with ffprobe ({@code ceil(seconds * 20) + padding}).
 */
public final class VerityVoiceDurations {
    private static final int PAD = 5;

    public static final int GREETING_PERSONAL_HELPER = ticks(5.866667);
    public static final int Q02_FIRST_HELLO = ticks(2.664490);
    public static final int Q02_FIRST_HOPING = ticks(2.664490);
    public static final int Q02_FIRST_IMAGINED_VOICE = ticks(2.664490);
    public static final int Q02_FIRST_DIDNT_IMAGINE = ticks(1.724082);
    public static final int Q02_FIRST_ALREADY_KNEW = ticks(1.044898);
    public static final int Q02_FIRST_NICE_TO_MEET = ticks(2.664490);
    public static final int Q02_REPEAT_HELLO_AGAIN = ticks(1.044898);
    public static final int Q02_REPEAT_HI = ticks(1.044898);
    public static final int Q02_REPEAT_NO_INTRO_01 = ticks(2.664490);
    public static final int Q02_REPEAT_NO_INTRO_02 = ticks(2.664490);
    public static final int Q02_REPEAT_LIKES_NAME = ticks(2.664490);
    public static final int Q02_REPEAT_HAPPIER = ticks(2.664490);

    /** Approximate total for quest_02_first_greeting (6 lines + pauses). */
    public static final int Q02_FIRST_GREETING_TOTAL = 420;

    private VerityVoiceDurations() {
    }

    public static int ticks(double seconds) {
        return (int) Math.ceil(seconds * 20.0) + PAD;
    }
}
