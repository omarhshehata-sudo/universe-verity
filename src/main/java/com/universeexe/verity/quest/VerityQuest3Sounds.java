package com.universeexe.verity.quest;

/**
 * Quest 3 sound IDs and playback durations (ticks at 20 TPS), derived from source OGG lengths.
 */
public final class VerityQuest3Sounds {
    public static final int PAUSE_700MS = 14;
    public static final int PAUSE_800MS = 16;
    public static final int PAUSE_900MS = 18;
    public static final int PAUSE_1S = 20;
    public static final int PAUSE_1200MS = 24;
    public static final int PAUSE_1500MS = 30;

    public static final int CAVE_RESPONSE_WINDOW = 160;
    public static final int EVAL_RESPONSE_WINDOW = 200;
    public static final int REPEAT_REQUEST_RARE_COOLDOWN = 24000;

    public static final String INTRO_01 = "verity.q03.intro_01";
    public static final String INTRO_02 = "verity.q03.intro_02";
    public static final String BEEP_01 = "verity.q03.beep_01";
    public static final String BEEP_02 = "verity.q03.beep_02";
    public static final String CAT_01 = "verity.q03.cat_01";
    public static final String CAT_02 = "verity.q03.cat_02";
    public static final String CAT_DEFENSE_01 = "verity.q03.cat_defense_01";
    public static final String CAT_DEFENSE_02 = "verity.q03.cat_defense_02";
    public static final String CAVE_01 = "verity.q03.cave_01";
    public static final String CAVE_YES_01 = "verity.q03.cave_yes_01";
    public static final String CAVE_YES_02 = "verity.q03.cave_yes_02";
    public static final String CAVE_NO_01 = "verity.q03.cave_no_01";
    public static final String CAVE_SILENCE_01 = "verity.q03.cave_silence_01";
    public static final String BOOM_01 = "verity.q03.boom_01";
    public static final String SQUEAK_01 = "verity.q03.squeak_01";
    public static final String IMITATION_01 = "verity.q03.imitation_01";
    public static final String EVALUATION_01 = "verity.q03.evaluation_01";
    public static final String RESPONSE_POSITIVE_01 = "verity.q03.response_positive_01";
    public static final String RESPONSE_NEGATIVE_01 = "verity.q03.response_negative_01";
    public static final String RESPONSE_NEGATIVE_02 = "verity.q03.response_negative_02";
    public static final String RESPONSE_ANOTHER_01 = "verity.q03.response_another_01";
    public static final String RESPONSE_ANOTHER_02 = "verity.q03.response_another_02";
    public static final String REPEAT_REQUEST_01 = "verity.q03.repeat_request_01";
    public static final String REPEAT_REQUEST_02 = "verity.q03.repeat_request_02";
    public static final String REPEAT_REQUEST_03 = "verity.q03.repeat_request_03";
    public static final String REPEAT_REQUEST_RARE_01 = "verity.q03.repeat_request_rare_01";
    public static final String REPEAT_BEEP_01 = "verity.q03.repeat_beep_01";
    public static final String REPEAT_CAT_01 = "verity.q03.repeat_cat_01";
    public static final String REPEAT_CAVE_01 = "verity.q03.repeat_cave_01";
    public static final String REPEAT_CAVE_RARE_01 = "verity.q03.repeat_cave_rare_01";
    public static final String REPEAT_BOOM_01 = "verity.q03.repeat_boom_01";
    public static final String REPEAT_SQUEAK_01 = "verity.q03.repeat_squeak_01";
    public static final String REPEAT_IMITATION_01 = "verity.q03.repeat_imitation_01";
    public static final String SFX_TINY_BEEP = "verity.q03.sfx_tiny_beep";
    public static final String SFX_CAVE = "verity.q03.sfx_cave";
    public static final String SFX_BOOM = "verity.q03.sfx_boom";
    public static final String SFX_SQUEAK = "verity.q03.sfx_squeak";
    public static final String SFX_FAKE_IMITATION = "verity.q03.sfx_fake_imitation";

    private VerityQuest3Sounds() {
    }

    public static int durationTicks(String soundId) {
        return switch (soundId) {
            case INTRO_01 -> 19;
            case INTRO_02 -> 25;
            case BEEP_01 -> 30;
            case BEEP_02 -> 31;
            case CAT_01 -> 37;
            case CAT_DEFENSE_01 -> 35;
            case CAT_DEFENSE_02 -> 36;
            case CAVE_01 -> 34;
            case CAVE_YES_01 -> 16;
            case CAVE_YES_02 -> 34;
            case CAVE_NO_01 -> 35;
            case CAVE_SILENCE_01 -> 33;
            case BOOM_01 -> 35;
            case SQUEAK_01 -> 42;
            case EVALUATION_01 -> 24;
            case RESPONSE_POSITIVE_01 -> 32;
            case RESPONSE_NEGATIVE_01 -> 13;
            case RESPONSE_NEGATIVE_02 -> 27;
            case RESPONSE_ANOTHER_01 -> 41;
            case RESPONSE_ANOTHER_02 -> 34;
            case REPEAT_REQUEST_01 -> 21;
            case REPEAT_REQUEST_02 -> 32;
            case REPEAT_REQUEST_03 -> 29;
            case REPEAT_REQUEST_RARE_01 -> 34;
            case REPEAT_CAT_01 -> 15;
            case REPEAT_CAVE_01 -> 27;
            case REPEAT_CAVE_RARE_01 -> 36;
            case REPEAT_BOOM_01 -> 26;
            case REPEAT_SQUEAK_01 -> 38;
            case REPEAT_IMITATION_01 -> 43;
            case SFX_TINY_BEEP -> 3;
            case SFX_CAVE -> 74;
            case SFX_BOOM -> 28;
            case SFX_SQUEAK -> 5;
            case SFX_FAKE_IMITATION -> 15;
            default -> 20;
        };
    }

    public static String subtitleKey(String soundId) {
        if (soundId == null || !soundId.startsWith("verity.q03.")) {
            return "";
        }
        return "subtitles.universe_verity.q03." + soundId.substring("verity.q03.".length());
    }

    /** Source OGG missing from manifest — step skipped at runtime. */
    public static boolean isAssetMissing(String soundId) {
        return CAT_02.equals(soundId) || IMITATION_01.equals(soundId) || REPEAT_BEEP_01.equals(soundId);
    }
}
