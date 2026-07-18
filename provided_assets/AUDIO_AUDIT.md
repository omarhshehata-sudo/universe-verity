# Verity Audio Audit

Generated: 2026-07-17

## Source inventory (Downloads)

| Source file | Duration | Channels | Archived |
|-------------|----------|----------|----------|
| `Verity-2026-07-17-02-51-[excited]-hello,-im-verity,-your-personal-helper.mp3` | 5.460 s | mono 44.1 kHz | `provided_assets/audio_originals/` |
| `Verity-2026-07-17-02-52-hello,-anyone-out-there,-[shouting]-can-someone.mp3` | 3.082 s | mono | archived |
| `Verity-2026-07-17-02-56-[shouting]-Is-someone-there.mp3` | 0.993 s | mono | archived |
| `Verity-2026-07-17-02-56-[shouting]-can-you-hear-me!.mp3` | 1.176 s | mono | archived |
| `Verity-2026-07-16-22-33-[excited]-Hello,-i'm-verity,-im-your-personal-he.mp3` | 5.590 s | mono | archived (alt greeting) |

### File 2 phrase analysis

- **Silence detect** (`noise=-30dB`, min 0.3 s): pause begins ~**1.40 s** → first phrase (“hello…”) ~0–1.4 s.
- **Trim used for `hello.ogg`:** 0–**1.0 s** muffled + 0.15 s fade-out (within requested 0.8–1.2 s window).
- **Full muffled copy:** `hello_long.ogg` and `anyone_out_there.ogg` (identical encode of entire file 2).

---

## Figura originals (Desktop pack)

| File | Duration | Location | Intended use |
|------|----------|----------|--------------|
| `talk.ogg` | 5.867 s | `provided_assets/audio_figura_originals/` | Optional ambience only — **do not** replace `greeting_personal_helper` |
| `creepytalk.ogg` | 1.483 s | same | Optional ambience |
| `dance.ogg` | 11.967 s | same | Optional ambience |

---

## Runtime outputs (`src/main/resources/assets/universe_verity/sounds/`)

| Output | Duration | Codec | ~Bitrate | Mapping |
|--------|----------|-------|----------|---------|
| `verity/voice/greeting_personal_helper.ogg` | 5.460 s | vorbis mono | ~76 kb/s | File 1, clean |
| `verity/box/voice/hello.ogg` | 1.000 s | vorbis mono | ~79 kb/s | File 2 trim, muffled |
| `verity/box/voice/hello_long.ogg` | 3.082 s | vorbis mono | ~61 kb/s | File 2 full, muffled |
| `verity/box/voice/anyone_out_there.ogg` | 3.082 s | vorbis mono | ~61 kb/s | File 2 full, muffled |
| `verity/box/voice/is_someone_there.ogg` | 0.993 s | vorbis mono | ~80 kb/s | File 3, muffled |
| `verity/box/voice/can_you_hear_me.ogg` | 1.176 s | vorbis mono | ~76 kb/s | File 4, muffled |
| `verity/box/movement/rustle_1.ogg` | 0.450 s | vorbis mono | ~132 kb/s | **GENERATED_SFX** |
| `verity/box/movement/rustle_2.ogg` | 0.550 s | vorbis mono | ~117 kb/s | **GENERATED_SFX** |
| `verity/box/movement/shift.ogg` | 0.350 s | vorbis mono | ~146 kb/s | **GENERATED_SFX** |
| `verity/box/movement/thump.ogg` | 0.180 s | vorbis mono | ~212 kb/s | **GENERATED_SFX** |
| `verity/box/movement/box_open.ogg` | 0.750 s | vorbis mono | ~109 kb/s | **GENERATED_SFX** |
| `verity/box/knocks/knock_1.ogg` | 0.060 s | vorbis mono | ~578 kb/s* | **GENERATED_SFX** |
| `verity/box/knocks/knock_2.ogg` | 0.050 s | vorbis mono | ~681 kb/s* | **GENERATED_SFX** |

\*Very short clips report inflated instantaneous bitrates; perceptual level is still low.

---

## Provided_assets conversions

| Output | Duration | Notes |
|--------|----------|-------|
| `greeting_personal_helper_alt_reference.ogg` | 5.590 s | Clean OGG of alt MP3; not in runtime `voice/` folder |

---

## Missing / not yet sourced

| Expected need | Status |
|---------------|--------|
| Reveal stinger / fanfare (`sounds/verity/reveal/`) | **Missing** — directory created, empty |
| Real cardboard Foley (rustle, knock, open) | **Replaced by GENERATED_SFX** until recorded |
| Clean (unmuffled) box voice variants | **Not requested** — box lines are muffled-only in runtime pack |
| Separate MP3 split for “anyone out there” only | **Not done** — full file 2 used; could be split manually later using ~1.4 s offset |
| `sounds.json` entries | **Not created in this pass** — wire events in mod resources separately |

---

## Muffle filter (box voice)

```
ffmpeg -af "lowpass=f=1200,volume=0.85" …
```

Applied to all `verity/box/voice/*.ogg` outputs. Primary greeting remains **unfiltered**.
