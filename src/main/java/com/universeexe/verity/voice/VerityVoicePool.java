package com.universeexe.verity.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Weighted voice pool with filtering, anti-repetition, and optional SILENT outcomes.
 */
public final class VerityVoicePool {
    private final String id;
    private final VerityVoiceCategory category;
    private final List<VerityVoiceVariant> variants;
    private final int silentWeight;

    public VerityVoicePool(String id, VerityVoiceCategory category, List<VerityVoiceVariant> variants, int silentWeight) {
        this.id = id;
        this.category = category;
        this.variants = List.copyOf(variants);
        this.silentWeight = Math.max(0, silentWeight);
    }

    public String id() {
        return id;
    }

    public VerityVoiceCategory category() {
        return category;
    }

    public List<VerityVoiceVariant> variants() {
        return variants;
    }

    public boolean isEmpty() {
        return variants.isEmpty() && silentWeight <= 0;
    }

    /** Legacy equal-random pick (debug / unfiltered). */
    public Optional<VerityVoiceVariant> pick(RandomSource random) {
        if (variants.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(variants.get(random.nextInt(variants.size())));
    }

    /**
     * Weighted selection with trust/mood/memory filters, cooldowns, and anti-repeat.
     */
    public Optional<VerityVoiceVariant> pick(
            ServerPlayer player,
            VerityVoiceSnapshot snapshot,
            VerityVoiceMemory memory,
            RandomSource random
    ) {
        long gameTime = snapshot.gameTime();
        List<WeightedCandidate> candidates = new ArrayList<>();
        List<String> rejections = VerityVoiceDirector.debugRejections(player.getUUID());

        for (VerityVoiceVariant variant : variants) {
            String reject = rejectionReason(player, snapshot, memory, variant, gameTime);
            if (reject != null) {
                if (rejections != null) {
                    rejections.add(variant.id() + ": " + reject);
                }
                continue;
            }
            int adjusted = adjustedWeight(player, memory, variant);
            if (adjusted > 0) {
                candidates.add(new WeightedCandidate(variant, adjusted));
            }
        }

        if (silentWeight > 0) {
            candidates.add(new WeightedCandidate(VerityVoiceVariant.silent(id + "_silent", 10), silentWeight));
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        int total = candidates.stream().mapToInt(c -> c.weight).sum();
        int roll = random.nextInt(total);
        int cursor = 0;
        for (WeightedCandidate candidate : candidates) {
            cursor += candidate.weight;
            if (roll < cursor) {
                return Optional.of(candidate.variant);
            }
        }
        return Optional.of(candidates.get(candidates.size() - 1).variant);
    }

    private static int adjustedWeight(ServerPlayer player, VerityVoiceMemory memory, VerityVoiceVariant variant) {
        int weight = Math.max(1, variant.weight());
        if (memory.recentlyPlayed(variant.id())) {
            return 0;
        }
        int lifetime = memory.lifetimeCount(player, variant.id());
        if (lifetime == 0) {
            weight += 2;
        } else if (lifetime > 3) {
            weight = Math.max(1, weight / 2);
        }
        if (variant.rare()) {
            weight = Math.max(1, weight / 3);
        }
        return weight;
    }

    private static String rejectionReason(
            ServerPlayer player,
            VerityVoiceSnapshot snapshot,
            VerityVoiceMemory memory,
            VerityVoiceVariant variant,
            long gameTime
    ) {
        if (snapshot.trust() < variant.minTrust() || snapshot.trust() > variant.maxTrust()) {
            return "trust";
        }
        if (!variant.allowedMoods().isEmpty() && !variant.allowedMoods().contains(snapshot.mood())) {
            return "mood";
        }
        for (VerityVoiceMemoryFlag required : variant.requiredMemory()) {
            if (!memory.hasFlag(player, required)) {
                return "missing_" + required.name();
            }
        }
        for (VerityVoiceMemoryFlag forbidden : variant.forbiddenMemory()) {
            if (memory.hasFlag(player, forbidden)) {
                return "forbidden_" + forbidden.name();
            }
        }
        if (memory.isOnCooldown(player, variant, gameTime)) {
            return "cooldown";
        }
        if (memory.exceedsMaxPlays(player, variant)) {
            return "max_plays";
        }
        if (memory.recentlyPlayed(variant.id())) {
            return "recent";
        }
        return null;
    }

    public static VerityVoicePool fromJson(String poolId, JsonObject obj) {
        VerityVoiceCategory category = VerityVoiceCategory.fromManifest(
                obj.has("category") ? obj.get("category").getAsString() : null);
        List<VerityVoiceVariant> variants = new ArrayList<>();
        if (obj.has("variants")) {
            JsonArray array = obj.getAsJsonArray("variants");
            for (int i = 0; i < array.size(); i++) {
                variants.add(VerityVoiceVariant.fromJson(poolId + "_" + i, array.get(i).getAsJsonObject()));
            }
        }
        int silentWeight = 0;
        if (obj.has("silent_weight")) {
            silentWeight = obj.get("silent_weight").getAsInt();
        } else if (obj.has("allow_silent") && obj.get("allow_silent").getAsBoolean()) {
            silentWeight = 1;
        }
        return new VerityVoicePool(poolId, category, Collections.unmodifiableList(variants), silentWeight);
    }

    private record WeightedCandidate(VerityVoiceVariant variant, int weight) {
    }
}
