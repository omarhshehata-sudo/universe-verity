# Verity Trust / Mood / Countdown System

**Loader note:** This project is **Forge 1.20.1** (`universe_verity`), not NeoForge. The system is implemented on that stack.

## Persistence
Trust and countdown state live in the owner player’s Forge persistent NBT under root `universe_verity` (same as intro data). Keys are in `VerityTrustKeys`. Survives reconnect, death copy (`VerityPlayerData.copy`), and restarts.

## Sync
- Trust is **server-only**.
- Mood is written to `VerityEntity` synched data (`DATA_MOOD`).
- Clients render mood faces from synced mood; they never compute trust.

## Face textures (replace these placeholders)
```
assets/universe_verity/textures/entity/verity/face_happy.png
assets/universe_verity/textures/entity/verity/face_friendly.png
assets/universe_verity/textures/entity/verity/face_meh.png
assets/universe_verity/textures/entity/verity/face_angry.png
assets/universe_verity/textures/entity/verity/face_furious.png
assets/universe_verity/textures/entity/verity/face_critical.png
assets/universe_verity/textures/entity/verity/face_monster.png
```

## Audio placeholders (replace OGG contents)
Under `assets/universe_verity/sounds/verity/trust|countdown|monster/` — currently copies of a silent-ish existing clip. Replace with final dialogue; keep filenames.

## Debug commands (op 2)
`/verity trust get|set|add|reason`
`/verity mood`
`/verity countdown start|status|skipday|canceldebug`
`/verity transform`
`/verity resetstory`

## Monster
Uses existing `verity_demon` entity + GeckoLib model. Owner pursuit AI enabled after transform.
