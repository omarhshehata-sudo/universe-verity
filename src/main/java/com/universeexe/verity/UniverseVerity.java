package com.universeexe.verity;

import com.universeexe.verity.command.VerityCommands;
import com.universeexe.verity.config.VerityClientConfig;
import com.universeexe.verity.config.VerityCommonConfig;
import com.universeexe.verity.event.PlayerDataHandler;
import com.universeexe.verity.event.PlayerIntroductionHandler;
import com.universeexe.verity.event.VerityItemInteractions;
import com.universeexe.verity.network.VerityNetwork;
import com.universeexe.verity.quest.VerityQuest3Handler;
import com.universeexe.verity.trust.VerityTrustEvents;
import com.universeexe.verity.registry.VerityEntities;
import com.universeexe.verity.registry.VerityItems;
import com.universeexe.verity.registry.VeritySounds;
import com.universeexe.verity.util.VerityAssetValidator;
import com.universeexe.verity.voice.VerityVoiceEvents;
import com.universeexe.verity.voice.VerityVoiceScheduler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(UniverseVerity.MOD_ID)
public class UniverseVerity {
    public static final String MOD_ID = "universe_verity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UniverseVerity() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        VerityEntities.ENTITY_TYPES.register(modBus);
        VerityItems.ITEMS.register(modBus);
        VeritySounds.SOUND_EVENTS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VerityCommonConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, VerityClientConfig.SPEC);

        modBus.addListener(this::commonSetup);
        modBus.addListener(VerityEntities::registerAttributes);

        MinecraftForge.EVENT_BUS.register(new PlayerIntroductionHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerDataHandler());
        MinecraftForge.EVENT_BUS.register(new VerityItemInteractions());
        MinecraftForge.EVENT_BUS.register(new VerityTrustEvents());
        MinecraftForge.EVENT_BUS.register(new VerityVoiceScheduler());
        MinecraftForge.EVENT_BUS.register(new VerityVoiceEvents());
        MinecraftForge.EVENT_BUS.register(new VerityQuest3Handler());
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        com.universeexe.verity.quest.VerityFtbQuestBridge.bootstrap();

        if (FMLEnvironment.dist.isClient()) {
            com.universeexe.verity.client.VerityClient.init(modBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            VerityNetwork.register();
            VerityAssetValidator.validate();
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        VerityCommands.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Universe: Verity loaded (sealed-box intro + reveal + voice director)");
        // Install VERITY chapter into this instance's FTB Quests config (CurseForge/Prism).
        com.universeexe.verity.quest.VerityFtbQuestInstaller.installIfNeeded(event.getServer());
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        VerityVoiceEvents.registerReloadListener(event);
    }
}
