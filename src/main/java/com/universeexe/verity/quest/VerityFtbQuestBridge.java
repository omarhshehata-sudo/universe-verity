package com.universeexe.verity.quest;

import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.data.VerityPlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Best-effort FTB Quests completion without a hard compile dependency.
 * Falls back silently when FTB Quests is absent.
 */
public final class VerityFtbQuestBridge {
    private static boolean ftbPresent;

    private VerityFtbQuestBridge() {
    }

    public static void bootstrap() {
        ftbPresent = ModList.get().isLoaded("ftbquests");
        if (ftbPresent) {
            UniverseVerity.LOGGER.info("[VerityQuest] FTB Quests detected — quest completion bridge enabled");
        }
    }

    public static boolean isFtbPresent() {
        return ftbPresent;
    }

    public static void completeQuest(ServerPlayer player, String questId) {
        if (player == null || questId == null || questId.isBlank() || !ftbPresent) {
            return;
        }
        if (player.getServer() == null) {
            return;
        }
        CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput().withPermission(2);
        String cmd = "ftbquests change_progress " + player.getGameProfile().getName() + " complete " + questId;
        try {
            player.getServer().getCommands().performPrefixedCommand(source, cmd);
            UniverseVerity.LOGGER.debug("[VerityQuest] FTB complete {} for {}", questId, player.getGameProfile().getName());
        } catch (Exception ex) {
            UniverseVerity.LOGGER.warn("[VerityQuest] FTB completion failed for {}: {}", questId, ex.getMessage());
        }
    }

    /** Align FTB progress with canonical local flags (safe no-op when FTB absent). */
    public static void reconcileAll(ServerPlayer player) {
        if (!ftbPresent || player == null) {
            return;
        }
        if (VerityPlayerData.isQuest1Complete(player) || VerityPlayerData.isVerityRevealed(player)) {
            completeQuest(player, VerityQuestIds.MEET_VERITY);
        }
        if (VerityPlayerData.isQuest2Complete(player) || VerityPlayerData.isVerityGreeted(player)) {
            completeQuest(player, VerityQuestIds.SAY_HELLO);
        }
        if (VerityPlayerData.isQuest3Complete(player)) {
            completeQuest(player, VerityQuestIds.MAKE_A_SOUND);
        }
    }
}
