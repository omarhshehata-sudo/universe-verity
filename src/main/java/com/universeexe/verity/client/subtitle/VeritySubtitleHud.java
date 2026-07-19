package com.universeexe.verity.client.subtitle;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Always-on Verity dialogue subtitles (yellow + smiley), independent of Minecraft accessibility subtitles.
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
            UniverseVerity.LOGGER.warn("[VeritySubtitles] Ignored empty subtitle show()");
            return;
        }
        activeStyledSubtitle = styledSubtitle;
        activeTicksRemaining = Math.max(20, durationTicks);
        UniverseVerity.LOGGER.debug("[VeritySubtitles] Show for {} ticks", activeTicksRemaining);
    }

    public static void clear() {
        activeStyledSubtitle = null;
        activeTicksRemaining = 0;
    }

    public static boolean isActive() {
        return activeStyledSubtitle != null && activeTicksRemaining > 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || activeTicksRemaining <= 0) {
            return;
        }
        activeTicksRemaining--;
        if (activeTicksRemaining <= 0) {
            activeStyledSubtitle = null;
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ResourceLocation location = event.getSound().getLocation();
        if (!UniverseVerity.MOD_ID.equals(location.getNamespace())) {
            return;
        }
        String resolvedSoundId = VeritySounds.resolveSoundId(location);
        if (resolvedSoundId == null) {
            return;
        }
        String subtitleKey = VeritySounds.subtitleKeyFor(resolvedSoundId);
        if (!VeritySubtitles.isVerityDialogueKey(subtitleKey)) {
            return;
        }
        show(VeritySubtitles.fromKey(subtitleKey), 60);
    }

    /** Hide vanilla subtitles while our styled line is active so they do not stack or steal focus. */
    @SubscribeEvent
    public static void onSubtitlePre(RenderGuiOverlayEvent.Pre event) {
        if (VanillaGuiOverlay.SUBTITLES.type().equals(event.getOverlay()) && isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (activeStyledSubtitle == null || activeTicksRemaining <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || mc.getWindow() == null) {
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
