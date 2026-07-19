package com.universeexe.verity.client.subtitle;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Styles vanilla accessibility subtitles for Verity dialogue (yellow + smiley).
 */
@OnlyIn(Dist.CLIENT)
public final class VeritySubtitleHud {
    @Nullable
    private static Component activeStyledSubtitle;
    private static int activeTicksRemaining;

    private VeritySubtitleHud() {
    }

    public static void show(Component styledSubtitle, int durationTicks) {
        if (styledSubtitle == null || styledSubtitle.equals(Component.empty())) {
            return;
        }
        activeStyledSubtitle = styledSubtitle;
        activeTicksRemaining = Math.max(1, durationTicks);
    }

    public static void clear() {
        activeStyledSubtitle = null;
        activeTicksRemaining = 0;
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ResourceLocation location = event.getSound().getLocation();
        if (!UniverseVerity.MOD_ID.equals(location.getNamespace())) {
            return;
        }
        String soundId = location.getPath();
        if (!VeritySubtitles.isVerityDialogueSound(soundId)) {
            return;
        }
        activeStyledSubtitle = VeritySubtitles.fromKey(VeritySounds.subtitleKeyFor(soundId));
        activeTicksRemaining = 60;
    }

    @SubscribeEvent
    public static void onSubtitlePre(RenderGuiOverlayEvent.Pre event) {
        if (!VanillaGuiOverlay.SUBTITLES.type().equals(event.getOverlay()) || activeStyledSubtitle == null) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onSubtitlePost(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.SUBTITLES.type().equals(event.getOverlay())) {
            return;
        }
        if (activeTicksRemaining > 0) {
            activeTicksRemaining--;
        } else {
            activeStyledSubtitle = null;
            return;
        }
        if (activeStyledSubtitle == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        Component subtitle = activeStyledSubtitle;
        int textWidth = mc.font.width(subtitle);
        int x = (width - textWidth) / 2;
        int y = height - 59;
        graphics.drawString(mc.font, subtitle, x, y, 0xFFFFFF, true);
    }
}
