package com.universeexe.verity.quest;

import com.universeexe.verity.entity.VerityEntity;
import com.universeexe.verity.trust.VerityTrustManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

/**
 * Routes simulated speech through the same server quest handlers as recognized voice.
 */
public final class VerityQuestSpeechSimulator {
    private VerityQuestSpeechSimulator() {
    }

    public static boolean simulate(ServerPlayer player, String rawText) {
        String phrase = normalize(rawText);
        if (phrase.isBlank()) {
            return false;
        }
        Optional<VerityEntity> verity = VerityTrustManager.findOwnedVerity(player);
        if (verity.isEmpty()) {
            return false;
        }
        VerityEntity entity = verity.get();

        if (isMakeSoundPhrase(phrase)) {
            if (!VerityQuestManager.isQuest2Complete(player)) {
                return false;
            }
            VerityQuestManager.handleMakeSoundIntent(player, entity, phrase);
            return true;
        }

        if (VerityQuestManager.isQuest3Complete(player)
                && VerityQuestManager.tryHandleQ3ResponsePhrase(player, entity, phrase)) {
            return true;
        }

        if (isHelloPhrase(phrase)) {
            if (!VerityQuestManager.isQuest1Complete(player)) {
                return false;
            }
            VerityQuestManager.handleHelloIntent(player, entity, phrase);
            return true;
        }

        if (!VerityQuestManager.isQuest3Complete(player)
                && VerityQuestManager.tryHandleQ3ResponsePhrase(player, entity, phrase)) {
            return true;
        }

        return false;
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.toLowerCase(Locale.ROOT).trim();
        normalized = normalized.replace('’', '\'');
        normalized = normalized.replaceAll("[^a-z0-9\\s']", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private static boolean isHelloPhrase(String phrase) {
        if (phrase.isBlank()) {
            return false;
        }
        return phrase.equals("hello")
                || phrase.equals("hi")
                || phrase.equals("hey")
                || phrase.contains("hello verity")
                || phrase.contains("hi verity")
                || phrase.contains("hey verity")
                || phrase.contains("hello there verity")
                || phrase.contains("good morning verity")
                || phrase.contains("good evening verity")
                || phrase.equals("verity hello")
                || phrase.equals("verity hi")
                || phrase.equals("verity hey");
    }

    private static boolean isMakeSoundPhrase(String phrase) {
        return phrase.contains("make a sound")
                || phrase.contains("make a noise")
                || phrase.contains("can you make a sound")
                || phrase.contains("do something funny")
                || phrase.equals("verity make a sound");
    }
}
