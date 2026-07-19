package com.universeexe.verity.voice;

import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.trust.VerityTrustKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player voice memory: session recency plus persistent flags/counters in player NBT.
 */
public final class VerityVoiceMemory {
    public static final int MAX_RECENT = 10;
    public static final int MAX_IDLE_RECENT = 3;
    public static final int MAX_CONVERSATIONS = 5;

    private static final String ROOT = "VerityVoiceMemory";
    private static final String RECENT_SOUNDS = "RecentSounds";
    private static final String IDLE_RECENT = "IdleRecent";
    private static final String CONVERSATIONS = "RecentConversations";
    private static final String SOUND_LAST = "SoundLast_";
    private static final String SOUND_COUNT = "SoundCount_";
    private static final String SOUND_DAILY = "SoundDaily_";
    private static final String CATEGORY_DAILY = "CatDaily_";
    private static final String DAILY_DAY = "VoiceDailyDay";

    private final UUID playerId;
    private final Deque<String> recentVariantIds = new ArrayDeque<>();
    private final Set<String> recentSet = new HashSet<>();
    private final Deque<String> recentIdle = new ArrayDeque<>();
    private final Deque<String> recentConversations = new ArrayDeque<>();

    private VerityVoiceMemory(UUID playerId) {
        this.playerId = playerId;
    }

    private static final Map<UUID, VerityVoiceMemory> BY_PLAYER = new java.util.concurrent.ConcurrentHashMap<>();

    public static VerityVoiceMemory forPlayer(UUID playerId) {
        return BY_PLAYER.computeIfAbsent(playerId, VerityVoiceMemory::new);
    }

    public static VerityVoiceMemory forPlayer(ServerPlayer player) {
        VerityVoiceMemory memory = forPlayer(player.getUUID());
        memory.ensureLoaded(player);
        return memory;
    }

    public static void clearPlayer(UUID playerId) {
        BY_PLAYER.remove(playerId);
    }

    private CompoundTag voiceTag(ServerPlayer player) {
        CompoundTag root = VerityPlayerData.get(player);
        if (!root.contains(ROOT)) {
            root.put(ROOT, new CompoundTag());
        }
        return root.getCompound(ROOT);
    }

    private void ensureLoaded(ServerPlayer player) {
        CompoundTag tag = voiceTag(player);
        if (tag.getBoolean("Loaded")) {
            return;
        }
        loadRecent(tag.getList(RECENT_SOUNDS, ListTag.TAG_STRING), recentVariantIds, recentSet, MAX_RECENT);
        loadRecent(tag.getList(IDLE_RECENT, ListTag.TAG_STRING), recentIdle, new HashSet<>(), MAX_IDLE_RECENT);
        for (int i = 0; i < tag.getList(CONVERSATIONS, ListTag.TAG_STRING).size(); i++) {
            recentConversations.addLast(tag.getList(CONVERSATIONS, ListTag.TAG_STRING).getString(i));
        }
        tag.putBoolean("Loaded", true);
    }

    private static void loadRecent(ListTag list, Deque<String> deque, Set<String> set, int max) {
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            deque.addLast(id);
            set.add(id);
        }
        while (deque.size() > max) {
            String removed = deque.removeFirst();
            set.remove(removed);
        }
    }

    private void persist(ServerPlayer player) {
        CompoundTag tag = voiceTag(player);
        tag.put(RECENT_SOUNDS, toListTag(recentVariantIds));
        tag.put(IDLE_RECENT, toListTag(recentIdle));
        tag.put(CONVERSATIONS, toListTag(recentConversations));
        tag.putBoolean("Loaded", true);
    }

    private static ListTag toListTag(Deque<String> deque) {
        ListTag list = new ListTag();
        for (String id : deque) {
            list.add(StringTag.valueOf(id));
        }
        return list;
    }

    private void ensureDailyBucket(ServerPlayer player, CompoundTag tag) {
        long day = player.serverLevel().getDayTime() / VerityTrustKeys.DAY_TICKS;
        if (tag.getLong(DAILY_DAY) != day) {
            tag.putLong(DAILY_DAY, day);
            List<String> remove = new ArrayList<>();
            for (String key : tag.getAllKeys()) {
                if (key.startsWith(SOUND_DAILY) || key.startsWith(CATEGORY_DAILY)) {
                    remove.add(key);
                }
            }
            for (String key : remove) {
                tag.remove(key);
            }
        }
    }

    public void remember(ServerPlayer player, String variantOrSoundId, VerityVoiceCategory category) {
        if (variantOrSoundId == null || variantOrSoundId.isBlank()) {
            return;
        }
        ensureLoaded(player);
        pushRecent(recentVariantIds, recentSet, variantOrSoundId, MAX_RECENT);
        if (category == VerityVoiceCategory.IDLE) {
            pushRecent(recentIdle, new HashSet<>(), variantOrSoundId, MAX_IDLE_RECENT);
        }

        CompoundTag tag = voiceTag(player);
        ensureDailyBucket(player, tag);
        long now = player.serverLevel().getGameTime();
        tag.putLong(SOUND_LAST + variantOrSoundId, now);
        tag.putInt(SOUND_COUNT + variantOrSoundId, tag.getInt(SOUND_COUNT + variantOrSoundId) + 1);
        tag.putInt(SOUND_DAILY + variantOrSoundId, tag.getInt(SOUND_DAILY + variantOrSoundId) + 1);
        tag.putInt(CATEGORY_DAILY + category.name(), tag.getInt(CATEGORY_DAILY + category.name()) + 1);
        persist(player);
    }

    public void rememberConversation(ServerPlayer player, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        ensureLoaded(player);
        recentConversations.remove(conversationId);
        recentConversations.addLast(conversationId);
        while (recentConversations.size() > MAX_CONVERSATIONS) {
            recentConversations.removeFirst();
        }
        persist(player);
    }

    private static void pushRecent(Deque<String> deque, Set<String> set, String id, int max) {
        if (set.remove(id)) {
            deque.remove(id);
        }
        deque.addLast(id);
        set.add(id);
        while (deque.size() > max) {
            String removed = deque.removeFirst();
            set.remove(removed);
        }
    }

    public boolean recentlyPlayed(String variantOrSoundId) {
        return recentSet.contains(variantOrSoundId);
    }

    public boolean recentlyPlayedIdle(String variantOrSoundId) {
        return recentIdle.contains(variantOrSoundId);
    }

    public long lastPlayedAt(ServerPlayer player, String variantOrSoundId) {
        return voiceTag(player).getLong(SOUND_LAST + variantOrSoundId);
    }

    public int lifetimeCount(ServerPlayer player, String variantOrSoundId) {
        return voiceTag(player).getInt(SOUND_COUNT + variantOrSoundId);
    }

    public int dailyCount(ServerPlayer player, String variantOrSoundId) {
        CompoundTag tag = voiceTag(player);
        ensureDailyBucket(player, tag);
        return tag.getInt(SOUND_DAILY + variantOrSoundId);
    }

    public int dailyCategoryCount(ServerPlayer player, VerityVoiceCategory category) {
        CompoundTag tag = voiceTag(player);
        ensureDailyBucket(player, tag);
        return tag.getInt(CATEGORY_DAILY + category.name());
    }

    public boolean isOnCooldown(ServerPlayer player, VerityVoiceVariant variant, long gameTime) {
        if (variant.cooldownTicks() <= 0) {
            return false;
        }
        long last = lastPlayedAt(player, variant.id());
        return last > 0 && gameTime - last < variant.cooldownTicks();
    }

    public boolean exceedsMaxPlays(ServerPlayer player, VerityVoiceVariant variant) {
        if (variant.maxPlays() < 0) {
            return false;
        }
        return lifetimeCount(player, variant.id()) >= variant.maxPlays();
    }

    public void setFlag(ServerPlayer player, VerityVoiceMemoryFlag flag) {
        CompoundTag tag = voiceTag(player);
        tag.putBoolean(flag.nbtKey(), true);
        tag.putLong(flag.lastKey(), player.serverLevel().getGameTime());
        tag.putInt(flag.countKey(), tag.getInt(flag.countKey()) + 1);
    }

    public boolean hasFlag(ServerPlayer player, VerityVoiceMemoryFlag flag) {
        return voiceTag(player).getBoolean(flag.nbtKey());
    }

    public int flagCount(ServerPlayer player, VerityVoiceMemoryFlag flag) {
        return voiceTag(player).getInt(flag.countKey());
    }

    public boolean callbackPlayed(ServerPlayer player, VerityVoiceMemoryFlag flag) {
        return voiceTag(player).getBoolean(flag.callbackKey());
    }

    public void markCallbackPlayed(ServerPlayer player, VerityVoiceMemoryFlag flag) {
        voiceTag(player).putBoolean(flag.callbackKey(), true);
    }

    public List<String> recentHistory() {
        return List.copyOf(recentVariantIds);
    }

    public List<String> recentIdleHistory() {
        return List.copyOf(recentIdle);
    }

    public List<String> recentConversationHistory() {
        return List.copyOf(recentConversations);
    }

    public Map<String, Long> cooldownSnapshot(ServerPlayer player, long gameTime) {
        Map<String, Long> out = new HashMap<>();
        CompoundTag tag = voiceTag(player);
        for (String key : tag.getAllKeys()) {
            if (key.startsWith(SOUND_LAST)) {
                String sound = key.substring(SOUND_LAST.length());
                long last = tag.getLong(key);
                out.put(sound, Math.max(0, gameTime - last));
            }
        }
        return out;
    }

    public List<String> memoryFlagSummary(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        for (VerityVoiceMemoryFlag flag : VerityVoiceMemoryFlag.values()) {
            if (hasFlag(player, flag)) {
                lines.add(flag.name() + " count=" + flagCount(player, flag)
                        + " callback=" + callbackPlayed(player, flag));
            }
        }
        return lines;
    }

    public void clearSession() {
        recentVariantIds.clear();
        recentSet.clear();
        recentIdle.clear();
        recentConversations.clear();
    }
}
