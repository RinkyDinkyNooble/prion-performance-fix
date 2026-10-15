# Changelog

## 1.1.0 (2026-10-15)

For Prion Infection: Whispers From Within 2.0.93.

- Faster: Prion's spawn check asks its mob cap before scanning about 28,800 blocks for light, so a mob kind already at its cap no longer pays for the scan (3.5% of the server tick at night in a cold biome; 708 ms to 8 ms over 3 minutes in the same spot).
- Fixed: Prion's cysts, Hives and kills no longer turn chests, spawners, other blocks with a block entity, unbreakable blocks or blocks as blast-proof as obsidian (SecurityCraft's reinforced blocks) into tissue. More blocks can be spared with the block tag `prionperformancefix:prion_immune`.
- Fixed: a cyst outside the Overworld acts where it lands, instead of in the Overworld at the same coordinates.
- The new fixes work on the server (singleplayer included); the client and server still don't need to match.

## 1.0.0 (2026-10-12)

First version, for Minecraft 1.20.1, Forge 47 and Prion Infection: Whispers From Within 2.0.93 (through Sinytra Connector).

- Faster: animated mobs find their moving parts without searching the whole model on every frame, poses are reset without nested streams, and Prion's health textures are only rebuilt when a mob's health changes. The first two also help every other mob on Minecraft's own animations while Oculus is installed.
- Fixed: Prion's health textures never freed for mobs that despawned, unloaded or died.
- Logs one line per patched class at startup, saying how many changes applied.
- Works on the client, the server or both, and the two don't need to match. Everything it changes is on the client.
