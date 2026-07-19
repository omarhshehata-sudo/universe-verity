package com.universeexe.verity.entity;

import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.voice.VerityVoiceCategory;
import com.universeexe.verity.voice.VerityVoiceContext;
import com.universeexe.verity.voice.VerityVoiceDirector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;

/**
 * Server-authoritative sealed-box waiting dialogue for Quest 1.
 * Advances only while the owner is within {@link #ACTIVATION_RANGE} blocks and no line is playing.
 */
public final class VerityBoxSequence {
    public static final int ACTIVATION_RANGE = 8;

    public static final int TICK_FIRST_CALL = 40;
    public static final int DUR_HELLO = 26;
    public static final int DUR_ANYONE_THERE = 32;
    public static final int DUR_HEAR_MOVING = 30;
    public static final int DUR_OPEN_REQUEST = 26;
    public static final int DUR_PLEASE = 17;
    public static final int DUR_STILL_THERE = 51;
    public static final int DUR_OPEN_MYSELF = 40;
    /** Legacy alias used by skip-intro helpers. */
    public static final int TICK_IDLE = 0;
    /** Clean reveal line duration. */
    public static final int DUR_OH_YOU_FOUND_THE_OPENING = 45;

    public static final int LINE_HELLO = 1;
    public static final int LINE_ANYONE = 2;
    public static final int LINE_HEAR_MOVING = 4;
    public static final int LINE_OPEN_REQUEST = 8;
    public static final int LINE_PLEASE = 16;
    public static final int RARE_STILL_01 = 32;
    public static final int RARE_STILL_02 = 64;
    public static final int RARE_OPEN_MYSELF_01 = 128;
    public static final int RARE_OPEN_MYSELF_02 = 256;

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
        if (box.isWaitingDialogueStopped()) {
            return;
        }
        if (box.isVoiceBusy()) {
            return;
        }

        ServerPlayer owner = box.findOwner();
        if (owner == null || owner.distanceTo(box) > ACTIVATION_RANGE) {
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

        scheduleAgitation(box, t, pauseBetween, pleaseDelay, stillThereDelay);
        tryPlayNextLine(box, owner, t, pauseBetween, pleaseDelay, stillThereDelay);

        if (box.isIntroCompleted()) {
            box.setIntroStage(VerityBoxStage.IDLE_CALLING);
            box.resetIdleCooldown();
        }
    }

    public static void stopWaitingDialogue(VerityBoxEntity box, ServerPlayer owner) {
        box.setWaitingDialogueStopped(true);
        box.setVoiceBusyTicks(0);
        if (owner != null) {
            VerityVoiceDirector.interrupt(owner, VerityVoiceCategory.QUEST);
            VerityVoiceDirector.interrupt(owner, VerityVoiceCategory.BOX_INTRO);
        }
    }

    private static void scheduleAgitation(
            VerityBoxEntity box,
            int t,
            int pauseBetween,
            int pleaseDelay,
            int stillThereDelay
    ) {
        int tick1 = TICK_FIRST_CALL;
        int tick2 = tick1 + DUR_HELLO + pauseBetween;
        int tick3 = tick2 + DUR_ANYONE_THERE + pauseBetween;
        int tick4 = tick3 + DUR_HEAR_MOVING + pauseBetween;
        int tickPlease = tick4 + DUR_OPEN_REQUEST + pleaseDelay;
        int tickRare = tickPlease + DUR_PLEASE + stillThereDelay;

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
        } else if (t == tickRare - 16) {
            box.triggerAnimation("shake", true);
        }
    }

    private static void tryPlayNextLine(
            VerityBoxEntity box,
            ServerPlayer owner,
            int t,
            int pauseBetween,
            int pleaseDelay,
            int stillThereDelay
    ) {
        int tick1 = TICK_FIRST_CALL;
        int tick2 = tick1 + DUR_HELLO + pauseBetween;
        int tick3 = tick2 + DUR_ANYONE_THERE + pauseBetween;
        int tick4 = tick3 + DUR_HEAR_MOVING + pauseBetween;
        int tickPlease = tick4 + DUR_OPEN_REQUEST + pleaseDelay;
        int tickRare = tickPlease + DUR_PLEASE + stillThereDelay;

        if (t == tick1 && tryMainLine(box, owner, LINE_HELLO, VeritySounds.Q01_BOX_HELLO, DUR_HELLO, "hello")) {
            return;
        }
        if (t == tick2 && tryMainLine(box, owner, LINE_ANYONE, VeritySounds.Q01_BOX_ANYONE_THERE, DUR_ANYONE_THERE, "anyone_there")) {
            return;
        }
        if (t == tick3 && tryMainLine(box, owner, LINE_HEAR_MOVING, VeritySounds.Q01_BOX_HEAR_MOVING, DUR_HEAR_MOVING, "hear_moving")) {
            return;
        }
        if (t == tick4 && tryMainLine(box, owner, LINE_OPEN_REQUEST, VeritySounds.Q01_BOX_OPEN_REQUEST, DUR_OPEN_REQUEST, "open_request")) {
            return;
        }
        if (t == tickPlease && tryMainLine(box, owner, LINE_PLEASE, VeritySounds.Q01_BOX_PLEASE, DUR_PLEASE, "please")) {
            return;
        }
        if (t >= tickRare && !box.isIntroCompleted()) {
            if (tryRareLine(box, owner)) {
                return;
            }
            box.setIntroCompleted(true);
        }
    }

    private static boolean tryMainLine(
            VerityBoxEntity box,
            ServerPlayer owner,
            int bit,
            RegistryObject<SoundEvent> sound,
            int busyTicks,
            String id
    ) {
        if (VerityPlayerData.hasQ1BoxLinePlayed(owner, bit)) {
            return false;
        }
        if (speak(box, owner, sound, busyTicks, id)) {
            VerityPlayerData.markQ1BoxLinePlayed(owner, bit);
            return true;
        }
        return false;
    }

    private static boolean tryRareLine(VerityBoxEntity box, ServerPlayer owner) {
        int roll = box.nextBoxRandomInt(100);
        if (roll >= 18) {
            return false;
        }
        int choice = box.nextBoxRandomInt(4);
        return switch (choice) {
            case 0 -> tryRarePair(box, owner, RARE_STILL_01, VeritySounds.Q01_BOX_STILL_THERE_01, DUR_STILL_THERE, "still_there_01");
            case 1 -> tryRarePair(box, owner, RARE_STILL_02, VeritySounds.Q01_BOX_STILL_THERE_02, DUR_STILL_THERE, "still_there_02");
            case 2 -> tryRarePair(box, owner, RARE_OPEN_MYSELF_01, VeritySounds.Q01_BOX_OPEN_MYSELF_01, DUR_OPEN_MYSELF, "open_myself_01");
            default -> tryRarePair(box, owner, RARE_OPEN_MYSELF_02, VeritySounds.Q01_BOX_OPEN_MYSELF_02, DUR_OPEN_MYSELF, "open_myself_02");
        };
    }

    private static boolean tryRarePair(
            VerityBoxEntity box,
            ServerPlayer owner,
            int bit,
            RegistryObject<SoundEvent> sound,
            int busyTicks,
            String id
    ) {
        if (VerityPlayerData.hasQ1RarePairPlayed(owner, bit)) {
            return false;
        }
        if (speak(box, owner, sound, busyTicks, id)) {
            VerityPlayerData.markQ1RarePairPlayed(owner, bit);
            return true;
        }
        return false;
    }

    private static boolean speak(
            VerityBoxEntity box,
            ServerPlayer owner,
            RegistryObject<SoundEvent> sound,
            int busyTicks,
            String id
    ) {
        if (!VerityCommonConfig.ENABLE_VOICE_LINES.get()) {
            return true;
        }
        if (!(box.level() instanceof ServerLevel) || box.level().isClientSide) {
            return false;
        }
        if (sound == null || !sound.isPresent()) {
            return false;
        }
        String soundId = VeritySounds.resolveId(sound.get());
        if (soundId == null) {
            return false;
        }
        VerityVoiceContext ctx = VerityVoiceContext.atEntity(
                owner,
                box.getId(),
                box.getX(),
                box.getY(),
                box.getZ(),
                SoundSource.BLOCKS,
                true
        );
        String subtitle = VeritySounds.subtitleKeyFor(soundId);
        if (VerityVoiceDirector.requestDirect(
                owner,
                soundId,
                VerityVoiceCategory.BOX_INTRO,
                busyTicks,
                subtitle,
                0.82f,
                0.98f,
                ctx
        )) {
            box.setVoiceBusyTicks(busyTicks);
            box.setLastVoiceLine(id);
            return true;
        }
        return false;
    }
}
