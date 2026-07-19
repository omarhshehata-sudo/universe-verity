# Quest 3 — MAKE A SOUND

Version: **universe-verity 1.0.29**, **universe-verity-voice 1.0.14**

## Single completion path

```
MAKE_SOUND intent (voice mod)
  → VerityVoiceGameplayResponses.handleMakeSound
  → VerityQuestManager.handleMakeSoundIntent
  → VerityQuest3Handler (session, demos, response windows)
  → VerityQuestManager.completeQuest3   ← canonical: flags, 40 XP, advancement, FTB
```

No alternate conversation-only completion path. `quest_03_first_make_sound` is the **intro** conversation only; handler orchestrates demos and response branches after it.

## Overview

| Field | Value |
|-------|-------|
| Quest ID | `verity_make_a_sound` |
| Completion flag (canonical) | `verity_sound_quest_complete` |
| Mirror flags | `VerityQ3QuestComplete`, `verity_made_sound` |
| Depends on | Quest 2 (`verity_say_hello`) |
| XP | **40** (code + FTB SNBT reward) |
| Trust | `COMPLETED_VERITY_QUEST` on completion; optional `POSITIVE_RESPONSE` (+1, daily-capped) on first-time positive answer |
| Memory | `verity_heard_unknown_sound` after rare nighttime cave repeat |

## How to trigger

1. Complete Quest 1 (meet Verity) and Quest 2 (say hello).
2. Stand within 12 blocks of your Verity.
3. Hold push-to-talk and say a **make sound** phrase (wake word required unless using PTT-only mode):
   - `verity make a sound`
   - `make a sound` / `make a noise` / `do a sound`
   - `can you make a sound`
4. During response windows, answer with PTT using short phrases (`yes`, `no`, `that was good`, `do another one`, etc.) — matched via `universe_verity_voice:q3_answer` intent.

## Conversation IDs (`quest_03.json`)

| ID | Used by | Role |
|----|---------|------|
| `quest_03_first_make_sound` | `VerityQuest3Handler.playFirstMakeSound` | First-time intro only (`intro_01` → `intro_02`) |
| `quest_03_evaluation` | Manifest reference | Post-demo “Was that good?” |
| `quest_03_response_*` | Manifest reference | Positive / negative / another closers |
| `quest_03_cave_*` | Manifest reference | Cave yes / no / silence closers |
| `quest_03_cat_defense` | Manifest reference | Cat demo + negative response |
| `quest_03_repeat_opener` | Pool (weights in handler) | Repeat openers |

Runtime demo branches and response windows are orchestrated by **`VerityQuest3Handler`** via `VerityVoiceDirector.requestEvent` after the intro conversation completes.

## Sound inventory

OGG path: `assets/universe_verity/sounds/verity/q03/<name>.ogg`  
Sound event ID: `verity.q03.<name>`

| File | Sound ID | Source | Status |
|------|----------|--------|--------|
| sfx_tiny_beep.ogg | verity.q03.sfx_tiny_beep | **Generated** (original electronic beep) | OK |
| sfx_cave.ogg | verity.q03.sfx_cave | Desktop | OK |
| sfx_boom.ogg | verity.q03.sfx_boom | Desktop | OK |
| sfx_squeak.ogg | verity.q03.sfx_squeak | **Generated** (Desktop source not found on disk) | OK |
| sfx_fake_imitation.ogg | verity.q03.sfx_fake_imitation | **Generated** (Desktop source not found on disk) | OK |
| intro_01.ogg | verity.q03.intro_01 | Downloads MP3 | OK |
| intro_02.ogg | verity.q03.intro_02 | Downloads MP3 | OK |
| beep_01.ogg | verity.q03.beep_01 | Desktop MP3 | OK |
| beep_02.ogg | verity.q03.beep_02 | Downloads MP3 | OK |
| cat_01.ogg | verity.q03.cat_01 | Desktop MP3 | OK |
| cat_02.ogg | verity.q03.cat_02 | — | **MISSING** (step skipped in flow) |
| cat_defense_01/02.ogg | verity.q03.cat_defense_* | Downloads MP3 | OK |
| cave_01.ogg | verity.q03.cave_01 | Downloads MP3 | OK |
| cave_yes_01/02.ogg | verity.q03.cave_yes_* | Downloads MP3 | OK |
| cave_no_01.ogg | verity.q03.cave_no_01 | Downloads MP3 | OK |
| cave_silence_01.ogg | verity.q03.cave_silence_01 | Downloads MP3 | OK |
| boom_01.ogg | verity.q03.boom_01 | Downloads MP3 | OK |
| squeak_01.ogg | verity.q03.squeak_01 | Downloads MP3 | OK |
| imitation_01.ogg | verity.q03.imitation_01 | — | **MISSING** (step skipped after fake imitation SFX) |
| evaluation_01.ogg | verity.q03.evaluation_01 | Downloads MP3 | OK |
| response_*.ogg | verity.q03.response_* | Downloads MP3 | OK |
| repeat_request_*.ogg | verity.q03.repeat_request_* | Downloads MP3 | OK |
| repeat_beep_01.ogg | verity.q03.repeat_beep_01 | — | **MISSING** (repeat beep ends after beep_01) |
| repeat_cat/cave/boom/squeak/imitation_*.ogg | verity.q03.repeat_* | Downloads MP3 | OK |

**Still missing sources:** `cat_02`, `imitation_01`, `repeat_beep_01`, Desktop `sfx_squeak` / `sfx_fake_imitation` (searched Desktop, Downloads, Music, Spotlight).

## Flow summary

### First time

1. Intro via `quest_03_first_make_sound`: `intro_01` → 700ms → `intro_02` → 1s
2. Weighted demo (beep 20%, cat 20%, cave 20%, boom 15%, squeak 15%, imitation 10%)
3. Demo branch (SFX + dialogue per spec)
4. **Cave:** 8s window — yes / no / silence (`cave_silence_01` on timeout)
5. **Others:** `evaluation_01` → 10s window — positive / negative / another / silence (silence completes with no trust change)
6. Cat + negative → `cat_defense_01` → `cat_defense_02` (not normal negative branch)
7. Final response → `VerityQuestManager.completeQuest3` (flags, advancement, FTB, 40 XP)

### Repeat (after Q3 complete)

1. Weighted opener: 40% / 35% / 20% / 5% rare (1 MC day cooldown on rare)
2. Same six demos with repeat reaction lines
3. Rare nighttime cave: `repeat_cave_rare_01`, look toward dark, set `verity_heard_unknown_sound`, no follow-up sound

## Implementation files

| File | Role |
|------|------|
| `VerityQuest3Handler.java` | Session state, demos, response windows |
| `VerityQuestManager.java` | Intent delegation + **canonical `completeQuest3`** |
| `VerityQuest3Sounds.java` | Sound IDs + duration ticks |
| `VeritySounds.java` + `sounds.json` + `en_us.json` | Registration + subtitles |
| `advancements/quests/verity_make_a_sound.json` | Impossible trigger (code awards) |
| `ftb_quests/chapter_verity.snbt` | FTB node (depends Q2, 40 XP, custom task, lore) |
| `universe-verity-voice/.../make_sound.json` | Trigger intent |
| `universe-verity-voice/.../q3_answer.json` | Response-window phrases |
| `VerityVoiceGameplayResponses.java` | Routes intents to quest handler |

## Critical constraints

- **Never** capture or replay player microphone audio for imitation — only `verity.q03.sfx_fake_imitation`.
- Quest 1 / Quest 2 OGG files, sound IDs, and completion logic are unchanged.
- All Q3 dialogue routes through `VerityVoiceDirector`.
