package com.universeexe.verity.client.voice;

import com.universeexe.verity.network.VoiceDebugSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Optional voice director debug overlay (disabled by default).
 */
@OnlyIn(Dist.CLIENT)
public final class VoiceDirectorDebugOverlay {
    private static boolean enabled;
    private static String currentLine = "";
    private static String category = "";
    private static int priority;
    private static int queueSize;
    private static int trust;
    private static String mood = "";
    private static String recentLine = "";
    private static String contextSummary = "";

    private VoiceDirectorDebugOverlay() {
    }

    public static void apply(VoiceDebugSyncPacket packet) {
        enabled = packet.enabled();
        currentLine = packet.currentLine();
        category = packet.category();
        priority = packet.priority();
        queueSize = packet.queueSize();
        trust = packet.trust();
        mood = packet.mood();
        recentLine = packet.recentLine();
        contextSummary = packet.contextSummary();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int x = 6;
        int y = 6;
        int color = 0xAAFFFF55;
        g.drawString(mc.font, "Verity Voice Director", x, y, color, true);
        y += 10;
        g.drawString(mc.font, "Line: " + currentLine, x, y, 0xFFFFFFFF, true);
        y += 10;
        g.drawString(mc.font, "Cat/Pri: " + category + " / " + priority, x, y, 0xFFFFFFFF, true);
        y += 10;
        g.drawString(mc.font, "Queue: " + queueSize + "  Trust: " + trust + "  Mood: " + mood, x, y, 0xFFFFFFFF, true);
        y += 10;
        g.drawString(mc.font, "Recent: " + recentLine, x, y, 0xFFFFFFFF, true);
        y += 10;
        g.drawString(mc.font, contextSummary, x, y, 0xFFCCCCCC, true);
    }
}
