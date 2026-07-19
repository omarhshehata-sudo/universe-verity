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
        CompoundTag root = persistent.getCompound(VerityIntroDataKeys.ROOT);
        migrateLegacyProgression(player, root);
        return root;
    }

    private static void migrateLegacyProgression(Player player, CompoundTag root) {
        if (root.getBoolean(VerityIntroDataKeys.REVEAL_COMPLETED) && !root.getBoolean(VerityIntroDataKeys.Q1_QUEST_COMPLETE)) {
            root.putBoolean(VerityIntroDataKeys.Q1_QUEST_COMPLETE, true);
            root.putBoolean(VerityIntroDataKeys.VERITY_REVEALED, true);
            if (!root.contains(VerityIntroDataKeys.VERITY_RELATIONSHIP)) {
                root.putString(VerityIntroDataKeys.VERITY_RELATIONSHIP, "new");
            }
        }
        if (root.getBoolean(VerityIntroDataKeys.GREETING_COMPLETED) && !root.getBoolean(VerityIntroDataKeys.Q2_QUEST_COMPLETE)) {
            root.putBoolean(VerityIntroDataKeys.Q2_QUEST_COMPLETE, true);
            root.putBoolean(VerityIntroDataKeys.VERITY_GREETED, true);
        }
        if (root.getBoolean("verity_made_sound") && !root.getBoolean(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE)) {
            root.putBoolean(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE, true);
        }
        if (root.getBoolean("verity_q03_complete") && !root.getBoolean(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE)) {
            root.putBoolean(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE, true);
        }
        if (root.getBoolean(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE) && !root.getBoolean(VerityIntroDataKeys.Q3_QUEST_COMPLETE)) {
            root.putBoolean(VerityIntroDataKeys.Q3_QUEST_COMPLETE, true);
        }
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

    public static boolean hasPlayedHelloWhisper(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.HELLO_WHISPER_PLAYED);
    }

    public static void setHelloWhisperPlayed(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.HELLO_WHISPER_PLAYED, value);
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
        tag.putBoolean(VerityIntroDataKeys.HELLO_WHISPER_PLAYED, false);
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

    public static boolean isVerityRevealed(Player player) {
        CompoundTag tag = get(player);
        if (tag.contains(VerityIntroDataKeys.VERITY_REVEALED)) {
            return tag.getBoolean(VerityIntroDataKeys.VERITY_REVEALED);
        }
        return isRevealCompleted(player);
    }

    public static void setVerityRevealed(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.VERITY_REVEALED, value);
        if (value) {
            setRevealCompleted(player, true);
        }
    }

    public static String getRelationship(Player player) {
        return get(player).getString(VerityIntroDataKeys.VERITY_RELATIONSHIP);
    }

    public static void setRelationship(Player player, String value) {
        if (value == null || value.isBlank()) {
            get(player).remove(VerityIntroDataKeys.VERITY_RELATIONSHIP);
        } else {
            get(player).putString(VerityIntroDataKeys.VERITY_RELATIONSHIP, value);
        }
    }

    public static boolean isVerityGreeted(Player player) {
        CompoundTag tag = get(player);
        if (tag.contains(VerityIntroDataKeys.VERITY_GREETED)) {
            return tag.getBoolean(VerityIntroDataKeys.VERITY_GREETED);
        }
        return isGreetingCompleted(player);
    }

    public static void setVerityGreeted(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.VERITY_GREETED, value);
        setGreetingCompleted(player, value);
    }

    public static boolean isVoiceKnowledgeQuestioned(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.VERITY_VOICE_KNOWLEDGE_QUESTIONED);
    }

    public static void setVoiceKnowledgeQuestioned(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.VERITY_VOICE_KNOWLEDGE_QUESTIONED, value);
    }

    public static int getFamiliarity(Player player) {
        return get(player).getInt(VerityIntroDataKeys.VERITY_FAMILIARITY);
    }

    public static void addFamiliarity(Player player, int delta) {
        CompoundTag tag = get(player);
        tag.putInt(VerityIntroDataKeys.VERITY_FAMILIARITY, Math.max(0, tag.getInt(VerityIntroDataKeys.VERITY_FAMILIARITY) + delta));
    }

    public static int getQ1BoxLinesPlayed(Player player) {
        return get(player).getInt(VerityIntroDataKeys.Q1_BOX_LINES_PLAYED);
    }

    public static void markQ1BoxLinePlayed(Player player, int lineBit) {
        CompoundTag tag = get(player);
        tag.putInt(VerityIntroDataKeys.Q1_BOX_LINES_PLAYED, tag.getInt(VerityIntroDataKeys.Q1_BOX_LINES_PLAYED) | lineBit);
    }

    public static boolean hasQ1BoxLinePlayed(Player player, int lineBit) {
        return (getQ1BoxLinesPlayed(player) & lineBit) != 0;
    }

    public static int getQ1RarePairsPlayed(Player player) {
        return get(player).getInt(VerityIntroDataKeys.Q1_RARE_PAIRS_PLAYED);
    }

    public static void markQ1RarePairPlayed(Player player, int pairBit) {
        CompoundTag tag = get(player);
        tag.putInt(VerityIntroDataKeys.Q1_RARE_PAIRS_PLAYED, tag.getInt(VerityIntroDataKeys.Q1_RARE_PAIRS_PLAYED) | pairBit);
    }

    public static boolean hasQ1RarePairPlayed(Player player, int pairBit) {
        return (getQ1RarePairsPlayed(player) & pairBit) != 0;
    }

    public static boolean isQuest1Complete(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.Q1_QUEST_COMPLETE);
    }

    public static void setQuest1Complete(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.Q1_QUEST_COMPLETE, value);
    }

    public static boolean isQuest2Complete(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.Q2_QUEST_COMPLETE);
    }

    public static void setQuest2Complete(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.Q2_QUEST_COMPLETE, value);
    }

    public static long getQ2FollowupUntil(Player player) {
        return get(player).getLong(VerityIntroDataKeys.Q2_FOLLOWUP_UNTIL);
    }

    public static void setQ2FollowupUntil(Player player, long gameTime) {
        get(player).putLong(VerityIntroDataKeys.Q2_FOLLOWUP_UNTIL, gameTime);
    }

    public static long getQ2LastRepeatGameTime(Player player) {
        return get(player).getLong(VerityIntroDataKeys.Q2_LAST_REPEAT_GAME_TIME);
    }

    public static void setQ2LastRepeatGameTime(Player player, long gameTime) {
        get(player).putLong(VerityIntroDataKeys.Q2_LAST_REPEAT_GAME_TIME, gameTime);
    }

    public static boolean isQuest3Complete(Player player) {
        CompoundTag tag = get(player);
        if (tag.contains(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE)) {
            return tag.getBoolean(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE);
        }
        return tag.getBoolean(VerityIntroDataKeys.Q3_QUEST_COMPLETE);
    }

    public static void setQuest3Complete(Player player, boolean value) {
        CompoundTag tag = get(player);
        tag.putBoolean(VerityIntroDataKeys.VERITY_SOUND_QUEST_COMPLETE, value);
        tag.putBoolean(VerityIntroDataKeys.VERITY_MADE_SOUND, value);
        tag.putBoolean(VerityIntroDataKeys.Q3_QUEST_COMPLETE, value);
    }

    public static long getQ3LastRareRequestGameTime(Player player) {
        return get(player).getLong(VerityIntroDataKeys.Q3_LAST_RARE_REQUEST_GAME_TIME);
    }

    public static void setQ3LastRareRequestGameTime(Player player, long gameTime) {
        get(player).putLong(VerityIntroDataKeys.Q3_LAST_RARE_REQUEST_GAME_TIME, gameTime);
    }

    public static boolean hasHeardUnknownSound(Player player) {
        return get(player).getBoolean(VerityIntroDataKeys.VERITY_HEARD_UNKNOWN_SOUND);
    }

    public static void setHeardUnknownSound(Player player, boolean value) {
        get(player).putBoolean(VerityIntroDataKeys.VERITY_HEARD_UNKNOWN_SOUND, value);
    }
}
