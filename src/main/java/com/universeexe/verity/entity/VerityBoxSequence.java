package com.universeexe.verity.entity;

import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.RegistryObject;

/**
 * Server-authoritative sealed-box introduction timeline.
 * Timeline ticks advance only while no voice line is playing (20 ticks = 1 second).
 * Movement/shake SFX are silenced; visual agitation + voice lines remain.
 *
 * Fixed voice order while sealed:
 * 1 Hellooo → 2 Is someone out there → 3 I can hear you moving → 4 Could you open this
 * → (longer delay) 5 Please → (very long delay) 6 You're still there
 * Open handoff (Verity entity): Oh you found the opening → short pause → greeting.
 */
public final class VerityBoxSequence {
    /** Initial quiet before first sealed line. */
    public static final int TICK_FIRST_CALL = 40; // 2.0s

    // Approximate voice durations in ticks + small tail buffer (busy gate).
    public static final int DUR_HELLOOO = 26;
    public static final int DUR_IS_SOMEONE_OUT_THERE = 32;
    public static final int DUR_I_CAN_HEAR_YOU_MOVING = 30;
    public static final int DUR_COULD_YOU_OPEN_THIS = 26;
    public static final int DUR_PLEASE = 17;
    public static final int DUR_YOURE_STILL_THERE = 51;
    /** Clean open-line duration before post-reveal greeting delay. */
    public static final int DUR_OH_YOU_FOUND_THE_OPENING = 45;

    /** Legacy alias used by skip-intro helpers. */
    public static final int TICK_IDLE = 0;

    private VerityBoxSequence() {
    }

    public static void tick(VerityBoxEntity box) {
        if (!VerityCommonConfig.INITIAL_SEQUENCE_ENABLED.get()) {
            box.setIntroStage(VerityBoxStage.IDLE_CALLING);
            box.setIntroCompleted(true);
            box.resetIdleCooldown();
            return;
        }
        if (box.isIntroCompleted() || box.getRevealStage() != VerityRevealStage.SEALED) {
            return;
        }

        // Pause the timeline while a voice line plays so timed events are never skipped.
        if (box.isVoiceBusy()) {
            return;
        }

        int t = box.getStageTicks();
        box.setStageTicks(t + 1);

        if (t == 0) {
            box.setIntroStage(VerityBoxStage.INITIAL_SILENCE);
        }

        int pauseBetween = VerityCommonConfig.BOX_VOICE_PAUSE_TICKS.get();
        int pleaseDelay = VerityCommonConfig.BOX_PLEASE_DELAY_TICKS.get();
        int stillThereDelay = VerityCommonConfig.BOX_STILL_THERE_DELAY_TICKS.get();

        int tick1 = TICK_FIRST_CALL;
        int tick2 = tick1 + 1 + pauseBetween;
        int tick3 = tick2 + 1 + pauseBetween;
        int tick4 = tick3 + 1 + pauseBetween;
        int tickPlease = tick4 + 1 + pleaseDelay;
        int tickStillThere = tickPlease + 1 + stillThereDelay;
        int tickIdle = tickStillThere + 1;

        // Light visual agitation between lines (no shake SFX).
        if (t == tick1 - 12) {
            box.setIntroStage(VerityBoxStage.FIRST_MOVEMENT);
            box.triggerAnimation("rustle_small", true);
        } else if (t == tick2 - 10) {
            box.triggerAnimation("knock", true);
        } else if (t == tick3 - 10) {
            box.setIntroStage(VerityBoxStage.MORE_MOVEMENT);
            box.triggerAnimation("shift", true);
        } else if (t == tick4 - 10) {
            box.triggerAnimation("shake", true);
        } else if (t == tickPlease - 14) {
            box.triggerAnimation("rustle_small", true);
        } else if (t == tickStillThere - 16) {
            box.triggerAnimation("shake", true);
        }

        if (t == tick1) {
            box.setIntroStage(VerityBoxStage.FIRST_CALL);
            speak(box, VeritySounds.BOX_HELLOOO, DUR_HELLOOO, "hellooo");
        } else if (t == tick2) {
            box.setIntroStage(VerityBoxStage.SECOND_CALL);
            speak(box, VeritySounds.BOX_IS_SOMEONE_OUT_THERE, DUR_IS_SOMEONE_OUT_THERE, "is_someone_out_there");
        } else if (t == tick3) {
            box.setIntroStage(VerityBoxStage.THIRD_CALL);
            speak(box, VeritySounds.BOX_I_CAN_HEAR_YOU_MOVING, DUR_I_CAN_HEAR_YOU_MOVING, "i_can_hear_you_moving");
        } else if (t == tick4) {
            box.setIntroStage(VerityBoxStage.FOURTH_CALL);
            speak(box, VeritySounds.BOX_COULD_YOU_OPEN_THIS, DUR_COULD_YOU_OPEN_THIS, "could_you_open_this");
        } else if (t == tickPlease) {
            box.setIntroStage(VerityBoxStage.FIFTH_CALL);
            speak(box, VeritySounds.BOX_PLEASE, DUR_PLEASE, "please");
        } else if (t == tickStillThere) {
            box.setIntroStage(VerityBoxStage.IDLE_CALLING);
            speak(box, VeritySounds.BOX_YOURE_STILL_THERE, DUR_YOURE_STILL_THERE, "youre_still_there");
        }

        if (t >= tickIdle && !box.isIntroCompleted()) {
            box.setIntroStage(VerityBoxStage.IDLE_CALLING);
            box.setIntroCompleted(true);
            box.resetIdleCooldown();
        }
    }

    private static void speak(VerityBoxEntity box, RegistryObject<SoundEvent> sound, int busyTicks, String id) {
        if (!VerityCommonConfig.ENABLE_VOICE_LINES.get()) {
            return;
        }
        playVoice(box, sound, 0.82f, 0.98f);
        box.setVoiceBusyTicks(busyTicks);
        box.setLastVoiceLine(id);
    }

    private static void playVoice(VerityBoxEntity box, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        if (sound == null || sound.get() == null || box.level().isClientSide) {
            return;
        }
        float vol = Math.min(0.95f, Math.max(0.05f, volume));
        box.level().playSound(null, box.getX(), box.getY(), box.getZ(), sound.get(), SoundSource.BLOCKS, vol, pitch);
    }
}
