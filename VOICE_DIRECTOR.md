# Verity Voice Director

Server-authoritative voice playback for **Universe: Verity** (Forge 1.20.1). All gameplay voice lines route through `VerityVoiceDirector`; clients only play sounds and subtitles from validated S2C packets.

## Architecture

```
Gameplay hook (trust, box intro, entity cue, voice mod)
        │
        ▼
VerityVoiceDirector  ──► priority queue per player
        │
        ▼
PlayVoicePacket (S2C)  ──► client plays registered sound + subtitle
        │
        ▼
PlaybackStatusPacket (C2S)  ──► director advances conversations / clears busy
```

### Packages

| Package | Role |
|---------|------|
| `com.universeexe.verity.voice` | Categories, priorities, pools, conversations, manifest loader, director, scheduler, snapshot, memory |
| `com.universeexe.verity.network` | `SimpleChannel`, S2C play/interrupt/debug, C2S playback status |
| `com.universeexe.verity.client.voice` | Optional debug overlay (off by default) |

### Priority (higher preempts lower)

| Category | Priority | Notes |
|----------|----------|-------|
| TRANSFORM / COUNTDOWN | 100 | Countdown interrupts everything except an active transform line |
| QUEST / BOX_INTRO | 90 | Sealed-box intro + story beats |
| DANGER | 80 | Hostile-nearby triggers (pool empty until assets added) |
| COMMAND_SUCCESS / FAILURE | 70 | Reserved for command feedback |
| ACK | 65 | Reserved |
| TRUST / MOOD | 60 | Mood-change lines |
| PLAYER_INTERACTION | 50 | FOLLOW / HELLO companion lines |
| ENV | 45 | Biome/time ambience (empty pool) |
| MEMORY / MONSTER | 40 | Post-transform demon ambience |
| IDLE | 20 | Never interrupts |

## Manifest JSON

Loaded from `data/universe_verity/verity_voice/*.json` on datapack reload.

```json
{
  "pools": {
    "companion_follow": {
      "category": "PLAYER_INTERACTION",
      "silent_weight": 0,
      "variants": [
        {
          "id": "lead_the_way",
          "sound": "verity.voice.lead_the_way",
          "duration_ticks": 26,
          "subtitle": "subtitles.universe_verity.verity.lead_the_way",
          "weight": 40,
          "minimum_trust": -10,
          "maximum_trust": 10,
          "moods": ["HAPPY", "FRIENDLY"],
          "cooldown_ticks": 2400,
          "maximum_plays": -1,
          "rare": false
        },
        {
          "id": "silent_stare",
          "silent": true,
          "duration_ticks": 20,
          "weight": 10
        }
      ]
    }
  }
}
```

### Variant fields

| Field | Purpose |
|-------|---------|
| `weight` | Weighted random selection (default 1) |
| `silent` | No audio; optional pause / expression-only outcome |
| `silent_weight` (pool) | Pool-level SILENT outcome weight |
| `minimum_trust` / `maximum_trust` | Trust filter |
| `moods` | Allowed mood list |
| `required_memory` / `forbidden_memory` | Persistent memory flags |
| `cooldown_ticks` | Per-variant repeat cooldown |
| `maximum_plays` | Lifetime cap (-1 = unlimited) |
| `rare` | Lower effective weight after first play |

Seeded manifests: `box_intro`, `companion`, `trust_mood`, `countdown`, `monster`. Empty placeholder pools: `quest_03`, `env`.

## Selection algorithm

1. Filter by trust, mood, memory flags, cooldowns, max plays
2. Remove last-10 recent variants
3. Boost weight for unheard lines; reduce for overplayed / rare lines
4. Weighted random (including optional SILENT)
5. Record in session + persistent NBT memory

## Persistent memory

`VerityVoiceMemory` stores flags in player persistent NBT under `VerityVoiceMemory` (survives reconnect):

- `PLAYER_OPENED_BOX`, `PLAYER_FIRST_GREETING`, `PLAYER_HIT_VERITY`, etc.
- Per-sound lifetime counts, daily counts, last-play timestamps
- Recent line history (bounded: 10 general, 3 idle, 5 conversations)

## Context snapshot

`VerityVoiceSnapshot` caches trust, mood, quest, weather, biome, distance, carried/following, countdown/transform state. Rebuilt at most once per tick per player — no full-world scans.

## Integration points

| Source | Director API |
|--------|----------------|
| `VerityTrustManager` | `requestPool`, `requestConversation` for mood/countdown/monster |
| `VerityBoxSequence.speak` | `requestDirect` (preserves timing + sound IDs) |
| `VerityEntity.playVoiceLine` / `playHelloVoiceResponse` | entity cue flush + `requestConversation` |
| `universe-verity-voice` FOLLOW/HELLO | unchanged — calls entity methods which route to director |

Sound IDs must exist in `VeritySounds.all()`. Clients reject unknown IDs.

## Debug commands

Requires OP level 2:

| Command | Purpose |
|---------|---------|
| `/verity voice status` | Current player queue / active line |
| `/verity voice queue` | List queued events |
| `/verity voice clear` | Clear queue and interrupt active line |
| `/verity voice play <event>` | Queue pool or conversation by id |
| `/verity voice playraw <soundEvent>` | Queue registered sound directly |
| `/verity voice pool <pool_id>` | Queue a manifest pool |
| `/verity voice context` | Print cached context snapshot |
| `/verity voice history` | Recent lines / conversations |
| `/verity voice memories` | Persistent memory flags |
| `/verity voice cooldowns` | Sound + ambient cooldown timestamps |
| `/verity voice interrupt` | Interrupt up to countdown priority |
| `/verity voice debug on` / `off` | Toggle client debug overlay + verbose logging |

Bonus: `/verity voice conversation <id>`, `/verity voice manifest`.

## Debug overlay

Off by default. When enabled via `/verity voice debug on`, shows current line, category, priority, queue size, trust, mood, recent line, and context summary in the top-left HUD.

## Networking

Channel: `universe_verity:voice` protocol `"1"`.

| Packet | Direction | Purpose |
|--------|-----------|---------|
| `PlayVoicePacket` | S2C | Positional sound + subtitle/action bar |
| `InterruptVoicePacket` | S2C | Stop active session |
| `PlaybackStatusPacket` | C2S | STARTED / FINISHED / INTERRUPTED |
| `VoiceDebugSyncPacket` | S2C | Debug overlay state |

Owner-only lines (trust, countdown whispers) send only to the owning player. Box/companion lines broadcast to entity tracking range.

## Protected assets

Do **not** change OGG files or sound event IDs for:

- Box intro: `VerityBoxSequence` / `verity.box.*`
- Companion: `VerityEntity.playVoiceLine` / `verity.voice.*` FOLLOW + HELLO

The director references these IDs from manifest JSON only.

## Adding Quest 3+ voice

1. Add OGG under `assets/universe_verity/sounds/verity/quest_03/`
2. Register sound in `VeritySounds.java` + `sounds.json` + `en_us.json` subtitles
3. Add variants to `data/universe_verity/verity_voice/quest_03.json`
4. Trigger via quest hook → `VerityVoiceDirector.requestPool` or `requestConversation`

## Intentionally stubbed

- `quest_03.json` pools — empty until Q3 assets exist
- `env.json` pools (`idle_ambient`, `env_ambient`, `danger_near_hostile`) — empty hooks
- Environmental event hooks (sunrise, village, diamonds) — scheduler stubs only via `VerityVoiceEvents`
- Conversation branches / player response windows — schema supports steps; branching not wired yet
- Facial cue fields in packets — not yet serialized (animation via entity callbacks only)
