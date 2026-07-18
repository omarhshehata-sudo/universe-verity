package com.universeexe.verity.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VerityClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PLAY_TITLE_INTRO_VIDEO;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("title_intro");
        PLAY_TITLE_INTRO_VIDEO = builder
                .comment("Play the 248-frame Verity title intro (skipped automatically if FancyMenu is loaded).")
                .define("playTitleIntroVideo", true);
        builder.pop();
        SPEC = builder.build();
    }

    private VerityClientConfig() {
    }
}
