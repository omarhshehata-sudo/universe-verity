package com.universeexe.verity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Procedural UV sphere used by Verity ball / held-item rendering (ported from verity-5.7.2).
 */
@OnlyIn(Dist.CLIENT)
public final class SphereMesh {
    private static final float TEXTURE_SCALE = 1.3f;

    private SphereMesh() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                              float radius, int latitudes, int longitudes, int packedLight,
                              int r, int g, int b, int a) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        drawSphere(pose, normal, consumer, radius, latitudes, longitudes, packedLight, r, g, b, a);
    }

    private static void drawSphere(Matrix4f pose, Matrix3f normal, VertexConsumer consumer, float radius,
                                   int latitudes, int longitudes, int light, int r, int g, int b, int a) {
        for (int lat = 0; lat <= latitudes; lat++) {
            float theta1 = (float) (lat * Math.PI / latitudes);
            float theta2 = (float) ((lat + 1) * Math.PI / latitudes);
            for (int lon = 0; lon <= longitudes; lon++) {
                float phi1 = (float) (lon * 2 * Math.PI / longitudes);
                float phi2 = (float) ((lon + 1) * 2 * Math.PI / longitudes);
                addVertex(pose, normal, consumer, radius, theta1, phi1, light, r, g, b, a);
                addVertex(pose, normal, consumer, radius, theta2, phi1, light, r, g, b, a);
                addVertex(pose, normal, consumer, radius, theta2, phi2, light, r, g, b, a);
                addVertex(pose, normal, consumer, radius, theta1, phi2, light, r, g, b, a);
            }
        }
    }

    private static void addVertex(Matrix4f pose, Matrix3f normalMatrix, VertexConsumer consumer, float radius,
                                  float theta, float phi, int light, int r, int g, int b, int a) {
        float x = (float) (radius * Math.sin(theta) * Math.cos(phi));
        float y = (float) (radius * Math.cos(theta));
        float z = (float) (radius * Math.sin(theta) * Math.sin(phi));
        float u = (float) (phi / (Math.PI * 2));
        float v = 0.25f + (float) (theta / Math.PI) * 0.5f;
        u = Mth.clamp(0.5f + (u - 0.5f) * TEXTURE_SCALE, 0.0f, 1.0f);
        v = Mth.clamp(0.5f + (v - 0.5f) * TEXTURE_SCALE, 0.0f, 1.0f);
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        float nx = x / length;
        float ny = y / length;
        float nz = z / length;
        consumer.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }
}
