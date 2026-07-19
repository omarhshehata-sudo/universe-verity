package com.universeexe.verity.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.universeexe.verity.UniverseVerity;
import com.universeexe.verity.registry.VeritySounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only audit of Quest 1–3 voice assets, registrations, and subtitles.
 */
public final class VerityVoiceAudit {
    private static final List<String> EXPECTED_Q3_MISSING = List.of(
            "cat_02", "imitation_01", "repeat_beep_01"
    );

    private VerityVoiceAudit() {
    }

    public static List<String> runReport() {
        List<String> lines = new ArrayList<>();
        lines.add("=== Verity Voice Audit ===");
        lines.add("Mod: " + UniverseVerity.MOD_ID + " (Quest 1–3 scope)");
        lines.add("");

        auditRegistrations(lines);
        auditSoundsJson(lines);
        auditOggFiles(lines);
        auditSubtitles(lines);
        auditQ3Missing(lines);
        auditManifest(lines);

        lines.add("");
        lines.add("=== End Voice Audit ===");
        return lines;
    }

    public static void sendReport(ServerPlayer player) {
        for (String line : runReport()) {
            player.sendSystemMessage(Component.literal(line));
        }
    }

    private static void auditRegistrations(List<String> lines) {
        lines.add("-- SoundEvent registrations (quest-scoped) --");
        int questSounds = 0;
        List<String> missingSubtitle = new ArrayList<>();
        for (var entry : VeritySounds.all().entrySet()) {
            String id = entry.getKey();
            if (!id.contains(".q01.") && !id.contains(".q02.") && !id.contains(".q03.")) {
                continue;
            }
            questSounds++;
            String subtitle = VeritySounds.subtitleKeyFor(id);
            if (subtitle == null || subtitle.isBlank()) {
                missingSubtitle.add(id);
            }
        }
        lines.add("Quest-scoped SoundEvents: " + questSounds);
        if (missingSubtitle.isEmpty()) {
            lines.add("Missing subtitle keys: none");
        } else {
            lines.add("Missing subtitle keys: " + missingSubtitle.size());
            missingSubtitle.forEach(s -> lines.add("  ! " + s));
        }
        lines.add("");
    }

    private static void auditSoundsJson(List<String> lines) {
        lines.add("-- sounds.json cross-check --");
        try (var reader = new InputStreamReader(
                UniverseVerity.class.getResourceAsStream("/assets/universe_verity/sounds.json"),
                StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Set<String> jsonKeys = root.keySet();
            Set<String> registeredQuest = VeritySounds.all().keySet().stream()
                    .filter(id -> id.contains(".q01.") || id.contains(".q02.") || id.contains(".q03."))
                    .collect(Collectors.toSet());

            List<String> registeredNotInJson = registeredQuest.stream()
                    .filter(id -> !jsonKeys.contains(id))
                    .sorted()
                    .toList();
            List<String> jsonNotRegistered = jsonKeys.stream()
                    .filter(k -> k.contains(".q01.") || k.contains(".q02.") || k.contains(".q03."))
                    .filter(k -> !registeredQuest.contains(k))
                    .sorted()
                    .toList();

            lines.add("Registered but missing sounds.json: " + registeredNotInJson.size());
            registeredNotInJson.forEach(s -> lines.add("  ! " + s));
            lines.add("sounds.json entries without registration: " + jsonNotRegistered.size());
            jsonNotRegistered.forEach(s -> lines.add("  ! " + s));

            int mp3Refs = 0;
            for (String key : jsonKeys) {
                if (key.contains(".mp3")) {
                    mp3Refs++;
                }
            }
            lines.add("MP3 references in sounds.json: " + mp3Refs + (mp3Refs == 0 ? " (OK)" : " (BAD)"));
        } catch (Exception ex) {
            lines.add("sounds.json audit failed: " + ex.getMessage());
        }
        lines.add("");
    }

    private static void auditOggFiles(List<String> lines) {
        lines.add("-- OGG files on disk --");
        Path soundsRoot = Path.of("src/main/resources/assets/universe_verity/sounds/verity");
        Map<String, Long> sizes = new LinkedHashMap<>();
        if (Files.isDirectory(soundsRoot)) {
            try {
                Files.walk(soundsRoot)
                        .filter(p -> p.toString().endsWith(".ogg"))
                        .sorted()
                        .forEach(p -> {
                            try {
                                sizes.put(soundsRoot.relativize(p).toString().replace('\\', '/'), Files.size(p));
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ex) {
                lines.add("Walk failed: " + ex.getMessage());
            }
        } else {
            lines.add("Dev path not found (runtime JAR audit uses registrations only).");
        }

        long q01 = sizes.keySet().stream().filter(k -> k.startsWith("q01/")).count();
        long q02 = sizes.keySet().stream().filter(k -> k.startsWith("q02/")).count();
        long q03 = sizes.keySet().stream().filter(k -> k.startsWith("q03/")).count();
        lines.add("q01 OGG count: " + q01);
        lines.add("q02 OGG count: " + q02);
        lines.add("q03 OGG count: " + q03);

        List<String> zeroByte = sizes.entrySet().stream()
                .filter(e -> e.getValue() <= 0)
                .map(Map.Entry::getKey)
                .toList();
        lines.add("Zero-byte OGG files: " + zeroByte.size());
        zeroByte.forEach(s -> lines.add("  ! " + s));
        lines.add("");
    }

    private static void auditSubtitles(List<String> lines) {
        lines.add("-- Subtitle lang keys --");
        try (var reader = new InputStreamReader(
                UniverseVerity.class.getResourceAsStream("/assets/universe_verity/lang/en_us.json"),
                StandardCharsets.UTF_8)) {
            JsonObject lang = JsonParser.parseReader(reader).getAsJsonObject();
            List<String> missing = new ArrayList<>();
            for (String id : VeritySounds.all().keySet()) {
                if (!id.contains(".q01.") && !id.contains(".q02.") && !id.contains(".q03.")) {
                    continue;
                }
                String key = VeritySounds.subtitleKeyFor(id);
                if (key != null && !key.isBlank() && !lang.has(key)) {
                    missing.add(key + " (" + id + ")");
                }
            }
            lines.add("Missing en_us subtitle entries: " + missing.size());
            missing.forEach(s -> lines.add("  ! " + s));
        } catch (Exception ex) {
            lines.add("Lang audit failed: " + ex.getMessage());
        }
        lines.add("");
    }

    private static void auditQ3Missing(List<String> lines) {
        lines.add("-- Known missing Quest 3 sources --");
        Path q03 = Path.of("src/main/resources/assets/universe_verity/sounds/verity/q03");
        for (String name : EXPECTED_Q3_MISSING) {
            Path file = q03.resolve(name + ".ogg");
            boolean exists = Files.exists(file);
            lines.add((exists ? "OK  " : "MISS") + " verity/q03/" + name + ".ogg"
                    + (exists ? "" : " — step skipped in handler"));
        }
        lines.add("");
    }

    private static void auditManifest(List<String> lines) {
        lines.add("-- Voice manifest conversations (quest_01/02/03.json) --");
        for (String file : List.of("quest_01.json", "quest_02.json", "quest_03.json")) {
            try (var reader = new InputStreamReader(
                    UniverseVerity.class.getResourceAsStream("/data/universe_verity/verity_voice/" + file),
                    StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int conv = root.has("conversations") ? root.getAsJsonObject("conversations").size() : 0;
                int pools = root.has("pools") ? root.getAsJsonObject("pools").size() : 0;
                lines.add(file + ": conversations=" + conv + " pools=" + pools);
            } catch (Exception ex) {
                lines.add(file + ": UNREADABLE (" + ex.getMessage() + ")");
            }
        }
        lines.add("Duplicate SoundEvent IDs: "
                + (ForgeRegistries.SOUND_EVENTS.getKeys().size() == new HashSet<>(ForgeRegistries.SOUND_EVENTS.getKeys()).size()
                ? "none detected" : "check registry"));
    }
}
