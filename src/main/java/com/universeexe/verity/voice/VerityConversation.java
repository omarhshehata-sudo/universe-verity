package com.universeexe.verity.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered multi-step voice script (HELLO whisper chain, countdown warnings, etc.).
 */
public final class VerityConversation {
    private final String id;
    private final VerityVoiceCategory category;
    private final List<VerityConversationStep> steps;

    public VerityConversation(String id, VerityVoiceCategory category, List<VerityConversationStep> steps) {
        this.id = id;
        this.category = category;
        this.steps = List.copyOf(steps);
    }

    public String id() {
        return id;
    }

    public VerityVoiceCategory category() {
        return category;
    }

    public List<VerityConversationStep> steps() {
        return steps;
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    public static VerityConversation fromJson(String conversationId, JsonObject obj) {
        VerityVoiceCategory category = VerityVoiceCategory.fromManifest(
                obj.has("category") ? obj.get("category").getAsString() : null);
        List<VerityConversationStep> steps = new ArrayList<>();
        if (obj.has("steps")) {
            JsonArray array = obj.getAsJsonArray("steps");
            for (int i = 0; i < array.size(); i++) {
                steps.add(VerityConversationStep.fromJson(array.get(i).getAsJsonObject()));
            }
        }
        return new VerityConversation(conversationId, category, Collections.unmodifiableList(steps));
    }
}
