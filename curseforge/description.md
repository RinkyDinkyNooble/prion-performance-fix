# Prion Performance Fix (Unofficial Patch)

A companion mod for Prion Infection: Whispers From Within 2.0.93 (Forge 1.20.1, through Sinytra Connector) that makes its mobs much cheaper to draw. Prion Infection itself is still required.

## Before And After

Same spot and settings, without the patch and then with it. The FPS counter is in the top left.

A crowd of about 40 Prion mobs: **95 FPS** average without, **360 FPS** with.

Without:

![Prion mobs without the patch](https://raw.githubusercontent.com/RinkyDinkyNooble/prion-performance-fix/main/curseforge/images/before.jpg)

With:

![Prion mobs with the patch](https://raw.githubusercontent.com/RinkyDinkyNooble/prion-performance-fix/main/curseforge/images/after.jpg)

## Faster

- Animated mobs no longer search their whole model for each moving part on every frame. Prion's mobs have big models, so this search was most of the time spent drawing them: for a Rooted it took 228 µs per frame, now under 1 µs. It also speeds up every other mob that uses Minecraft's own animations (frogs, wardens, camels, sniffers and modded mobs). Embeddium has its own version of this fix, but Oculus turns it off.
- Resetting a mob's pose before animating it no longer walks its whole model through nested streams. For a Vicious Turret that took 4 µs per frame, now 0.15 µs.
- A Prion mob's health texture is only rebuilt when the mob's health changes. Before, it was rebuilt pixel by pixel and sent to the graphics card again on every frame, for every mob.

Mobs look and move exactly as before.

## Bug Fixes

- Prion mobs' health textures are freed when the mob leaves the world. Before, a mob that despawned or unloaded kept its texture in memory until the game closed. A mob that died gave its texture back, then took a new one for its death animation and kept that.

The log has one line for each patched class, saying how many changes applied.

### Credit

This is an unofficial patch, not affiliated with Prion Infection. [Prion Infection: Whispers From Within](https://www.curseforge.com/minecraft/mc-mods/prioninfection) is made by Demod.
