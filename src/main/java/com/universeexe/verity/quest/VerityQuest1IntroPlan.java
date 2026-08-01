package com.universeexe.verity.quest;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.voice.VerityQueuedVoiceEvent;
import com.universeexe.verity.voice.VerityVoiceDurations;
import com.universeexe.verity.voice.VerityVoiceVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Authoritative post-reveal Quest 1 intro voice plan.
 * <p>
 * After box-open float/fall/land, exactly one line plays:
 * {@code verity.greeting.personal_helper} via {@link com.universeexe.verity.network.PlayVoicePacket}.
 * No reveal "Oh!" / "You found the opening" lines and no quest_01_intro_ending pool tail.
 * </p>
 * Duration measured with ffprobe on {@code greeting_personal_helper.ogg}: 5.866667 s.
 */
public final class VerityQuest1IntroPlan {
    /** Measured ffprobe duration (seconds) of greeting_personal_helper.ogg. */
    public static final double GREETING_OGG_SECONDS = 5.866667;

    public static final String GREETING_SOUND_ID = "verity.greeting.personal_helper";
    public static final String SUBTITLE_KEY = VeritySounds.subtitleKeyFor(GREETING_SOUND_ID);
    public static final int DURATION_TICKS = VerityVoiceDurations.GREETING_PERSONAL_HELPER;

    private static final Set<String> FORBIDDEN_SOUND_IDS = Set.of(
            "verity.q01.reveal.oh",
            "verity.q01.reveal.found_opening",
            "verity.box.oh_you_found_the_opening",
            "verity.q01.intro.hello",
            "verity.q01.intro.name",
            "verity.q01.intro.helper_friend",
            "verity.q01.intro.ask_anything",
            "verity.q01.intro.know_everything",
            "verity.q01.intro.almost_everything",
            "verity.q01.intro.everything_important"
    );

    private static volatile boolean verified;

    public record PlannedStep(String soundId, int durationTicks, int pauseAfterTicks, String subtitleKey) {
        public VerityQueuedVoiceEvent.ResolvedStep toResolvedStep() {
            return new VerityQueuedVoiceEvent.ResolvedStep(
                    VerityVoiceVariant.simple(
                            soundId,
                            soundId,
                            durationTicks,
                            subtitleKey,
                            0.92f,
                            1.0f
                    ),
                    pauseAfterTicks
            );
        }
    }

    private VerityQuest1IntroPlan() {
    }

    public static List<PlannedStep> plannedSteps() {
        return List.of(new PlannedStep(GREETING_SOUND_ID, DURATION_TICKS, 0, SUBTITLE_KEY));
    }

    public static ArrayList<VerityQueuedVoiceEvent.ResolvedStep> resolvedSteps() {
        var resolved = new ArrayList<VerityQueuedVoiceEvent.ResolvedStep>();
        for (PlannedStep step : plannedSteps()) {
            resolved.add(step.toResolvedStep());
        }
        return resolved;
    }

    public static void verifyOnce() {
        if (verified) {
            return;
        }
        synchronized (VerityQuest1IntroPlan.class) {
            if (verified) {
                return;
            }
            verifyOrThrow();
            verified = true;
            UniverseVerity.LOGGER.info(
                    "[VerityQuest1Intro] Plan verified: 1 step, sound={}, duration_ticks={}, subtitle={}",
                    GREETING_SOUND_ID,
                    DURATION_TICKS,
                    SUBTITLE_KEY);
        }
    }

    public static void verifyOrThrow() {
        int expectedTicks = VerityVoiceDurations.ticks(GREETING_OGG_SECONDS);
        if (DURATION_TICKS != expectedTicks) {
            throw new IllegalStateException(
                    "GREETING_PERSONAL_HELPER ticks " + DURATION_TICKS + " != ffprobe-derived " + expectedTicks);
        }
        List<PlannedStep> plan = plannedSteps();
        if (plan.size() != 1) {
            throw new IllegalStateException("Quest 1 intro must be exactly 1 voice step, got " + plan.size());
        }
        for (PlannedStep step : plan) {
            if (FORBIDDEN_SOUND_IDS.contains(step.soundId())) {
                throw new IllegalStateException("Forbidden intro sound in plan: " + step.soundId());
            }
            if (!GREETING_SOUND_ID.equals(step.soundId())) {
                throw new IllegalStateException("Unexpected intro sound: " + step.soundId());
            }
            if (step.durationTicks() <= 0) {
                throw new IllegalStateException("Invalid duration for " + step.soundId());
            }
            if (!SUBTITLE_KEY.equals(step.subtitleKey())) {
                throw new IllegalStateException("Subtitle key mismatch for " + step.soundId());
            }
            if (!VeritySounds.subtitleKeyFor(step.soundId()).equals(step.subtitleKey())) {
                throw new IllegalStateException("Subtitle key does not match sound id mapping for " + step.soundId());
            }
            RegistryObject<net.minecraft.sounds.SoundEvent> registered = VeritySounds.byId(step.soundId());
            if (registered == null || !registered.isPresent()) {
                throw new IllegalStateException("Intro sound not registered: " + step.soundId());
            }
        }
    }

    public static List<String> formatPlanLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Quest 1 post-reveal intro voice plan (after float/fall/land):");
        int index = 1;
        for (PlannedStep step : plannedSteps()) {
            lines.add(String.format(
                    "%d. soundId=%s duration_ticks=%d pause_after=%d subtitle=%s",
                    index++,
                    step.soundId(),
                    step.durationTicks(),
                    step.pauseAfterTicks(),
                    step.subtitleKey()));
        }
        lines.add("forbidden_in_intro=" + FORBIDDEN_SOUND_IDS.size() + " sound ids blocked");
        lines.add("ffprobe_greeting_seconds=" + GREETING_OGG_SECONDS);
        return lines;
    }

    public static void sendPlanTo(ServerPlayer player) {
        verifyOnce();
        for (String line : formatPlanLines()) {
            player.sendSystemMessage(Component.literal("[VerityIntroPlan] " + line));
        }
    }

    public static void logPlanDryRun(String context) {
        verifyOnce();
        UniverseVerity.LOGGER.info("[VerityQuest1Intro] Dry-run ({})", context);
        for (String line : formatPlanLines()) {
            UniverseVerity.LOGGER.info("[VerityQuest1Intro] {}", line);
        }
    }
}
