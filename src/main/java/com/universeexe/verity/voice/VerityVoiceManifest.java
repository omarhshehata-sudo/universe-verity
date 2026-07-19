package com.universeexe.verity.voice;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.universeexe.verity.UniverseVerity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads {@code data/universe_verity/verity_voice/*.json} voice manifests.
 */
public final class VerityVoiceManifest extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final VerityVoiceManifest INSTANCE = new VerityVoiceManifest();

    private final Map<String, VerityVoicePool> pools = new HashMap<>();
    private final Map<String, VerityConversation> conversations = new HashMap<>();

    private VerityVoiceManifest() {
        super(GSON, "verity_voice");
    }

    public static VerityVoiceManifest get() {
        return INSTANCE;
    }

    public Map<String, VerityVoicePool> pools() {
        return Collections.unmodifiableMap(pools);
    }

    public Map<String, VerityConversation> conversations() {
        return Collections.unmodifiableMap(conversations);
    }

    public Optional<VerityVoicePool> pool(String id) {
        return Optional.ofNullable(pools.get(id));
    }

    public Optional<VerityConversation> conversation(String id) {
        return Optional.ofNullable(conversations.get(id));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        pools.clear();
        conversations.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject root = entry.getValue().getAsJsonObject();
            if (root.has("pools")) {
                JsonObject poolObj = root.getAsJsonObject("pools");
                for (String poolId : poolObj.keySet()) {
                    VerityVoicePool pool = VerityVoicePool.fromJson(poolId, poolObj.getAsJsonObject(poolId));
                    if (pools.putIfAbsent(poolId, pool) != null) {
                        UniverseVerity.LOGGER.warn("[VerityVoice] Duplicate pool id {} in {}", poolId, entry.getKey());
                    }
                }
            }
            if (root.has("conversations")) {
                JsonObject convObj = root.getAsJsonObject("conversations");
                for (String convId : convObj.keySet()) {
                    VerityConversation conversation = VerityConversation.fromJson(convId, convObj.getAsJsonObject(convId));
                    if (conversations.putIfAbsent(convId, conversation) != null) {
                        UniverseVerity.LOGGER.warn("[VerityVoice] Duplicate conversation id {} in {}", convId, entry.getKey());
                    }
                }
            }
        }

        UniverseVerity.LOGGER.info(
                "[VerityVoice] Loaded {} pools and {} conversations from {} manifest files",
                pools.size(),
                conversations.size(),
                objects.size()
        );
    }
}
