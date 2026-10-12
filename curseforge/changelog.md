# Changelog

## 1.0.0 (2026-10-12)

First version, for Minecraft 1.20.1, Forge 47 and Prion Infection: Whispers From Within 2.0.93 (through Sinytra Connector).

- Faster: animated mobs find their moving parts without searching the whole model on every frame, poses are reset without nested streams, and Prion's health textures are only rebuilt when a mob's health changes. The first two also help every other mob on Minecraft's own animations while Oculus is installed.
- Fixed: Prion's health textures never freed for mobs that despawned, unloaded or died.
- Logs one line per patched class at startup, saying how many changes applied.
- Works on the client, the server or both, and the two don't need to match. Everything it changes is on the client.
