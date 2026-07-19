package com.universeexe.verity.quest;

import com.universeexe.verity.UniverseVerity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Ships the VERITY FTB Quests chapter into the instance config folder so the
 * quest book works without a manual SNBT copy step.
 *
 * <p>Only writes {@code chapters/verity.snbt}. Never touches {@code data.snbt}
 * or other chapter files — overwriting book metadata can hide pack chapters.
 *
 * Target (FTB Quests 1.20.1):
 *   config/ftbquests/quests/chapters/verity.snbt
 */
public final class VerityFtbQuestInstaller {
    private static final String EMBEDDED_CHAPTER =
            "/data/universe_verity/ftbquests/quests/chapters/verity.snbt";
    private static final String VERSION_MARKER = "universe_verity_chapter_version.txt";
    /** Bump when embedded SNBT content changes (forces chapter rewrite). */
    private static final String CHAPTER_CONTENT_VERSION = "3";

    private VerityFtbQuestInstaller() {
    }

    public static void installIfNeeded(MinecraftServer server) {
        if (!ModList.get().isLoaded("ftbquests")) {
            UniverseVerity.LOGGER.warn(
                    "[VerityQuest] FTB Quests is NOT installed — Verity chapter cannot appear in the quest book. "
                            + "Install ftbquests (and FTB Library / FTB Teams) in this CurseForge/Prism instance.");
            return;
        }

        Path questsRoot = FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests");
        Path chaptersDir = questsRoot.resolve("chapters");
        Path chapterFile = chaptersDir.resolve("verity.snbt");
        Path markerFile = questsRoot.resolve(VERSION_MARKER);

        try {
            Files.createDirectories(chaptersDir);

            String modVersion = ModList.get()
                    .getModContainerById(UniverseVerity.MOD_ID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");

            String markerExpected = modVersion + "|" + CHAPTER_CONTENT_VERSION;
            boolean needsWrite = !Files.isRegularFile(chapterFile)
                    || !Files.isRegularFile(markerFile)
                    || !markerExpected.equals(Files.readString(markerFile).trim());

            if (needsWrite) {
                writeResource(EMBEDDED_CHAPTER, chapterFile);
                Files.writeString(markerFile, markerExpected + "\n", StandardCharsets.UTF_8);
                UniverseVerity.LOGGER.info(
                        "[VerityQuest] Installed VERITY FTB chapter → {}",
                        chapterFile.toAbsolutePath());
                scheduleReload(server);
            } else {
                UniverseVerity.LOGGER.debug(
                        "[VerityQuest] VERITY FTB chapter already up to date at {}",
                        chapterFile.toAbsolutePath());
            }
        } catch (Exception ex) {
            UniverseVerity.LOGGER.error("[VerityQuest] Failed to install FTB Verity chapter", ex);
        }
    }

    private static void writeResource(String classpath, Path target) throws IOException {
        try (InputStream in = VerityFtbQuestInstaller.class.getResourceAsStream(classpath)) {
            if (in == null) {
                throw new IOException("Missing embedded resource: " + classpath);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void scheduleReload(MinecraftServer server) {
        if (server == null) {
            return;
        }
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                        "ftbquests reload");
                UniverseVerity.LOGGER.info("[VerityQuest] Ran /ftbquests reload after chapter install");
            } catch (Exception ex) {
                UniverseVerity.LOGGER.warn(
                        "[VerityQuest] Could not auto-reload FTB Quests ({}). Restart the instance once.",
                        ex.getMessage());
            }
        });
    }
}
