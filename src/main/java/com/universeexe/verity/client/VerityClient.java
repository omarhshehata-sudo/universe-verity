package com.universeexe.verity.client;

import com.universeexe.verity.client.render.VerityBoxRenderer;
import com.universeexe.verity.client.render.VerityDemonRenderer;
import com.universeexe.verity.client.render.VerityRenderer;
import com.universeexe.verity.registry.VerityEntities;
import com.universeexe.verity.network.VerityVoiceClientPlayback;
import com.universeexe.verity.client.voice.VoiceDirectorDebugOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

@OnlyIn(Dist.CLIENT)
public final class VerityClient {
    private VerityClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(VerityClient::registerRenderers);
        MinecraftForge.EVENT_BUS.addListener(VerityClient::onClientTick);
        MinecraftForge.EVENT_BUS.register(VoiceDirectorDebugOverlay.class);
    }

    @OnlyIn(Dist.CLIENT)
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            VerityVoiceClientPlayback.clientTick();
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VerityEntities.VERITY_BOX.get(), VerityBoxRenderer::new);
        event.registerEntityRenderer(VerityEntities.VERITY.get(), VerityRenderer::new);
        event.registerEntityRenderer(VerityEntities.VERITY_DEMON.get(), VerityDemonRenderer::new);
    }
}
