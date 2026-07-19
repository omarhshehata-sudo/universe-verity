package com.universeexe.verity.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.universeexe.verity.trust.MoodState;

import java.util.EnumSet;
import java.util.Set;

/**
 * One voice choice with timing, presentation, and selection metadata.
 */
public record VerityVoiceVariant(
        String id,
        String soundId,
        int durationTicks,
        String subtitleKey,
        float volume,
        float pitch,
        int weight,
        boolean silent,
        boolean rare,
        int cooldownTicks,
        int maxPlays,
        int minTrust,
        int maxTrust,
        Set<MoodState> allowedMoods,
        Set<VerityVoiceMemoryFlag> requiredMemory,
        Set<VerityVoiceMemoryFlag> forbiddenMemory
) {
    public static final float DEFAULT_VOLUME = 1.0f;
    public static final float DEFAULT_PITCH = 1.0f;
    public static final int DEFAULT_WEIGHT = 1;

    public VerityVoiceVariant {
        if (durationTicks < 1) {
            durationTicks = 1;
        }
        volume = clampVolume(volume);
        if (weight < 0) {
            weight = 0;
        }
        allowedMoods = allowedMoods == null ? Set.of() : Set.copyOf(allowedMoods);
        requiredMemory = requiredMemory == null ? Set.of() : Set.copyOf(requiredMemory);
        forbiddenMemory = forbiddenMemory == null ? Set.of() : Set.copyOf(forbiddenMemory);
    }

    public static VerityVoiceVariant silent(String id, int durationTicks) {
        return new VerityVoiceVariant(
                id, "", durationTicks, "", DEFAULT_VOLUME, DEFAULT_PITCH,
                DEFAULT_WEIGHT, true, false, 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                Set.of(), Set.of(), Set.of()
        );
    }

    /** Unfiltered variant for direct/cue playback paths. */
    public static VerityVoiceVariant simple(
            String id,
            String soundId,
            int durationTicks,
            String subtitleKey,
            float volume,
            float pitch
    ) {
        return new VerityVoiceVariant(
                id, soundId, durationTicks, subtitleKey, volume, pitch,
                DEFAULT_WEIGHT, false, false, 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                Set.of(), Set.of(), Set.of()
        );
    }

    public static VerityVoiceVariant fromJson(String fallbackId, JsonObject obj) {
        String id = obj.has("id") ? obj.get("id").getAsString() : fallbackId;
        boolean silent = obj.has("silent") && obj.get("silent").getAsBoolean();
        String sound = obj.has("sound") ? obj.get("sound").getAsString() : "";
        int duration = obj.has("duration_ticks") ? obj.get("duration_ticks").getAsInt() : 20;
        String subtitle = obj.has("subtitle") ? obj.get("subtitle").getAsString() : "";
        float volume = obj.has("volume") ? obj.get("volume").getAsFloat() : DEFAULT_VOLUME;
        float pitch = obj.has("pitch") ? obj.get("pitch").getAsFloat() : DEFAULT_PITCH;
        int weight = obj.has("weight") ? obj.get("weight").getAsInt() : DEFAULT_WEIGHT;
        boolean rare = obj.has("rare") && obj.get("rare").getAsBoolean();
        int cooldown = obj.has("cooldown_ticks") ? obj.get("cooldown_ticks").getAsInt() : 0;
        int maxPlays = obj.has("maximum_plays") ? obj.get("maximum_plays").getAsInt()
                : (obj.has("max_plays") ? obj.get("max_plays").getAsInt() : -1);
        int minTrust = obj.has("minimum_trust") ? obj.get("minimum_trust").getAsInt()
                : (obj.has("min_trust") ? obj.get("min_trust").getAsInt() : Integer.MIN_VALUE);
        int maxTrust = obj.has("maximum_trust") ? obj.get("maximum_trust").getAsInt()
                : (obj.has("max_trust") ? obj.get("max_trust").getAsInt() : Integer.MAX_VALUE);
        Set<MoodState> moods = parseMoods(obj);
        Set<VerityVoiceMemoryFlag> required = parseFlags(obj, "required_memory", "required_memory_flags");
        Set<VerityVoiceMemoryFlag> forbidden = parseFlags(obj, "forbidden_memory", "forbidden_memory_flags");
        if (silent && sound.isBlank()) {
            return silent(id, duration);
        }
        return new VerityVoiceVariant(
                id, sound, duration, subtitle, volume, pitch, weight, silent, rare,
                cooldown, maxPlays, minTrust, maxTrust, moods, required, forbidden
        );
    }

    private static Set<MoodState> parseMoods(JsonObject obj) {
        if (!obj.has("moods")) {
            return Set.of();
        }
        JsonArray array = obj.getAsJsonArray("moods");
        EnumSet<MoodState> moods = EnumSet.noneOf(MoodState.class);
        for (int i = 0; i < array.size(); i++) {
            moods.add(MoodState.fromName(array.get(i).getAsString()));
        }
        return moods;
    }

    private static Set<VerityVoiceMemoryFlag> parseFlags(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key)) {
                JsonArray array = obj.getAsJsonArray(key);
                EnumSet<VerityVoiceMemoryFlag> flags = EnumSet.noneOf(VerityVoiceMemoryFlag.class);
                for (int i = 0; i < array.size(); i++) {
                    try {
                        flags.add(VerityVoiceMemoryFlag.valueOf(array.get(i).getAsString().trim().toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                return flags;
            }
        }
        return Set.of();
    }

    public static float clampVolume(float volume) {
        return Math.min(0.95f, Math.max(0.05f, volume));
    }
}
