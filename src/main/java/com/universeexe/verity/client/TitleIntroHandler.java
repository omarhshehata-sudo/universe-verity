package com.universeexe.verity.client;

import com.universeexe.verity.config.VerityClientConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Plays the JAR title intro once per session when opening vanilla {@link TitleScreen}.
 * Never injects when FancyMenu is loaded (isolation rule).
 */
@Mod.EventBusSubscriber(modid = com.universeexe.verity.UniverseVerity.MOD_ID, value = Dist.CLIENT)
public final class TitleIntroHandler {
    private static boolean hasPlayedIntro;

    private TitleIntroHandler() {
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (hasPlayedIntro) {
            return;
        }
        if (!(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        if (ModList.get().isLoaded("fancymenu")) {
            hasPlayedIntro = true;
            return;
        }
        if (!VerityClientConfig.PLAY_TITLE_INTRO_VIDEO.get()) {
            hasPlayedIntro = true;
            return;
        }
        hasPlayedIntro = true;
        event.setNewScreen(new IntroVideoScreen(event.getScreen()));
    }
}
