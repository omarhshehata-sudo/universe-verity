package com.universeexe.verity.voice;

import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.quest.VerityQuest1IntroPlan;
import com.universeexe.verity.trust.VerityTrustManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Hooks idle, danger, and environment voice triggers into the director.
 */
public final class VerityVoiceEvents {
    private static final int IDLE_CHECK_INTERVAL = 200;
    private static final int DANGER_COOLDOWN_TICKS = 600;
    private static final int IDLE_COOLDOWN_TICKS = 7200;

    public VerityVoiceEvents() {
    }

    public static void registerReloadListener(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(VerityVoiceManifest.get());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            VerityQuest1IntroPlan.verifyOnce();
        } catch (IllegalStateException ex) {
            com.universeexe.verity.UniverseVerity.LOGGER.error("[VerityQuest1Intro] Plan verification failed", ex);
            throw ex;
        }
        com.universeexe.verity.UniverseVerity.LOGGER.info(
                "[VerityVoice] Director online — {} pools, {} conversations",
                VerityVoiceManifest.get().pools().size(),
                VerityVoiceManifest.get().conversations().size()
        );
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide || player.tickCount % IDLE_CHECK_INTERVAL != 0) {
            return;
        }
        if (!VerityCommonConfig.ENABLE_VOICE_LINES.get()) {
            return;
        }
        tickDanger(player);
        tickIdle(player);
    }

    private static void tickDanger(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long last = VerityPlayerData.get(player).getLong(VerityVoiceKeys.LAST_DANGER_VOICE);
        if (now - last < DANGER_COOLDOWN_TICKS) {
            return;
        }
        VerityTrustManager.findOwnedVerity(player).ifPresent(verity -> {
            if (verity.distanceTo(player) > 24) {
                return;
            }
            boolean threat = level.getEntitiesOfClass(Monster.class, verity.getBoundingBox().inflate(12))
                    .stream()
                    .anyMatch(m -> m.getTarget() == player || m.distanceTo(verity) < 8);
            if (!threat) {
                return;
            }
            VerityVoiceContext ctx = VerityVoiceContext.atEntity(
                    player,
                    verity.getId(),
                    verity.getX(),
                    verity.getY(),
                    verity.getZ(),
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    true
            ).withOnStart(() -> verity.beginTalkingForTicks(20));
            if (VerityVoiceDirector.requestPool(player, "danger_near_hostile", ctx)) {
                VerityPlayerData.get(player).putLong(VerityVoiceKeys.LAST_DANGER_VOICE, now);
            }
        });
    }

    private static void tickIdle(ServerPlayer player) {
        if (VerityPlayerData.get(player).getBoolean(com.universeexe.verity.trust.VerityTrustKeys.STORY_MUTE_RANDOM)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long last = VerityPlayerData.get(player).getLong(VerityVoiceKeys.LAST_IDLE_VOICE);
        if (now - last < IDLE_COOLDOWN_TICKS) {
            return;
        }
        if (VerityVoiceDirector.isPlayerBusy(player.getUUID())) {
            return;
        }
        VerityTrustManager.findOwnedVerity(player).ifPresent(verity -> {
            if (verity.distanceTo(player) > 16 || verity.isFollowingOwner()) {
                return;
            }
            if (!(verity instanceof VerityEntity)) {
                return;
            }
            VerityVoiceContext ctx = VerityVoiceContext.atEntity(
                    player,
                    verity.getId(),
                    verity.getX(),
                    verity.getY(),
                    verity.getZ(),
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    false
            );
            if (VerityVoiceDirector.requestPool(player, "idle_ambient", ctx)) {
                VerityPlayerData.get(player).putLong(VerityVoiceKeys.LAST_IDLE_VOICE, now);
            }
        });
    }
}
