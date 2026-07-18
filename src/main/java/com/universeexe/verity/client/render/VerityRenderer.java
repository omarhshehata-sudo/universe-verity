package com.universeexe.verity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.universeexe.verity.client.util.VerityBounceAnimation;
import com.universeexe.verity.client.util.VerityRollCalculator;
import com.universeexe.verity.entity.VerityEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Procedural sphere + face texture renderer (ported from verity-5.7.2 SphereEntityRenderer).
 */
public class VerityRenderer extends EntityRenderer<VerityEntity> {
    private static final Map<VerityEntity, VerityRollCalculator> ROLL = new WeakHashMap<>();

    public VerityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.30f;
    }

    @Override
    public ResourceLocation getTextureLocation(VerityEntity entity) {
        return VerityFaceTextures.forEntity(entity);
    }

    @Override
    public void render(VerityEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (entity.tickCount < 3 && entity.clientBounceTicks < 0 && entity.getBounceStartTick() > 0) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        LocalPlayer player = Minecraft.getInstance().player;
        float stretchY = 1.0f;
        float stretchXZ = 1.0f;
        double visualYOffset = 0.0;
        boolean isAnimating = entity.clientBounceTicks >= 0;
        boolean isTalking = entity.isVisuallyTalking();

        if (isAnimating) {
            float exactTick = (float) (VerityEntity.BOUNCE_DURATION_TICKS - entity.clientBounceTicks) + partialTick;
            stretchY = VerityBounceAnimation.getScaleY(exactTick);
            stretchXZ = VerityBounceAnimation.getScaleXZ(exactTick);
            visualYOffset = VerityBounceAnimation.getYOffset(exactTick);
        } else if (isTalking && entity.clientIntroDelay <= 0) {
            float talkTime = (float) entity.tickCount + partialTick;
            stretchY = 1.0f + Mth.sin(talkTime * 0.6f) * 0.12f;
            stretchXZ = 1.0f + (1.0f - stretchY) * 0.5f;
        } else {
            // Subtle idle bob
            float idleTime = (float) entity.tickCount + partialTick;
            stretchY = 1.0f + Mth.sin(idleTime * 0.08f) * 0.03f;
            stretchXZ = 1.0f + (1.0f - stretchY) * 0.4f;
        }

        VerityRollCalculator roll = ROLL.computeIfAbsent(entity, e -> new VerityRollCalculator());
        roll.tick(entity, VerityEntity.TARGET_WIDTH * 0.5f);
        float rollAngle = roll.getRollDegrees();
        // Also accumulate JAR-style position roll for smoother look when moving.
        rollAngle += Mth.lerp(partialTick, entity.clientRollAngleO, entity.clientRollAngle);

        double entityX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yOld, entity.getY())
                + entity.getBbHeight() * 0.5 + visualYOffset;
        double entityZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        double viewerX = entityX;
        double viewerY = entityY + 1.0;
        double viewerZ = entityZ;
        if (player != null) {
            viewerX = Mth.lerp(partialTick, player.xOld, player.getX());
            viewerY = Mth.lerp(partialTick, player.yOld, player.getY()) + player.getEyeHeight();
            viewerZ = Mth.lerp(partialTick, player.zOld, player.getZ());
        }

        float bodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.pushPose();
        poseStack.translate(0.0, visualYOffset, 0.0);
        SphereRenderHelper.renderEntityBillboard(
                poseStack, bufferSource, getTextureLocation(entity), packedLight,
                entityX, entityY, entityZ, viewerX, viewerY, viewerZ,
                stretchY, stretchXZ, 255, 255, 255, 255, rollAngle, bodyYaw);
        poseStack.popPose();

        if (entity.isVisualDebug()) {
            renderVisualDebug(entity, poseStack, bufferSource, packedLight, roll);
        }
    }

    private void renderVisualDebug(VerityEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                   int light, VerityRollCalculator roll) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Vec3 delta = entity.getDeltaMovement();
        double speed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        String[] lines = {
                String.format(Locale.ROOT, "pos %.2f %.2f %.2f", entity.getX(), entity.getY(), entity.getZ()),
                String.format(Locale.ROOT, "box %.2fx%.2f ground=%s", entity.getBbWidth(), entity.getBbHeight(),
                        entity.onGround()),
                String.format(Locale.ROOT, "speed %.4f roll %.1f", speed, roll == null ? 0 : roll.getRollDegrees()),
                String.format(Locale.ROOT, "yaw body %.1f head %.1f", entity.yBodyRot, entity.getYHeadRot()),
                "anim " + entity.getSyncedAnimation() + " expr " + entity.getRenderExpression().id(),
                "face " + VerityFaceTextures.forEntity(entity),
                "bounce " + entity.clientBounceTicks + " talk=" + entity.isVisuallyTalking(),
        };
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.35D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.015f, -0.015f, 0.015f);
        Matrix4f mat = poseStack.last().pose();
        float y = 0;
        for (String line : lines) {
            float w = font.width(line) / 2.0f;
            font.drawInBatch(Component.literal(line), -w, y, 0xFFFFFF, false, mat, buffer,
                    Font.DisplayMode.NORMAL, 0x66000000, light);
            y += 10;
        }
        poseStack.popPose();
    }
}
