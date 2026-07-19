package com.universeexe.verity.voice;

import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.registry.VerityItems;
import com.universeexe.verity.trust.MoodState;
import com.universeexe.verity.trust.VerityTrustKeys;
import com.universeexe.verity.trust.VerityTrustManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached server-side context used for pool filtering and debug output.
 */
public record VerityVoiceSnapshot(
        int trust,
        MoodState mood,
        @Nullable String currentQuest,
        long gameTime,
        long dayTime,
        boolean raining,
        boolean thundering,
        ResourceKey<Level> dimension,
        String biomeTag,
        float playerHealth,
        int playerFood,
        double distanceToVerity,
        boolean verityCarried,
        boolean verityFollowing,
        boolean countdownStarted,
        boolean transformed,
        int nearbyHostiles
) {
    private static final int CACHE_TICKS = 20;
    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();

    public static VerityVoiceSnapshot capture(ServerPlayer player, @Nullable VerityEntity verity) {
        ServerLevel level = player.serverLevel();
        long tick = level.getGameTime();
        Cached cached = CACHE.get(player.getUUID());
        if (cached != null && cached.tick == tick) {
            return cached.snapshot;
        }
        VerityVoiceSnapshot snapshot = build(player, verity, level);
        CACHE.put(player.getUUID(), new Cached(tick, snapshot));
        return snapshot;
    }

    public static void invalidate(UUID playerId) {
        CACHE.remove(playerId);
    }

    private static VerityVoiceSnapshot build(ServerPlayer player, @Nullable VerityEntity verity, ServerLevel level) {
        var tag = VerityPlayerData.get(player);
        String quest = tag.contains(VerityTrustKeys.HELP_CONVERSATION_ID)
                ? tag.getString(VerityTrustKeys.HELP_CONVERSATION_ID)
                : null;
        if (quest != null && quest.isBlank()) {
            quest = null;
        }

        double distance = verity == null ? -1 : verity.distanceTo(player);
        boolean carried = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(VerityItems.VERITY_ITEM.get())) {
                carried = true;
                break;
            }
        }
        boolean following = verity != null && verity.isFollowingOwner();

        BlockPos pos = player.blockPosition();
        Holder<Biome> biome = level.getBiome(pos);
        String biomeTag = biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown");

        int hostiles = 0;
        if (verity != null) {
            hostiles = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                            verity.getBoundingBox().inflate(12))
                    .size();
        }

        return new VerityVoiceSnapshot(
                VerityTrustManager.getTrust(player),
                VerityTrustManager.getMood(player),
                quest,
                level.getGameTime(),
                level.getDayTime(),
                level.isRaining(),
                level.isThundering(),
                level.dimension(),
                biomeTag,
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                distance,
                carried,
                following,
                tag.getBoolean(VerityTrustKeys.COUNTDOWN_STARTED),
                tag.getBoolean(VerityTrustKeys.TRANSFORMED),
                hostiles
        );
    }

    public String debugSummary() {
        return "trust=" + trust
                + " mood=" + mood
                + " quest=" + (currentQuest == null ? "none" : currentQuest)
                + " dist=" + String.format("%.1f", distanceToVerity)
                + " carried=" + verityCarried
                + " follow=" + verityFollowing
                + " weather=" + (thundering ? "storm" : (raining ? "rain" : "clear"))
                + " biome=" + biomeTag
                + " hostiles=" + nearbyHostiles
                + " countdown=" + countdownStarted
                + " transformed=" + transformed;
    }

    private record Cached(long tick, VerityVoiceSnapshot snapshot) {
    }
}
