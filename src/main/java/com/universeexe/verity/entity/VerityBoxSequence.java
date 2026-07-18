package com.universeexe.verity.entity;

import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.RegistryObject;

/**
 * Server-authoritative sealed-box introduction timeline.
 * Times are in ticks from sequence start (20 ticks = 1 second).
 * Movement/shake SFX are silenced; visual agitation + voice lines remain.
 */
public final class VerityBoxSequence {
    public static final int TICK_FIRST_MOVEMENT = 30;   // 1.5s
    public static final int TICK_TINY_MOVE = 48;        // 2.4s
    public static final int TICK_FIRST_CALL = 66;       // 3.3s — Is anyone out there?
    public static final int TICK_QUIET_RUSTLE = 120;    // 6.0s
    public static final int TICK_KNOCKING = 140;        // 7.0s
    public static final int TICK_SHIFT = 200;           // 10.0s
    public static final int TICK_BREATH = 220;          // 11.0s
    public static final int TICK_SECOND_CALL = 250;     // 12.5s — Can you hear me?
    public static final int TICK_SMALL_SHAKE = 320;     // 16.0s
    public static final int TICK_THIRD_CALL = 360;      // 18.0s — Can someone let me out?
    public static final int TICK_IDLE = 420;            // 21.0s

    // Approximate voice durations in ticks + safety gap (tail silence before next SFX).
    public static final int DUR_ANYONE = 38;
    public static final int DUR_HEAR_ME = 34;
    public static final int DUR_IS_SOMEONE = 42;

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

        switch (t) {
            case TICK_FIRST_MOVEMENT -> {
                box.setIntroStage(VerityBoxStage.FIRST_MOVEMENT);
                box.triggerAnimation("rustle_small", true);
            }
            case TICK_TINY_MOVE -> box.triggerAnimation("knock", true);
            case TICK_FIRST_CALL -> {
                box.setIntroStage(VerityBoxStage.FIRST_CALL);
                speak(box, VeritySounds.BOX_ANYONE_OUT_THERE, DUR_ANYONE, "anyone_out_there");
            }
            case TICK_QUIET_RUSTLE -> box.triggerAnimation("rustle_small", true);
            case TICK_KNOCKING -> {
                box.setIntroStage(VerityBoxStage.KNOCKING);
                box.triggerAnimation("knock", true);
                box.scheduleKnockFollowup(8);
            }
            case TICK_SHIFT -> {
                box.setIntroStage(VerityBoxStage.MORE_MOVEMENT);
                box.triggerAnimation("shift", true);
            }
            case TICK_BREATH -> {
                // Visual-only breath beat (shake SFX removed).
            }
            case TICK_SECOND_CALL -> {
                box.setIntroStage(VerityBoxStage.SECOND_CALL);
                speak(box, VeritySounds.BOX_CAN_YOU_HEAR_ME, DUR_HEAR_ME, "can_you_hear_me");
            }
            case TICK_SMALL_SHAKE -> {
                box.triggerAnimation("shake", true);
            }
            case TICK_THIRD_CALL -> {
                box.setIntroStage(VerityBoxStage.THIRD_CALL);
                speak(box, VeritySounds.BOX_IS_SOMEONE_THERE, DUR_IS_SOMEONE, "is_someone_there");
            }
            default -> {
            }
        }

        if (t >= TICK_IDLE && !box.isIntroCompleted()) {
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
