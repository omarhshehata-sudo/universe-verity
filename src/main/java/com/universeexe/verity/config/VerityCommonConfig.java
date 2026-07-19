package com.universeexe.verity.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VerityCommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_FIRST_JOIN_INTRODUCTION;
    public static final ForgeConfigSpec.BooleanValue OVERWORLD_ONLY;
    public static final ForgeConfigSpec.IntValue FIRST_JOIN_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue PREFERRED_SPAWN_DISTANCE;
    public static final ForgeConfigSpec.IntValue MAXIMUM_SPAWN_RETRIES;
    public static final ForgeConfigSpec.IntValue SPAWN_RETRY_DELAY_TICKS;
    public static final ForgeConfigSpec.BooleanValue PROTECT_INTRO_BOX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_VOICE_LINES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUBTITLES;
    public static final ForgeConfigSpec.DoubleValue MAXIMUM_HEARING_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue INITIAL_SEQUENCE_ENABLED;
    public static final ForgeConfigSpec.IntValue BOX_VOICE_PAUSE_TICKS;
    public static final ForgeConfigSpec.IntValue BOX_PLEASE_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue BOX_STILL_THERE_DELAY_TICKS;
    public static final ForgeConfigSpec.BooleanValue IDLE_CALLING_ENABLED;
    public static final ForgeConfigSpec.IntValue IDLE_MINIMUM_DELAY_SECONDS;
    public static final ForgeConfigSpec.IntValue IDLE_MAXIMUM_DELAY_SECONDS;
    public static final ForgeConfigSpec.BooleanValue INTERACTION_RESPONSE_ENABLED;
    public static final ForgeConfigSpec.IntValue INTERACTION_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_ONE_BOX_PER_PLAYER;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    public static final ForgeConfigSpec.BooleanValue ENABLE_BOX_REVEAL;
    public static final ForgeConfigSpec.BooleanValue OWNER_ONLY_REVEAL;
    public static final ForgeConfigSpec.BooleanValue REVEAL_REQUIRES_EMPTY_HAND;
    public static final ForgeConfigSpec.IntValue REVEAL_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue BOX_OPENING_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue VERITY_SPAWN_TICK;
    public static final ForgeConfigSpec.IntValue BOX_REMOVAL_TICK;
    public static final ForgeConfigSpec.IntValue GREETING_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue OPEN_FOUND_PAUSE_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GREETING;
    public static final ForgeConfigSpec.DoubleValue GREETING_VOLUME;
    public static final ForgeConfigSpec.DoubleValue GREETING_HEARING_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue PROTECT_VERITY;
    public static final ForgeConfigSpec.BooleanValue KEEP_VERITY_STATIONARY_AFTER_REVEAL;
    public static final ForgeConfigSpec.BooleanValue LOOK_AT_OWNER_AFTER_REVEAL;
    public static final ForgeConfigSpec.BooleanValue ALLOW_REVEAL_RETRY_AFTER_FAILURE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_REVEAL_LOGGING;
    public static final ForgeConfigSpec.BooleanValue USE_MUFFLED_SUBTITLE_STYLE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("introduction");
        ENABLE_FIRST_JOIN_INTRODUCTION = builder.define("enableFirstJoinIntroduction", true);
        OVERWORLD_ONLY = builder.define("overworldOnly", true);
        FIRST_JOIN_DELAY_TICKS = builder.defineInRange("firstJoinDelayTicks", 0, 0, 1200);
        PREFERRED_SPAWN_DISTANCE = builder.defineInRange("preferredSpawnDistance", 4, 2, 8);
        MAXIMUM_SPAWN_RETRIES = builder.defineInRange("maximumSpawnRetries", 10, 1, 50);
        SPAWN_RETRY_DELAY_TICKS = builder.defineInRange("spawnRetryDelayTicks", 20, 5, 200);
        PROTECT_INTRO_BOX = builder.define("protectIntroBox", true);
        ENABLE_VOICE_LINES = builder.define("enableVoiceLines", true);
        ENABLE_SUBTITLES = builder.define("enableSubtitles", true);
        MAXIMUM_HEARING_DISTANCE = builder.defineInRange("maximumHearingDistance", 24.0, 8.0, 64.0);
        INITIAL_SEQUENCE_ENABLED = builder.define("initialSequenceEnabled", true);
        // Pause after each of the first four sealed lines (clip end → next line).
        BOX_VOICE_PAUSE_TICKS = builder.defineInRange("boxVoicePauseTicks", 60, 40, 80);
        // Longer wait after line 4 before muffled "Please".
        BOX_PLEASE_DELAY_TICKS = builder.defineInRange("boxPleaseDelayTicks", 240, 80, 1200);
        // Very long wait after "Please" before "You're still there".
        BOX_STILL_THERE_DELAY_TICKS = builder.defineInRange("boxStillThereDelayTicks", 900, 200, 6000);
        IDLE_CALLING_ENABLED = builder.define("idleCallingEnabled", true);
        // Quiet gap between sealed-box activity bursts (seconds). Forever until reveal.
        IDLE_MINIMUM_DELAY_SECONDS = builder.defineInRange("idleMinimumDelaySeconds", 5, 2, 300);
        IDLE_MAXIMUM_DELAY_SECONDS = builder.defineInRange("idleMaximumDelaySeconds", 6, 2, 600);
        INTERACTION_RESPONSE_ENABLED = builder.define("interactionResponseEnabled", true);
        INTERACTION_COOLDOWN_TICKS = builder.defineInRange("interactionCooldownTicks", 20, 1, 200);
        ALLOW_ONE_BOX_PER_PLAYER = builder.define("allowOneBoxPerPlayer", true);
        DEBUG_LOGGING = builder.define("debugLogging", false);
        USE_MUFFLED_SUBTITLE_STYLE = builder.define("useMuffledSubtitleStyle", true);
        builder.pop();

        builder.push("reveal");
        ENABLE_BOX_REVEAL = builder.define("enableBoxReveal", true);
        OWNER_ONLY_REVEAL = builder.define("ownerOnlyReveal", true);
        REVEAL_REQUIRES_EMPTY_HAND = builder.define("revealRequiresEmptyHand", false);
        REVEAL_DELAY_TICKS = builder.defineInRange("revealDelayTicks", 0, 0, 100);
        BOX_OPENING_DURATION_TICKS = builder.defineInRange("boxOpeningDurationTicks", 40, 1, 200);
        VERITY_SPAWN_TICK = builder.defineInRange("veritySpawnTick", 40, 1, 200);
        BOX_REMOVAL_TICK = builder.defineInRange("boxRemovalTick", 40, 1, 250);
        // Legacy delay used when open-found line is disabled / skipped.
        GREETING_DELAY_TICKS = builder.defineInRange("greetingDelayTicks", 20, 0, 200);
        // Pause after "Oh! You found the opening" before the personal-helper greeting.
        OPEN_FOUND_PAUSE_TICKS = builder.defineInRange("openFoundPauseTicks", 20, 0, 100);
        ENABLE_GREETING = builder.define("enableGreeting", true);
        GREETING_VOLUME = builder.defineInRange("greetingVolume", 1.0, 0.0, 2.0);
        GREETING_HEARING_DISTANCE = builder.defineInRange("greetingHearingDistance", 24.0, 8.0, 64.0);
        PROTECT_VERITY = builder.define("protectVerity", true);
        // Default OFF: throw/pickup needs free physics. When ON, lock applies only on ground
        // (horizontal only — never cancels gravity / midair fall), never while WasThrown,
        // and never on the client — see VerityEntity.tick.
        KEEP_VERITY_STATIONARY_AFTER_REVEAL = builder.define("keepVerityStationaryAfterReveal", false);
        LOOK_AT_OWNER_AFTER_REVEAL = builder.define("lookAtOwnerAfterReveal", true);
        ALLOW_REVEAL_RETRY_AFTER_FAILURE = builder.define("allowRevealRetryAfterFailure", true);
        DEBUG_REVEAL_LOGGING = builder.define("debugRevealLogging", false);
        builder.pop();

        SPEC = builder.build();
    }

    private VerityCommonConfig() {
    }

    public static int clampedIdleMinSeconds() {
        int min = IDLE_MINIMUM_DELAY_SECONDS.get();
        int max = IDLE_MAXIMUM_DELAY_SECONDS.get();
        return Math.min(min, max);
    }

    public static int clampedIdleMaxSeconds() {
        int min = IDLE_MINIMUM_DELAY_SECONDS.get();
        int max = IDLE_MAXIMUM_DELAY_SECONDS.get();
        return Math.max(min, max);
    }
}
