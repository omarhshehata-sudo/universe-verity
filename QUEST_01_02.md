# Quest 1 & 2 — MEET VERITY! / SAY HELLO

Version: **universe-verity 1.0.26**, **universe-verity-voice 1.0.12**

## Overview

| Quest | ID | Completion trigger | XP | Trust |
|-------|-----|-------------------|-----|-------|
| MEET VERITY! | `verity_meet_verity` | Q1 intro voice finishes (or fallback) + `verity_revealed=true` | 50 | `OPENED_BOX` once |
| SAY HELLO | `verity_say_hello` | First `universe_verity_voice:hello` after Q1 → `quest_02_first_greeting` ends | 35 | `FIRST_GREETING` once |

Quest 3 stub (`verity_make_a_sound`) is locked in `ftb_quests/chapter_verity.snbt`.

## Player flags (`universe_verity` NBT root)

| Key | Type | Set when |
|-----|------|----------|
| `verity_revealed` | bool | Q1 complete |
| `verity_relationship` | string | `"new"` after Q1 |
| `verity_greeted` | bool | Q2 complete |
| `verity_voice_knowledge_questioned` | bool | Optional Q2 follow-up conversation |
| `verity_familiarity` | int | +1 on Q2 |
| `VerityQ1BoxLinesPlayed` | int bitmask | Waiting lines |
| `VerityQ1RarePairsPlayed` | int bitmask | Rare sealed-box pairs |
| `VerityQ1QuestComplete` / `VerityQ2QuestComplete` | bool | Quest rewards fired |

Legacy keys (`VerityRevealCompleted`, `VerityGreetingCompleted`) migrate forward on load.

## Sound ID migration (old → new)

Reuses existing OGG where dialogue matches; legacy IDs remain registered.

| New ID | Source OGG | Status |
|--------|------------|--------|
| `verity.q01.box.hello` | `box/voice/hellooo.ogg` | **Reuse** |
| `verity.q01.box.anyone_there` | `box/voice/is_someone_out_there.ogg` | **Reuse** |
| `verity.q01.box.hear_moving` | `box/voice/i_can_hear_you_moving.ogg` | **Reuse** |
| `verity.q01.box.open_request` | `box/voice/could_you_open_this.ogg` | **Reuse** |
| `verity.q01.box.please` | `box/voice/please.ogg` | **Reuse** |
| `verity.q01.box.still_there_01` | `box/voice/youre_still_there.ogg` | **Reuse** |
| `verity.q01.box.still_there_02` | copy of still_there | **Placeholder VO** |
| `verity.q01.box.open_myself_01/02` | copy of please | **Placeholder VO** |
| `verity.q01.reveal.oh` | copy of hellooo | **Placeholder VO** |
| `verity.q01.reveal.found_opening` | `box/voice/oh_you_found_the_opening.ogg` | **Reuse** |
| `verity.q01.intro.*` | `voice/greeting_personal_helper.ogg` (split TBD) | **Placeholder VO** (split lines) |
| `verity.q02.first.hello/hoping` | hello_hoping clip | **Reuse / partial** |
| `verity.q02.first.imagined_voice` | `your_voice_sounds_exactly.ogg` | **Reuse** |
| `verity.q02.first.didnt_imagine` | `i_mean_imagined_it.ogg` | **Reuse** |
| `verity.q02.first.already_knew` | hello_again | **Placeholder VO** |
| `verity.q02.first.nice_to_meet` | hello_hoping | **Placeholder VO** |
| `verity.q02.followup.*` | greeting_personal_helper / hello_again | **Placeholder VO** |
| `verity.q02.repeat.hello_again/hi` | hello_again | **Reuse** |
| `verity.q02.repeat.no_intro_*`, `likes_name`, `happier` | hello_hoping copies | **Placeholder VO** |

Legacy IDs (`verity.box.*`, `verity.voice.*`) unchanged for companion/follow paths.

## Voice Director data

- `data/universe_verity/verity_voice/quest_01.json` — intro ending pool (85/12/3 weights)
- `data/universe_verity/verity_voice/quest_02.json` — first greeting, knowledge follow-up, repeat pool

## Q1 flow

1. Owner right-clicks sealed box (blocks re-interaction while revealing).
2. `VerityBoxSequence.stopWaitingDialogue` interrupts box intro voice.
3. Reveal cinematic (~1s settle via `greetingDelayTicks`, then shake/open/spawn).
4. `VerityQuestManager.beginQuest1Intro` plays reveal + intro via Voice Director (owner-only).
5. On event complete: flags, trust, XP, advancement, FTB command bridge.

## Q2 flow

1. Requires `VerityQuestManager.isQuest1Complete`.
2. PTT hello intent (`maximum_distance: 8`), server validates owner + proximity.
3. First hello → `quest_02_first_greeting` conversation → quest complete + 12s follow-up window.
4. Knowledge phrases in window → `quest_02_knowledge_followup`.
5. Later hellos → weighted `quest_02_repeat_greeting` pool (5s min gap, happier 1/day).

## FTB Quests

- Chapter template: `ftb_quests/chapter_verity.snbt` (copy into modpack `config/ftbquests/quests/chapters/`).
- Runtime completion: `VerityFtbQuestBridge` runs `ftbquests change_progress <player> complete <quest_id>` when FTB Quests is loaded.

## Placeholder art

- `assets/universe_verity/textures/gui/quests/verity_box_icon.png` — gold hex Q1 placeholder
- `assets/universe_verity/textures/gui/quests/verity_voice_icon.png` — cyan Q2 placeholder

Replace with final quest icons when art is ready.

## Regression notes

- **Box open:** waiting dialogue pauses beyond 8 blocks; stops on reveal; intro no longer completes on click alone.
- **Hello:** routed through Quest 2 pools; first greeting blocks repeats until complete; polite greeting trust only on repeat hellos (daily via existing `POLITE_GREETING`).
- **Found opening:** restored in Q1 intro after `verity.q01.reveal.oh`.

See also `VOICE_DIRECTOR.md` for director architecture.
