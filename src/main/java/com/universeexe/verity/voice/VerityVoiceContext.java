package com.universeexe.verity.voice;

import com.universeexe.verity.entity.VerityEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;

/**
 * Playback anchor, audience, and cached contextual snapshot for a queued voice event.
 */
public record VerityVoiceContext(
        @Nullable ServerPlayer owner,
        int anchorEntityId,
        double x,
        double y,
        double z,
        boolean ownerOnly,
        SoundSource soundSource,
        boolean actionBarMessage,
        @Nullable Runnable onStart,
        @Nullable Runnable onComplete,
        @Nullable VerityVoiceSnapshot snapshot
) {
    public static VerityVoiceContext atEntity(
            @Nullable ServerPlayer owner,
            int entityId,
            double x,
            double y,
            double z,
            SoundSource source,
            boolean ownerOnly
    ) {
        VerityVoiceSnapshot snapshot = null;
        if (owner != null) {
            VerityEntity verity = entityId >= 0 && owner.serverLevel().getEntity(entityId) instanceof VerityEntity v
                    ? v : null;
            snapshot = VerityVoiceSnapshot.capture(owner, verity);
        }
        return new VerityVoiceContext(owner, entityId, x, y, z, ownerOnly, source, ownerOnly, null, null, snapshot);
    }

    public VerityVoiceContext withOnStart(@Nullable Runnable onStart) {
        return new VerityVoiceContext(owner, anchorEntityId, x, y, z, ownerOnly, soundSource, actionBarMessage, onStart, onComplete, snapshot);
    }

    public VerityVoiceContext withOnComplete(@Nullable Runnable onComplete) {
        return new VerityVoiceContext(owner, anchorEntityId, x, y, z, ownerOnly, soundSource, actionBarMessage, onStart, onComplete, snapshot);
    }

    public VerityVoiceContext withSnapshot(@Nullable VerityVoiceSnapshot snapshot) {
        return new VerityVoiceContext(owner, anchorEntityId, x, y, z, ownerOnly, soundSource, actionBarMessage, onStart, onComplete, snapshot);
    }
}
