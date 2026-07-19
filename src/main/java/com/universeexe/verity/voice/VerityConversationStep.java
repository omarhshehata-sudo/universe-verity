package com.universeexe.verity.voice;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

/**
 * One step in a multi-line {@link VerityConversation}.
 * Steps reference either a manifest pool or an inline variant (sound + duration).
 */
public record VerityConversationStep(
        @Nullable String poolId,
        @Nullable VerityVoiceVariant inlineVariant,
        int pauseAfterTicks
) {
    public static VerityConversationStep fromJson(JsonObject obj) {
        int pause = obj.has("pause_after_ticks") ? obj.get("pause_after_ticks").getAsInt() : 0;
        if (obj.has("pool")) {
            return new VerityConversationStep(obj.get("pool").getAsString(), null, pause);
        }
        VerityVoiceVariant inline = VerityVoiceVariant.fromJson("inline", obj);
        return new VerityConversationStep(null, inline, pause);
    }
}
