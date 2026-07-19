package com.universeexe.verity.quest;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.data.VerityPlayerData;
import com.universeexe.verity.registry.VerityItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the shipped FTB Quests chapter template and local quest flag alignment.
 */
public final class VerityBookAudit {
    private VerityBookAudit() {
    }

    public static List<String> runReport() {
        List<String> lines = new ArrayList<>();
        lines.add("=== Verity Quest Book Audit ===");
        lines.add("Template: ftb_quests/chapter_verity.snbt");
        lines.add("Chapter ID: " + VerityQuestIds.CHAPTER_ID);
        lines.add("Filename: " + VerityQuestIds.CHAPTER_FILENAME);
        lines.add("FTB Quests mod loaded: " + VerityFtbQuestBridge.isFtbPresent());
        lines.add("");

        Path snbt = Path.of("ftb_quests/chapter_verity.snbt");
        if (Files.exists(snbt)) {
            try {
                String text = Files.readString(snbt);
                checkContains(lines, text, "title: \"VERITY\"", "Chapter title VERITY");
                checkContains(lines, text, "verity_meet_verity", "Quest 1 ID");
                checkContains(lines, text, "verity_say_hello", "Quest 2 ID");
                checkContains(lines, text, "verity_make_a_sound", "Quest 3 ID");
                checkContains(lines, text, "open_verity_box", "Task open_verity_box");
                checkContains(lines, text, "say_hello_to_verity", "Task say_hello_to_verity");
                checkContains(lines, text, "make_sound_with_verity", "Task make_sound_with_verity");
                checkContains(lines, text, "dependencies: [\"verity_meet_verity\"]", "Q2 depends Q1");
                checkContains(lines, text, "dependencies: [\"verity_say_hello\"]", "Q3 depends Q2");
                checkContains(lines, text, "universe_verity:verity_box_quest_icon", "Q1 custom icon");
                checkContains(lines, text, "universe_verity:verity_voice_quest_icon", "Q2 custom icon");
                checkContains(lines, text, "universe_verity:verity_sound_quest_icon", "Q3 custom icon");
                checkContains(lines, text, "verity_chapter_background.png", "Chapter background");
                checkContains(lines, text, "x: 0.0d", "Q1 x=0");
                checkContains(lines, text, "x: 4.0d", "Q2 x=4");
                checkContains(lines, text, "x: 8.0d", "Q3 x=8");
                checkContains(lines, text, "y: 1.0d", "Q3 y=1");
                checkContains(lines, text, "shape: \"hexagon\"", "Q1 hexagon");
                checkContains(lines, text, "shape: \"circle\"", "Q2 circle");
                checkContains(lines, text, "shape: \"rsquare\"", "Q3 rsquare");
                checkContains(lines, text, "xp: 50", "Q1 XP 50");
                checkContains(lines, text, "xp: 35", "Q2 XP 35");
                checkContains(lines, text, "xp: 40", "Q3 XP 40");
                checkContains(lines, text, "{@pagebreak}", "Completion pagebreak lore");
            } catch (Exception ex) {
                lines.add("SNBT read failed: " + ex.getMessage());
            }
        } else {
            lines.add("SNBT template missing at " + snbt.toAbsolutePath());
        }

        lines.add("");
        lines.add("-- Custom icon items registered --");
        lines.add("verity_box_quest_icon: " + VerityItems.VERITY_BOX_QUEST_ICON.isPresent());
        lines.add("verity_voice_quest_icon: " + VerityItems.VERITY_VOICE_QUEST_ICON.isPresent());
        lines.add("verity_sound_quest_icon: " + VerityItems.VERITY_SOUND_QUEST_ICON.isPresent());

        Path bg = Path.of("src/main/resources/assets/universe_verity/textures/gui/quests/verity_chapter_background.png");
        lines.add("Background PNG on disk: " + Files.exists(bg));

        lines.add("");
        lines.add("Note: Cream/gold palette styling uses FTB chapter images + SNBT lore.");
        lines.add("FTB Quests does not expose per-node pulse CSS; locked nodes rely on FTB defaults.");
        lines.add("");
        lines.add("=== End Quest Book Audit ===");
        return lines;
    }

    public static void sendReport(ServerPlayer player) {
        for (String line : runReport()) {
            player.sendSystemMessage(Component.literal(line));
        }
    }

    public static void syncProgress(ServerPlayer player) {
        VerityFtbQuestBridge.reconcileAll(player);
        player.sendSystemMessage(Component.literal("[Verity] FTB quest progress reconciled from local flags."));
        UniverseVerity.LOGGER.info("[VerityQuest] book sync for {}", player.getGameProfile().getName());
    }

    public static List<String> playerStatus(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("Local Q1: " + VerityPlayerData.isQuest1Complete(player) + " (revealed=" + VerityPlayerData.isVerityRevealed(player) + ")");
        lines.add("Local Q2: " + VerityPlayerData.isQuest2Complete(player) + " (greeted=" + VerityPlayerData.isVerityGreeted(player) + ")");
        lines.add("Local Q3: " + VerityPlayerData.isQuest3Complete(player));
        lines.add("FTB bridge: " + (VerityFtbQuestBridge.isFtbPresent() ? "active" : "absent (local flags only)"));
        return lines;
    }

    private static void checkContains(List<String> lines, String haystack, String needle, String label) {
        lines.add((haystack.contains(needle) ? "OK  " : "FAIL") + " " + label);
    }
}
