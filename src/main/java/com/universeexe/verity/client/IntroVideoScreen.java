package com.universeexe.verity.client;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Title-screen frame intro from verity-5.7.2 (248 frames @ 24fps).
 * Skipped automatically when FancyMenu is present (see {@link TitleIntroHandler}).
 */
@OnlyIn(Dist.CLIENT)
public class IntroVideoScreen extends Screen {
    public static final int TOTAL_FRAMES = 248;
    private static final int FPS = 24;

    private final Screen previousScreen;
    private long startTime;
    private boolean videoStarted;

    public IntroVideoScreen(Screen previousScreen) {
        super(Component.literal("Verity Intro"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();
        if (!videoStarted) {
            startTime = System.currentTimeMillis();
            if (minecraft != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(VeritySounds.INTRO_VIDEO_AUDIO.get(), 1.0f));
            }
            videoStarted = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        long elapsedMillis = System.currentTimeMillis() - startTime;
        int currentFrame = (int) (elapsedMillis * FPS / 1000L) + 1;
        if (currentFrame > TOTAL_FRAMES) {
            skip();
            return;
        }
        ResourceLocation frameLoc = new ResourceLocation(UniverseVerity.MOD_ID,
                String.format("textures/intro/frame_%04d.png", currentFrame));
        float aspect = 16f / 9f;
        int drawW = this.width;
        int drawH = (int) (this.width / aspect);
        if (drawH > this.height) {
            drawH = this.height;
            drawW = (int) (this.height * aspect);
        }
        int drawX = (this.width - drawW) / 2;
        int drawY = (this.height - drawH) / 2;
        guiGraphics.blit(frameLoc, drawX, drawY, 0, 0, drawW, drawH, drawW, drawH);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            skip();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void skip() {
        if (minecraft != null) {
            minecraft.getSoundManager().stop(
                    new ResourceLocation(UniverseVerity.MOD_ID, "verity.intro.video_audio"),
                    SoundSource.MASTER);
            minecraft.setScreen(previousScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
