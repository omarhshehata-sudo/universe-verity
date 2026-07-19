package com.universeexe.verity.registry;

import com.universeexe.verity.UniverseVerity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VeritySounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, UniverseVerity.MOD_ID);

    // Legacy box intro (kept for migration)
    public static final RegistryObject<SoundEvent> BOX_HELLOOO = register("verity.box.hellooo");
    public static final RegistryObject<SoundEvent> BOX_IS_SOMEONE_OUT_THERE = register("verity.box.is_someone_out_there");
    public static final RegistryObject<SoundEvent> BOX_I_CAN_HEAR_YOU_MOVING = register("verity.box.i_can_hear_you_moving");
    public static final RegistryObject<SoundEvent> BOX_COULD_YOU_OPEN_THIS = register("verity.box.could_you_open_this");
    public static final RegistryObject<SoundEvent> BOX_PLEASE = register("verity.box.please");
    public static final RegistryObject<SoundEvent> BOX_YOURE_STILL_THERE = register("verity.box.youre_still_there");
    public static final RegistryObject<SoundEvent> BOX_OH_YOU_FOUND_THE_OPENING = register("verity.box.oh_you_found_the_opening");

    // Quest 1 waiting (official IDs)
    public static final RegistryObject<SoundEvent> Q01_BOX_HELLO = register("verity.q01.box.hello");
    public static final RegistryObject<SoundEvent> Q01_BOX_ANYONE_THERE = register("verity.q01.box.anyone_there");
    public static final RegistryObject<SoundEvent> Q01_BOX_HEAR_MOVING = register("verity.q01.box.hear_moving");
    public static final RegistryObject<SoundEvent> Q01_BOX_OPEN_REQUEST = register("verity.q01.box.open_request");
    public static final RegistryObject<SoundEvent> Q01_BOX_PLEASE = register("verity.q01.box.please");
    public static final RegistryObject<SoundEvent> Q01_BOX_STILL_THERE_01 = register("verity.q01.box.still_there_01");
    public static final RegistryObject<SoundEvent> Q01_BOX_STILL_THERE_02 = register("verity.q01.box.still_there_02");
    public static final RegistryObject<SoundEvent> Q01_BOX_OPEN_MYSELF_01 = register("verity.q01.box.open_myself_01");
    public static final RegistryObject<SoundEvent> Q01_BOX_OPEN_MYSELF_02 = register("verity.q01.box.open_myself_02");

    // Quest 1 reveal + intro
    public static final RegistryObject<SoundEvent> Q01_REVEAL_OH = register("verity.q01.reveal.oh");
    public static final RegistryObject<SoundEvent> Q01_REVEAL_FOUND_OPENING = register("verity.q01.reveal.found_opening");
    public static final RegistryObject<SoundEvent> Q01_INTRO_HELLO = register("verity.q01.intro.hello");
    public static final RegistryObject<SoundEvent> Q01_INTRO_NAME = register("verity.q01.intro.name");
    public static final RegistryObject<SoundEvent> Q01_INTRO_HELPER_FRIEND = register("verity.q01.intro.helper_friend");
    public static final RegistryObject<SoundEvent> Q01_INTRO_ASK_ANYTHING = register("verity.q01.intro.ask_anything");
    public static final RegistryObject<SoundEvent> Q01_INTRO_KNOW_EVERYTHING = register("verity.q01.intro.know_everything");
    public static final RegistryObject<SoundEvent> Q01_INTRO_ALMOST_EVERYTHING = register("verity.q01.intro.almost_everything");
    public static final RegistryObject<SoundEvent> Q01_INTRO_EVERYTHING_IMPORTANT = register("verity.q01.intro.everything_important");

    // Quest 2
    public static final RegistryObject<SoundEvent> Q02_FIRST_HELLO = register("verity.q02.first.hello");
    public static final RegistryObject<SoundEvent> Q02_FIRST_HOPING = register("verity.q02.first.hoping");
    public static final RegistryObject<SoundEvent> Q02_FIRST_IMAGINED_VOICE = register("verity.q02.first.imagined_voice");
    public static final RegistryObject<SoundEvent> Q02_FIRST_DIDNT_IMAGINE = register("verity.q02.first.didnt_imagine");
    public static final RegistryObject<SoundEvent> Q02_FIRST_ALREADY_KNEW = register("verity.q02.first.already_knew");
    public static final RegistryObject<SoundEvent> Q02_FIRST_NICE_TO_MEET = register("verity.q02.first.nice_to_meet");
    public static final RegistryObject<SoundEvent> Q02_FOLLOWUP_KNOW_WHAT = register("verity.q02.followup.know_what");
    public static final RegistryObject<SoundEvent> Q02_FOLLOWUP_DONT_REMEMBER = register("verity.q02.followup.dont_remember");
    public static final RegistryObject<SoundEvent> Q02_FOLLOWUP_DID_I = register("verity.q02.followup.did_i");
    public static final RegistryObject<SoundEvent> Q02_REPEAT_HELLO_AGAIN = register("verity.q02.repeat.hello_again");
    public static final RegistryObject<SoundEvent> Q02_REPEAT_HI = register("verity.q02.repeat.hi");
    public static final RegistryObject<SoundEvent> Q02_REPEAT_NO_INTRO_01 = register("verity.q02.repeat.no_introduction_01");
    public static final RegistryObject<SoundEvent> Q02_REPEAT_NO_INTRO_02 = register("verity.q02.repeat.no_introduction_02");
    public static final RegistryObject<SoundEvent> Q02_REPEAT_LIKES_NAME = register("verity.q02.repeat.likes_name");
    public static final RegistryObject<SoundEvent> Q02_REPEAT_HAPPIER = register("verity.q02.repeat.happier");

    // Quest 3 — MAKE A SOUND
    public static final RegistryObject<SoundEvent> Q03_INTRO_01 = register("verity.q03.intro_01");
    public static final RegistryObject<SoundEvent> Q03_INTRO_02 = register("verity.q03.intro_02");
    public static final RegistryObject<SoundEvent> Q03_BEEP_01 = register("verity.q03.beep_01");
    public static final RegistryObject<SoundEvent> Q03_BEEP_02 = register("verity.q03.beep_02");
    public static final RegistryObject<SoundEvent> Q03_CAT_01 = register("verity.q03.cat_01");
    public static final RegistryObject<SoundEvent> Q03_CAT_DEFENSE_01 = register("verity.q03.cat_defense_01");
    public static final RegistryObject<SoundEvent> Q03_CAT_DEFENSE_02 = register("verity.q03.cat_defense_02");
    public static final RegistryObject<SoundEvent> Q03_CAVE_01 = register("verity.q03.cave_01");
    public static final RegistryObject<SoundEvent> Q03_CAVE_YES_01 = register("verity.q03.cave_yes_01");
    public static final RegistryObject<SoundEvent> Q03_CAVE_YES_02 = register("verity.q03.cave_yes_02");
    public static final RegistryObject<SoundEvent> Q03_CAVE_NO_01 = register("verity.q03.cave_no_01");
    public static final RegistryObject<SoundEvent> Q03_CAVE_SILENCE_01 = register("verity.q03.cave_silence_01");
    public static final RegistryObject<SoundEvent> Q03_BOOM_01 = register("verity.q03.boom_01");
    public static final RegistryObject<SoundEvent> Q03_SQUEAK_01 = register("verity.q03.squeak_01");
    public static final RegistryObject<SoundEvent> Q03_EVALUATION_01 = register("verity.q03.evaluation_01");
    public static final RegistryObject<SoundEvent> Q03_RESPONSE_POSITIVE_01 = register("verity.q03.response_positive_01");
    public static final RegistryObject<SoundEvent> Q03_RESPONSE_NEGATIVE_01 = register("verity.q03.response_negative_01");
    public static final RegistryObject<SoundEvent> Q03_RESPONSE_NEGATIVE_02 = register("verity.q03.response_negative_02");
    public static final RegistryObject<SoundEvent> Q03_RESPONSE_ANOTHER_01 = register("verity.q03.response_another_01");
    public static final RegistryObject<SoundEvent> Q03_RESPONSE_ANOTHER_02 = register("verity.q03.response_another_02");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_REQUEST_01 = register("verity.q03.repeat_request_01");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_REQUEST_02 = register("verity.q03.repeat_request_02");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_REQUEST_03 = register("verity.q03.repeat_request_03");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_REQUEST_RARE_01 = register("verity.q03.repeat_request_rare_01");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_CAT_01 = register("verity.q03.repeat_cat_01");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_CAVE_01 = register("verity.q03.repeat_cave_01");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_CAVE_RARE_01 = register("verity.q03.repeat_cave_rare_01");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_BOOM_01 = register("verity.q03.repeat_boom_01");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_SQUEAK_01 = register("verity.q03.repeat_squeak_01");
    public static final RegistryObject<SoundEvent> Q03_REPEAT_IMITATION_01 = register("verity.q03.repeat_imitation_01");
    public static final RegistryObject<SoundEvent> Q03_SFX_TINY_BEEP = register("verity.q03.sfx_tiny_beep");
    public static final RegistryObject<SoundEvent> Q03_SFX_CAVE = register("verity.q03.sfx_cave");
    public static final RegistryObject<SoundEvent> Q03_SFX_BOOM = register("verity.q03.sfx_boom");
    public static final RegistryObject<SoundEvent> Q03_SFX_SQUEAK = register("verity.q03.sfx_squeak");
    public static final RegistryObject<SoundEvent> Q03_SFX_FAKE_IMITATION = register("verity.q03.sfx_fake_imitation");

    public static final RegistryObject<SoundEvent> BOX_RUSTLE_1 = register("verity.box.rustle_1");
    public static final RegistryObject<SoundEvent> BOX_RUSTLE_2 = register("verity.box.rustle_2");
    public static final RegistryObject<SoundEvent> BOX_KNOCK_1 = register("verity.box.knock_1");
    public static final RegistryObject<SoundEvent> BOX_KNOCK_2 = register("verity.box.knock_2");
    public static final RegistryObject<SoundEvent> BOX_SHIFT = register("verity.box.shift");
    public static final RegistryObject<SoundEvent> BOX_THUMP = register("verity.box.thump");
    public static final RegistryObject<SoundEvent> REVEAL_BOX_OPEN = register("verity.reveal.box_open");
    public static final RegistryObject<SoundEvent> REVEAL_MOVEMENT = register("verity.reveal.movement");
    public static final RegistryObject<SoundEvent> GREETING_PERSONAL_HELPER = register("verity.greeting.personal_helper");
    public static final RegistryObject<SoundEvent> VOICE_OKAY_WHERE_ARE_WE_GOING = register("verity.voice.okay_where_are_we_going");
    public static final RegistryObject<SoundEvent> VOICE_ALRIGHT_RIGHT_BEHIND_YOU = register("verity.voice.alright_right_behind_you");
    public static final RegistryObject<SoundEvent> VOICE_LEAD_THE_WAY = register("verity.voice.lead_the_way");
    public static final RegistryObject<SoundEvent> VOICE_HELLO_HOPING_YOU_WOULD_TALK = register("verity.voice.hello_hoping_you_would_talk");
    public static final RegistryObject<SoundEvent> VOICE_HELLO_AGAIN = register("verity.voice.hello_again");
    public static final RegistryObject<SoundEvent> VOICE_YOUR_VOICE_SOUNDS_EXACTLY = register("verity.voice.your_voice_sounds_exactly");
    public static final RegistryObject<SoundEvent> VOICE_I_MEAN_IMAGINED_IT = register("verity.voice.i_mean_imagined_it");
    public static final RegistryObject<SoundEvent> INTRO_VIDEO_AUDIO = register("verity.intro.video_audio");

    public static final RegistryObject<SoundEvent> TRUST_HAPPY_A = register("verity.trust.happy_a");
    public static final RegistryObject<SoundEvent> TRUST_HAPPY_B = register("verity.trust.happy_b");
    public static final RegistryObject<SoundEvent> TRUST_FRIENDLY_A = register("verity.trust.friendly_a");
    public static final RegistryObject<SoundEvent> TRUST_FRIENDLY_B = register("verity.trust.friendly_b");
    public static final RegistryObject<SoundEvent> TRUST_MEH_A = register("verity.trust.meh_a");
    public static final RegistryObject<SoundEvent> TRUST_MEH_B = register("verity.trust.meh_b");
    public static final RegistryObject<SoundEvent> TRUST_ANGRY_A = register("verity.trust.angry_a");
    public static final RegistryObject<SoundEvent> TRUST_ANGRY_B = register("verity.trust.angry_b");
    public static final RegistryObject<SoundEvent> TRUST_FURIOUS_A = register("verity.trust.furious_a");
    public static final RegistryObject<SoundEvent> TRUST_FURIOUS_B = register("verity.trust.furious_b");

    public static final RegistryObject<SoundEvent> COUNTDOWN_WARNING_01 = register("verity.countdown.warning_01");
    public static final RegistryObject<SoundEvent> COUNTDOWN_WARNING_02 = register("verity.countdown.warning_02");
    public static final RegistryObject<SoundEvent> COUNTDOWN_WARNING_03 = register("verity.countdown.warning_03");
    public static final RegistryObject<SoundEvent> COUNTDOWN_TWO_DAYS = register("verity.countdown.two_days");
    public static final RegistryObject<SoundEvent> COUNTDOWN_ONE_DAY = register("verity.countdown.one_day");
    public static final RegistryObject<SoundEvent> COUNTDOWN_ARRIVAL = register("verity.countdown.arrival");
    public static final RegistryObject<SoundEvent> COUNTDOWN_TRANSFORM = register("verity.countdown.transform");

    public static final RegistryObject<SoundEvent> MONSTER_REMEMBER = register("verity.monster.remember");
    public static final RegistryObject<SoundEvent> MONSTER_LISTENED = register("verity.monster.listened");
    public static final RegistryObject<SoundEvent> MONSTER_KNOW_WHERE = register("verity.monster.know_where");

    private static final Map<String, RegistryObject<SoundEvent>> BY_ID = new LinkedHashMap<>();

    static {
        registerAll(
                "verity.box.hellooo", BOX_HELLOOO,
                "verity.box.is_someone_out_there", BOX_IS_SOMEONE_OUT_THERE,
                "verity.box.i_can_hear_you_moving", BOX_I_CAN_HEAR_YOU_MOVING,
                "verity.box.could_you_open_this", BOX_COULD_YOU_OPEN_THIS,
                "verity.box.please", BOX_PLEASE,
                "verity.box.youre_still_there", BOX_YOURE_STILL_THERE,
                "verity.box.oh_you_found_the_opening", BOX_OH_YOU_FOUND_THE_OPENING,
                "verity.q01.box.hello", Q01_BOX_HELLO,
                "verity.q01.box.anyone_there", Q01_BOX_ANYONE_THERE,
                "verity.q01.box.hear_moving", Q01_BOX_HEAR_MOVING,
                "verity.q01.box.open_request", Q01_BOX_OPEN_REQUEST,
                "verity.q01.box.please", Q01_BOX_PLEASE,
                "verity.q01.box.still_there_01", Q01_BOX_STILL_THERE_01,
                "verity.q01.box.still_there_02", Q01_BOX_STILL_THERE_02,
                "verity.q01.box.open_myself_01", Q01_BOX_OPEN_MYSELF_01,
                "verity.q01.box.open_myself_02", Q01_BOX_OPEN_MYSELF_02,
                "verity.q01.reveal.oh", Q01_REVEAL_OH,
                "verity.q01.reveal.found_opening", Q01_REVEAL_FOUND_OPENING,
                "verity.q01.intro.hello", Q01_INTRO_HELLO,
                "verity.q01.intro.name", Q01_INTRO_NAME,
                "verity.q01.intro.helper_friend", Q01_INTRO_HELPER_FRIEND,
                "verity.q01.intro.ask_anything", Q01_INTRO_ASK_ANYTHING,
                "verity.q01.intro.know_everything", Q01_INTRO_KNOW_EVERYTHING,
                "verity.q01.intro.almost_everything", Q01_INTRO_ALMOST_EVERYTHING,
                "verity.q01.intro.everything_important", Q01_INTRO_EVERYTHING_IMPORTANT,
                "verity.q02.first.hello", Q02_FIRST_HELLO,
                "verity.q02.first.hoping", Q02_FIRST_HOPING,
                "verity.q02.first.imagined_voice", Q02_FIRST_IMAGINED_VOICE,
                "verity.q02.first.didnt_imagine", Q02_FIRST_DIDNT_IMAGINE,
                "verity.q02.first.already_knew", Q02_FIRST_ALREADY_KNEW,
                "verity.q02.first.nice_to_meet", Q02_FIRST_NICE_TO_MEET,
                "verity.q02.followup.know_what", Q02_FOLLOWUP_KNOW_WHAT,
                "verity.q02.followup.dont_remember", Q02_FOLLOWUP_DONT_REMEMBER,
                "verity.q02.followup.did_i", Q02_FOLLOWUP_DID_I,
                "verity.q02.repeat.hello_again", Q02_REPEAT_HELLO_AGAIN,
                "verity.q02.repeat.hi", Q02_REPEAT_HI,
                "verity.q02.repeat.no_introduction_01", Q02_REPEAT_NO_INTRO_01,
                "verity.q02.repeat.no_introduction_02", Q02_REPEAT_NO_INTRO_02,
                "verity.q02.repeat.likes_name", Q02_REPEAT_LIKES_NAME,
                "verity.q02.repeat.happier", Q02_REPEAT_HAPPIER,
                "verity.q03.intro_01", Q03_INTRO_01,
                "verity.q03.intro_02", Q03_INTRO_02,
                "verity.q03.beep_01", Q03_BEEP_01,
                "verity.q03.beep_02", Q03_BEEP_02,
                "verity.q03.cat_01", Q03_CAT_01,
                "verity.q03.cat_defense_01", Q03_CAT_DEFENSE_01,
                "verity.q03.cat_defense_02", Q03_CAT_DEFENSE_02,
                "verity.q03.cave_01", Q03_CAVE_01,
                "verity.q03.cave_yes_01", Q03_CAVE_YES_01,
                "verity.q03.cave_yes_02", Q03_CAVE_YES_02,
                "verity.q03.cave_no_01", Q03_CAVE_NO_01,
                "verity.q03.cave_silence_01", Q03_CAVE_SILENCE_01,
                "verity.q03.boom_01", Q03_BOOM_01,
                "verity.q03.squeak_01", Q03_SQUEAK_01,
                "verity.q03.evaluation_01", Q03_EVALUATION_01,
                "verity.q03.response_positive_01", Q03_RESPONSE_POSITIVE_01,
                "verity.q03.response_negative_01", Q03_RESPONSE_NEGATIVE_01,
                "verity.q03.response_negative_02", Q03_RESPONSE_NEGATIVE_02,
                "verity.q03.response_another_01", Q03_RESPONSE_ANOTHER_01,
                "verity.q03.response_another_02", Q03_RESPONSE_ANOTHER_02,
                "verity.q03.repeat_request_01", Q03_REPEAT_REQUEST_01,
                "verity.q03.repeat_request_02", Q03_REPEAT_REQUEST_02,
                "verity.q03.repeat_request_03", Q03_REPEAT_REQUEST_03,
                "verity.q03.repeat_request_rare_01", Q03_REPEAT_REQUEST_RARE_01,
                "verity.q03.repeat_cat_01", Q03_REPEAT_CAT_01,
                "verity.q03.repeat_cave_01", Q03_REPEAT_CAVE_01,
                "verity.q03.repeat_cave_rare_01", Q03_REPEAT_CAVE_RARE_01,
                "verity.q03.repeat_boom_01", Q03_REPEAT_BOOM_01,
                "verity.q03.repeat_squeak_01", Q03_REPEAT_SQUEAK_01,
                "verity.q03.repeat_imitation_01", Q03_REPEAT_IMITATION_01,
                "verity.q03.sfx_tiny_beep", Q03_SFX_TINY_BEEP,
                "verity.q03.sfx_cave", Q03_SFX_CAVE,
                "verity.q03.sfx_boom", Q03_SFX_BOOM,
                "verity.q03.sfx_squeak", Q03_SFX_SQUEAK,
                "verity.q03.sfx_fake_imitation", Q03_SFX_FAKE_IMITATION,
                "verity.box.rustle_1", BOX_RUSTLE_1,
                "verity.box.rustle_2", BOX_RUSTLE_2,
                "verity.box.knock_1", BOX_KNOCK_1,
                "verity.box.knock_2", BOX_KNOCK_2,
                "verity.box.shift", BOX_SHIFT,
                "verity.box.thump", BOX_THUMP,
                "verity.reveal.box_open", REVEAL_BOX_OPEN,
                "verity.reveal.movement", REVEAL_MOVEMENT,
                "verity.greeting.personal_helper", GREETING_PERSONAL_HELPER,
                "verity.voice.okay_where_are_we_going", VOICE_OKAY_WHERE_ARE_WE_GOING,
                "verity.voice.alright_right_behind_you", VOICE_ALRIGHT_RIGHT_BEHIND_YOU,
                "verity.voice.lead_the_way", VOICE_LEAD_THE_WAY,
                "verity.voice.hello_hoping_you_would_talk", VOICE_HELLO_HOPING_YOU_WOULD_TALK,
                "verity.voice.hello_again", VOICE_HELLO_AGAIN,
                "verity.voice.your_voice_sounds_exactly", VOICE_YOUR_VOICE_SOUNDS_EXACTLY,
                "verity.voice.i_mean_imagined_it", VOICE_I_MEAN_IMAGINED_IT,
                "verity.intro.video_audio", INTRO_VIDEO_AUDIO,
                "verity.trust.happy_a", TRUST_HAPPY_A,
                "verity.trust.happy_b", TRUST_HAPPY_B,
                "verity.trust.friendly_a", TRUST_FRIENDLY_A,
                "verity.trust.friendly_b", TRUST_FRIENDLY_B,
                "verity.trust.meh_a", TRUST_MEH_A,
                "verity.trust.meh_b", TRUST_MEH_B,
                "verity.trust.angry_a", TRUST_ANGRY_A,
                "verity.trust.angry_b", TRUST_ANGRY_B,
                "verity.trust.furious_a", TRUST_FURIOUS_A,
                "verity.trust.furious_b", TRUST_FURIOUS_B,
                "verity.countdown.warning_01", COUNTDOWN_WARNING_01,
                "verity.countdown.warning_02", COUNTDOWN_WARNING_02,
                "verity.countdown.warning_03", COUNTDOWN_WARNING_03,
                "verity.countdown.two_days", COUNTDOWN_TWO_DAYS,
                "verity.countdown.one_day", COUNTDOWN_ONE_DAY,
                "verity.countdown.arrival", COUNTDOWN_ARRIVAL,
                "verity.countdown.transform", COUNTDOWN_TRANSFORM,
                "verity.monster.remember", MONSTER_REMEMBER,
                "verity.monster.listened", MONSTER_LISTENED,
                "verity.monster.know_where", MONSTER_KNOW_WHERE
        );
    }

    private VeritySounds() {
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(UniverseVerity.MOD_ID, id)));
    }

    @SafeVarargs
    private static void registerAll(Object... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            BY_ID.put((String) pairs[i], (RegistryObject<SoundEvent>) pairs[i + 1]);
        }
    }

    public static Map<String, RegistryObject<SoundEvent>> all() {
        return BY_ID;
    }

    public static RegistryObject<SoundEvent> byId(String id) {
        return BY_ID.get(id);
    }

    @javax.annotation.Nullable
    public static String resolveId(SoundEvent sound) {
        if (sound == null) {
            return null;
        }
        for (Map.Entry<String, RegistryObject<SoundEvent>> entry : BY_ID.entrySet()) {
            if (entry.getValue().isPresent() && entry.getValue().get() == sound) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String subtitleKeyFor(String soundId) {
        if (soundId == null || !soundId.startsWith("verity.")) {
            return "";
        }
        return "subtitles.universe_verity." + soundId.substring("verity.".length());
    }
}
