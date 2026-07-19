package com.universeexe.verity.event;

import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityBoxEntity;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.registry.VerityEntities;
import com.universeexe.verity.util.SafeBoxPlacement;
import com.universeexe.verity.util.VerityDebug;
import com.universeexe.verity.quest.VerityFtbQuestBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerIntroductionHandler {
    private final Map<UUID, Integer> pendingDelays = new HashMap<>();

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        considerScheduling(player);
        reconnectExisting(player);
        VerityFtbQuestBridge.reconcileAll(player);
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        considerScheduling(player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Death must not create a second box.
        reconnectExisting(player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (pendingDelays.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> it = pendingDelays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining > 0) {
                entry.setValue(remaining);
                continue;
            }
            UUID playerId = entry.getKey();
            it.remove();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                trySpawnBox(player);
            }
        }
    }

    private void considerScheduling(ServerPlayer player) {
        if (!VerityCommonConfig.ENABLE_FIRST_JOIN_INTRODUCTION.get()) {
            return;
        }
        if (VerityPlayerData.isRevealCompleted(player) || VerityPlayerData.getVerityUuid(player).isPresent()) {
            return;
        }
        if (VerityPlayerData.isIntroStarted(player) && VerityPlayerData.getBoxUuid(player).isPresent()) {
            return;
        }
        if (VerityCommonConfig.OVERWORLD_ONLY.get() && player.level().dimension() != Level.OVERWORLD) {
            VerityDebug.log("Delaying intro for {} until Overworld", player.getGameProfile().getName());
            return;
        }
        if (VerityPlayerData.isPendingSpawn(player) || pendingDelays.containsKey(player.getUUID())) {
            return;
        }
        if (VerityPlayerData.isIntroStarted(player) && VerityPlayerData.getBoxUuid(player).isEmpty()) {
            // Recovery path: intro flagged but box missing.
            VerityDebug.log("Recovering missing box for {}", player.getGameProfile().getName());
        } else if (VerityPlayerData.isIntroStarted(player)) {
            return;
        }

        int delay = VerityCommonConfig.FIRST_JOIN_DELAY_TICKS.get();
        pendingDelays.put(player.getUUID(), delay);
        VerityPlayerData.setPendingSpawn(player, true);
        VerityDebug.log("Scheduled Verity intro for {} in {} ticks", player.getGameProfile().getName(), delay);
    }

    private void reconnectExisting(ServerPlayer player) {
        Optional<UUID> boxId = VerityPlayerData.getBoxUuid(player);
        if (boxId.isPresent() && player.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(boxId.get());
            if (entity instanceof VerityBoxEntity box) {
                VerityDebug.log("Reconnected {} to box {}", player.getGameProfile().getName(), box.getUUID());
                return;
            }
            // Search other loaded levels via server
            for (ServerLevel serverLevel : player.server.getAllLevels()) {
                Entity found = serverLevel.getEntity(boxId.get());
                if (found instanceof VerityBoxEntity) {
                    VerityDebug.log("Found owned box in dimension {}", serverLevel.dimension().location());
                    return;
                }
            }
            if (!VerityPlayerData.isRevealCompleted(player)) {
                VerityDebug.log("Stored box UUID missing for {}; allowing recovery spawn", player.getGameProfile().getName());
                VerityPlayerData.setPendingSpawn(player, false);
                VerityPlayerData.setIntroStarted(player, false);
                considerScheduling(player);
            }
        }

        Optional<UUID> verityId = VerityPlayerData.getVerityUuid(player);
        if (verityId.isPresent()) {
            for (ServerLevel serverLevel : player.server.getAllLevels()) {
                Entity found = serverLevel.getEntity(verityId.get());
                if (found instanceof VerityEntity) {
                    VerityDebug.log("Reconnected {} to Verity {}", player.getGameProfile().getName(), verityId.get());
                    return;
                }
            }
        }
    }

    private void trySpawnBox(ServerPlayer player) {
        VerityPlayerData.setPendingSpawn(player, false);
        if (!VerityCommonConfig.ENABLE_FIRST_JOIN_INTRODUCTION.get()) {
            return;
        }
        if (VerityCommonConfig.OVERWORLD_ONLY.get() && player.level().dimension() != Level.OVERWORLD) {
            VerityDebug.log("Player {} left Overworld before spawn; will wait", player.getGameProfile().getName());
            return;
        }
        if (VerityPlayerData.isRevealCompleted(player)) {
            return;
        }
        if (VerityCommonConfig.ALLOW_ONE_BOX_PER_PLAYER.get() && VerityPlayerData.getBoxUuid(player).isPresent()) {
            // Verify still exists
            Optional<UUID> existing = VerityPlayerData.getBoxUuid(player);
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity e = level.getEntity(existing.get());
                if (e instanceof VerityBoxEntity) {
                    return;
                }
            }
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.hasChunkAt(player.blockPosition())) {
            retryLater(player);
            return;
        }

        Optional<SafeBoxPlacement.Placement> placement = SafeBoxPlacement.find(level, player);
        if (placement.isEmpty()) {
            retryLater(player);
            return;
        }

        VerityBoxEntity box = VerityEntities.VERITY_BOX.get().create(level);
        if (box == null) {
            retryLater(player);
            return;
        }
        SafeBoxPlacement.Placement place = placement.get();
        box.moveTo(place.position().x, place.position().y, place.position().z, place.yRot(), 0);
        box.configureSpawn(player.getUUID(), place.yRot());
        if (!level.addFreshEntity(box)) {
            retryLater(player);
            return;
        }

        VerityPlayerData.setIntroStarted(player, true);
        VerityPlayerData.setBoxUuid(player, box.getUUID());
        VerityPlayerData.setSpawnRetryCount(player, 0);
        VerityDebug.log("Spawned Verity box {} for {} at {}", box.getUUID(), player.getGameProfile().getName(), place.position());
    }

    private void retryLater(ServerPlayer player) {
        int retries = VerityPlayerData.getSpawnRetryCount(player) + 1;
        VerityPlayerData.setSpawnRetryCount(player, retries);
        if (retries > VerityCommonConfig.MAXIMUM_SPAWN_RETRIES.get()) {
            VerityDebug.warn("Exceeded box spawn retries for {}", player.getGameProfile().getName());
            return;
        }
        pendingDelays.put(player.getUUID(), VerityCommonConfig.SPAWN_RETRY_DELAY_TICKS.get());
        VerityPlayerData.setPendingSpawn(player, true);
        VerityDebug.log("Retrying box spawn for {} (attempt {})", player.getGameProfile().getName(), retries);
    }

    public void forceSchedule(ServerPlayer player) {
        pendingDelays.put(player.getUUID(), 5);
        VerityPlayerData.setPendingSpawn(player, true);
    }
}
