package com.universeexe.verity.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Derives visual roll rotation from horizontal movement distance.
 * Face readability is handled separately via face_root counter-tilt in the renderer.
 */
@OnlyIn(Dist.CLIENT)
public final class VerityRollCalculator {
    public enum SpeedBand {
        IDLE,
        SLOW,
        NORMAL,
        FAST
    }

    private float rollDegrees;
    private float settleWobble;
    private boolean wasMoving;

    public VerityRollCalculator() {
    }

    public void tick(Entity entity, float bodyRadiusBlocks) {
        Vec3 delta = entity.getDeltaMovement();
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        boolean moving = horizontal > 0.002D;

        if (moving) {
            // arc length / radius → degrees
            float radius = Math.max(0.15f, bodyRadiusBlocks);
            float add = (float) ((horizontal / radius) * (180.0F / Math.PI));
            rollDegrees = Mth.wrapDegrees(rollDegrees + add);
            wasMoving = true;
            settleWobble = 0.0f;
        } else if (wasMoving) {
            // one small settling wobble after stop
            settleWobble = 6.0f;
            wasMoving = false;
        } else if (settleWobble > 0.05f) {
            settleWobble *= 0.82f;
        } else {
            settleWobble = 0.0f;
        }
    }

    public float getRollDegrees() {
        return rollDegrees + (float) Math.sin(settleWobble) * settleWobble * 0.35f;
    }

    public float getFaceCounterTilt() {
        // Keep face mostly readable; allow slight lean into roll.
        return Mth.clamp(-getRollDegrees() * 0.08f, -12.0f, 12.0f);
    }

    public SpeedBand bandFor(Entity entity) {
        Vec3 delta = entity.getDeltaMovement();
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 0.002D) {
            return SpeedBand.IDLE;
        }
        if (horizontal < 0.08D) {
            return SpeedBand.SLOW;
        }
        if (horizontal < 0.18D) {
            return SpeedBand.NORMAL;
        }
        return SpeedBand.FAST;
    }

    public void reset() {
        rollDegrees = 0.0f;
        settleWobble = 0.0f;
        wasMoving = false;
    }
}
