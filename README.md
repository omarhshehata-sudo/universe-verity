# Universe Verity

Forge 1.20.1 mod for the **universe.exe** horror pack. Implements Verity's sealed-box introduction and first reveal.

## Install

1. Install [Minecraft Forge 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) (47.x).
2. Install [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) for Forge 1.20.1 (required dependency).
3. Download the latest JAR from [Releases](https://github.com/omarhshehata-sudo/universe-verity/releases) and place it in your Minecraft `mods` folder.

## Requirements

- Minecraft **1.20.1**
- Forge **47.x** (tested with 47.4.10)
- Java **17**
- **GeckoLib** (declared dependency)
- **FTB Quests** + **FTB Library** + **FTB Teams** (for the in-game VERITY quest book chapter)

Verity quests appear in the **FTB Quests book**, not the vanilla Minecraft Advancements tab. On first world load the mod auto-installs the VERITY chapter into `config/ftbquests/quests/chapters/verity.snbt`. If the chapter is missing after updating, run `/verity book sync` or restart the instance once.

## Build from source

```bash
./gradlew build
```

Output JAR: `build/libs/universe_verity-<version>.jar`

## Releases

Published builds: [github.com/omarhshehata-sudo/universe-verity/releases](https://github.com/omarhshehata-sudo/universe-verity/releases)

## Gameplay (overview)

1. First Overworld join → after ~3s a sealed cardboard box spawns safely in front of the player.
2. Muffled Verity voice lines and subtle box movement play from the box.
3. Owner right-clicks the box → opening sequence → Verity ball entity appears → box is removed.
4. Verity plays the greeting and remains for later features.

## Developer commands

`/verity intro spawn|reset|replay|remove|status|skip <player>`  
`/verity box locate|animate <player> <animation>`  
`/verity reveal trigger|reset|status|skipanimation <player>`  
`/verity entity locate|remove|respawn <player>`  
`/verity greeting replay|stop|status <player>`  
`/verity sound play <sound_id>`  
`/verity debug <true|false>`

## Config

`config/universe_verity-common.toml` (generated on first run)

## Publishing updates

See [RELEASE_PROCESS.md](RELEASE_PROCESS.md) for how agents should bump, build, and publish new versions to this same repository.
