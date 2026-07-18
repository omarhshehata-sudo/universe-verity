package com.universeexe.verity.client.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 50-tick box-drop / reveal bounce curves from verity-5.7.2 {@code VerityAnimation}.
 */
@OnlyIn(Dist.CLIENT)
public final class VerityBounceAnimation {
    public static final int DURATION_TICKS = 50;

    private VerityBounceAnimation() {
    }

    public static float getYOffset(float tick) {
        if (tick < 0.0f || tick > DURATION_TICKS) {
            return 0.0f;
        }
        if (tick <= 12.0f) {
            float p = tick / 12.0f;
            return 1.0f - p * p * p;
        }
        if (tick <= 30.0f) {
            float p = (tick - 12.0f) / 18.0f;
            return 4.0f * p * (1.0f - p) * 0.5f;
        }
        if (tick <= 42.0f) {
            float p = (tick - 30.0f) / 12.0f;
            return 4.0f * p * (1.0f - p) * 0.15f;
        }
        return 0.0f;
    }

    public static float getScaleY(float tick) {
        if (tick < 0.0f || tick > DURATION_TICKS) {
            return 1.0f;
        }
        if (tick <= 12.0f) {
            float p = tick / 12.0f;
            return 1.0f + p * p * 0.3f;
        }
        if (tick <= 18.0f) {
            float p = (tick - 12.0f) / 6.0f;
            return 1.0f - (float) Math.sin(p * Math.PI) * 0.6f;
        }
        if (tick >= 30.0f && tick <= 36.0f) {
            float p = (tick - 30.0f) / 6.0f;
            return 1.0f - (float) Math.sin(p * Math.PI) * 0.3f;
        }
        if (tick >= 42.0f && tick <= 46.0f) {
            float p = (tick - 42.0f) / 4.0f;
            return 1.0f - (float) Math.sin(p * Math.PI) * 0.1f;
        }
        return 1.0f;
    }

    public static float getScaleXZ(float tick) {
        float scaleY = getScaleY(tick);
        return 1.0f + (1.0f - scaleY) * 0.75f;
    }
}
