# Release process (Universe Verity)

Future updates for this mod should always be published to the same GitHub repository:

**https://github.com/omarhshehata-sudo/universe-verity**

## Steps

1. Bump the mod version in `gradle.properties` (and any version strings that must match).
2. Build: `./gradlew build`
3. Confirm the JAR at `build/libs/universe_verity-<version>.jar`.
4. Commit and push source changes to `main` on this repo.
5. Create a GitHub Release with the JAR attached:

```bash
gh release create "v<version>" \
  --repo omarhshehata-sudo/universe-verity \
  --title "Universe Verity <version>" \
  --notes "Universe Verity <version> for Forge 1.20.1 (requires GeckoLib)." \
  "build/libs/universe_verity-<version>.jar"
```

Example for 1.0.13:

```bash
gh release create "v1.0.13" \
  --repo omarhshehata-sudo/universe-verity \
  --title "Universe Verity 1.0.13" \
  --notes "Universe Verity 1.0.13 for Forge 1.20.1 (requires GeckoLib)." \
  "build/libs/universe_verity-1.0.13.jar"
```

Do not create a new repo for each version. Do not force-push unrelated history. Do not commit secrets, `.env`, credentials, or huge `provided_assets` extracts (runtime assets stay under `src/main/resources`).
