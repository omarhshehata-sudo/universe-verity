package com.universeexe.verity.client;

import com.universeexe.verity.client.render.VerityBoxRenderer;
import com.universeexe.verity.client.render.VerityDemonRenderer;
import com.universeexe.verity.client.render.VerityRenderer;
import com.universeexe.verity.registry.VerityEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

@OnlyIn(Dist.CLIENT)
public final class VerityClient {
    private VerityClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(VerityClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VerityEntities.VERITY_BOX.get(), VerityBoxRenderer::new);
        event.registerEntityRenderer(VerityEntities.VERITY.get(), VerityRenderer::new);
        event.registerEntityRenderer(VerityEntities.VERITY_DEMON.get(), VerityDemonRenderer::new);
    }
}
