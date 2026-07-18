package com.universeexe.verity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.universeexe.verity.client.model.VerityBoxModel;
import com.universeexe.verity.entity.VerityBoxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VerityBoxRenderer extends GeoEntityRenderer<VerityBoxEntity> {
    /** Extra visual scale on top of the 2x geo (approx. one-block crate). */
    private static final float RENDER_SCALE = 1.15f;

    public VerityBoxRenderer(EntityRendererProvider.Context context) {
        super(context, new VerityBoxModel());
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(VerityBoxEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
