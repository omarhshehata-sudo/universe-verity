package com.universeexe.verity.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class VerityPlayerData {
    private VerityPlayerData() {
    }

    public static CompoundTag get(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(VerityIntroDataKeys.ROOT)) {
            CompoundTag root = new CompoundTag();
            root.putInt(VerityIntroDataKeys.PROGRESSION_VERSION, VerityIntroDataKeys.CURRENT_PROGRESSION_VERSION);
            persistent.put(VerityIntroDataKeys.ROOT, root);
        }
        return persistent.getCompound(VerityIntroDataKeys.ROOT);
    }

    public static void copy(Player original, Player clone) {
        CompoundTag source = original.getPersistentData().getCompound(VerityIntroDataKeys.ROOT);
        if (!source.isEmpty()) {
            clone.getPersistentData().put(VerityIntroDataKeys.ROOT, source.copy());
        }
    }

    public static boolean isIntroStarted(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.INTRO_STARTED);
    }

    public static void setIntroStarted(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.INTRO_STARTED, value);
    }

    public static boolean isIntroCompleted(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.INTRO_COMPLETED);
    }

    public static void setIntroCompleted(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.INTRO_COMPLETED, value);
    }

    public static Optional<UUID> getBoxUuid(Player player) {
        CompoundTag tag = get(player);
        if (!tag.hasUUID(VerityIntroDataKeys.BOX_UUID)) {
            return Optional.empty();
        }
        return Optional.of(tag.getUUID(VerityIntroDataKeys.BOX_UUID));
    }

    public static void setBoxUuid(Player player, @Nullable UUID uuid) {
        CompoundTag tag = get(player);
        if (uuid == null) {
            tag.remove(VerityIntroDataKeys.BOX_UUID);
        } else {
            tag.putUUID(VerityIntroDataKeys.BOX_UUID, uuid);
        }
    }

    public static boolean isRevealStarted(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.REVEAL_STARTED);
    }

    public static void setRevealStarted(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.REVEAL_STARTED, value);
    }

    public static boolean isRevealCompleted(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.REVEAL_COMPLETED);
    }

    public static void setRevealCompleted(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.REVEAL_COMPLETED, value);
    }

    public static Optional<UUID> getVerityUuid(Player player) {
        CompoundTag tag = get(player);
        if (!tag.hasUUID(VerityIntroDataKeys.ENTITY_UUID)) {
            return Optional.empty();
        }
        return Optional.of(tag.getUUID(VerityIntroDataKeys.ENTITY_UUID));
    }

    public static void setVerityUuid(Player player, @Nullable UUID uuid) {
        CompoundTag tag = get(player);
        if (uuid == null) {
            tag.remove(VerityIntroDataKeys.ENTITY_UUID);
        } else {
            tag.putUUID(VerityIntroDataKeys.ENTITY_UUID, uuid);
        }
    }

    public static void setGreetingPlayed(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.GREETING_PLAYED, value);
    }

    public static boolean isGreetingPlayed(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.GREETING_PLAYED);
    }

    public static void setGreetingCompleted(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.GREETING_COMPLETED, value);
    }

    public static boolean isGreetingCompleted(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.GREETING_COMPLETED);
    }

    public static boolean isPendingSpawn(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.PENDING_SPAWN);
    }

    public static void setPendingSpawn(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.PENDING_SPAWN, value);
    }

    public static int getSpawnRetryCount(Player player) {
        return get(player).getInt(VerityIntroDataKeys.SPAWN_RETRY_COUNT);
    }

    public static void setSpawnRetryCount(Player player, int value) {
        get(player).putInt(VerityIntroDataKeys.SPAWN_RETRY_COUNT, value);
    }

    public static void resetIntroduction(ServerPlayer player) {
        CompoundTag tag = get(player);
        tag.putBoolean(VerityIntroDataKeys.INTRO_STARTED, false);
        tag.putBoolean(VerityIntroDataKeys.INTRO_COMPLETED, false);
        tag.remove(VerityIntroDataKeys.BOX_UUID);
        tag.putBoolean(VerityIntroDataKeys.REVEAL_STARTED, false);
        tag.putBoolean(VerityIntroDataKeys.REVEAL_COMPLETED, false);
        tag.remove(VerityIntroDataKeys.ENTITY_UUID);
        tag.putBoolean(VerityIntroDataKeys.GREETING_PLAYED, false);
        tag.putBoolean(VerityIntroDataKeys.GREETING_COMPLETED, false);
        tag.putBoolean(VerityIntroDataKeys.PENDING_SPAWN, false);
        tag.putInt(VerityIntroDataKeys.SPAWN_RETRY_COUNT, 0);
        tag.putString(VerityIntroDataKeys.INTRO_STAGE, "");
        tag.putInt(VerityIntroDataKeys.PROGRESSION_VERSION, VerityIntroDataKeys.CURRENT_PROGRESSION_VERSION);
    }

    public static void markRevealComplete(ServerPlayer player, UUID verityUuid) {
        setRevealStarted(player, true);
        setRevealCompleted(player, true);
        setIntroCompleted(player, true);
        setVerityUuid(player, verityUuid);
        setBoxUuid(player, null);
    }
}
