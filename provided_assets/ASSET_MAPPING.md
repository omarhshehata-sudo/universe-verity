# Verity Audio Asset Mapping

Generated: 2026-07-17

## Encoding pipeline

| Step | Tool | Notes |
|------|------|-------|
| Decode / filter | `ffmpeg` | MP3 → WAV; box voice uses `lowpass=f=1200,volume=0.85` |
| Encode | `oggenc -q 4` (vorbis-tools / libvorbis) | OGG Vorbis, mono, ~50–76 kb/s effective on speech |
| Target | Minecraft `assets/.../sounds/` | Vorbis in `.ogg` containers |

Homebrew `ffmpeg` on this machine does not ship `libvorbis`; encoding uses **oggenc** instead of `ffmpeg -c:a libvorbis`.

---

## PROVIDED_VOICE (from Verity MP3 recordings)

| Runtime path | Category | Source (archived in `audio_originals/`) | Processing |
|--------------|----------|----------------------------------------|------------|
| `sounds/verity/voice/greeting_personal_helper.ogg` | PROVIDED_VOICE | `Verity-2026-07-17-02-51-[excited]-hello,-im-verity,-your-personal-helper.mp3` | **Clean** (no muffle); post-reveal greeting |
| `sounds/verity/box/voice/hello.ogg` | PROVIDED_VOICE | Same as row below (file 2), first **1.0 s** | **Muffled** + fade-out; short “Hellooo?” clip |
| `sounds/verity/box/voice/hello_long.ogg` | PROVIDED_VOICE | File 2 (full clip) | **Muffled** full take; fallback if short `hello.ogg` trim is awkward in-game |
| `sounds/verity/box/voice/anyone_out_there.ogg` | PROVIDED_VOICE | `Verity-2026-07-17-02-52-hello,-anyone-out-there,-[shouting]-can-someone.mp3` | **Muffled** full clip (hello + anyone out there + …) |
| `sounds/verity/box/voice/is_someone_there.ogg` | PROVIDED_VOICE | `Verity-2026-07-17-02-56-[shouting]-Is-someone-there.mp3` | **Muffled** |
| `sounds/verity/box/voice/can_you_hear_me.ogg` | PROVIDED_VOICE | `Verity-2026-07-17-02-56-[shouting]-can-you-hear-me!.mp3` | **Muffled** |

### Reference-only (not wired as primary runtime greeting)

| Path | Source | Notes |
|------|--------|-------|
| `provided_assets/greeting_personal_helper_alt_reference.ogg` | `Verity-2026-07-16-22-33-[excited]-Hello,-i'm-verity,-im-your-personal-he.mp3` | Alternate take; compare before swapping primary greeting |
| `provided_assets/audio_originals/*.mp3` | Downloads folder | **Unmodified** backups |

### Figura pack ambience (optional reference — **not** greeting replacement)

| Path | Source | Use |
|------|--------|-----|
| `provided_assets/audio_figura_originals/talk.ogg` | V1.3 Verity Ball Model Figura Avatar | Optional ambience / legacy reference |
| `provided_assets/audio_figura_originals/creepytalk.ogg` | same | Optional ambience |
| `provided_assets/audio_figura_originals/dance.ogg` | same | Optional ambience |

---

## GENERATED_SFX (procedural — no source recordings existed)

These were synthesized with **ffmpeg `lavfi` noise sources** and band-pass / envelope filters to suggest soft cardboard motion. They are placeholders until real Foley is recorded.

| Runtime path | Category | Generator summary |
|--------------|----------|-------------------|
| `sounds/verity/box/movement/rustle_1.ogg` | GENERATED_SFX | Pink noise ~0.45 s, soft HP/LP, fade |
| `sounds/verity/box/movement/rustle_2.ogg` | GENERATED_SFX | Brown noise ~0.55 s + light tremolo |
| `sounds/verity/box/movement/shift.ogg` | GENERATED_SFX | Pink noise ~0.35 s, muffled band |
| `sounds/verity/box/movement/thump.ogg` | GENERATED_SFX | Brown noise ~0.18 s, low thump |
| `sounds/verity/box/movement/box_open.ogg` | GENERATED_SFX | Pink noise ~0.75 s, tear-ish tremolo + fades |
| `sounds/verity/box/knocks/knock_1.ogg` | GENERATED_SFX | Short white-noise tap ~60 ms |
| `sounds/verity/box/knocks/knock_2.ogg` | GENERATED_SFX | Short white-noise tap ~50 ms |

---

## Empty / reserved directories

| Directory | Status |
|-----------|--------|
| `sounds/verity/reveal/` | **Empty** — reserved for post-box-reveal stingers (no source assets yet) |

---

## Minecraft sound event names (suggested)

| Event ID suffix | File |
|-----------------|------|
| `verity.voice.greeting_personal_helper` | `verity/voice/greeting_personal_helper.ogg` |
| `verity.box.voice.hello` | `verity/box/voice/hello.ogg` |
| `verity.box.voice.hello_long` | `verity/box/voice/hello_long.ogg` |
| `verity.box.voice.anyone_out_there` | `verity/box/voice/anyone_out_there.ogg` |
| `verity.box.voice.is_someone_there` | `verity/box/voice/is_someone_there.ogg` |
| `verity.box.voice.can_you_hear_me` | `verity/box/voice/can_you_hear_me.ogg` |
| `verity.box.movement.rustle_1` | `verity/box/movement/rustle_1.ogg` |
| `verity.box.movement.rustle_2` | `verity/box/movement/rustle_2.ogg` |
| `verity.box.movement.shift` | `verity/box/movement/shift.ogg` |
| `verity.box.movement.thump` | `verity/box/movement/thump.ogg` |
| `verity.box.movement.box_open` | `verity/box/movement/box_open.ogg` |
| `verity.box.knocks.knock_1` | `verity/box/knocks/knock_1.ogg` |
| `verity.box.knocks.knock_2` | `verity/box/knocks/knock_2.ogg` |

## Greeting voice update
- Runtime: `assets/universe_verity/sounds/verity/voice/greeting_personal_helper.ogg`
- Source: `V1.5 Verity Models Figura Avatars/Verity Ball Model [Figura Avatar]/talk.ogg`
- Used for: "Hello, I'm Verity, your personal helper friend. Ask me anything. I know everything."
- Previous MP3 greeting retained under `provided_assets/audio_originals/` but is no longer the runtime greeting.


## Box voice update 2026-07-17 03:45
- anyone_out_there <- Verity-2026-07-17-03-45-Is-anyone-out-there.mp3
- can_you_hear_me <- Verity-2026-07-17-03-46-[emphasis]-can-you-hear-me.mp3
- is_someone_there (event kept) <- Verity-2026-07-17-03-47-[emphasis]-can-someone-let-me-out.mp3  (spoken: Can someone let me out?)
