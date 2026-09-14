# CowClient architecture

Fresh implementation; no Ascend files and no FoxClient source copied.

## Client
Minecraft 1.21.11, official Mojang mappings, Java 21, Fabric Loader >=0.18.1. Fabric API 0.141.1+1.21.11. These APIs are version-specific; do not label this jar compatible with 1.21.1 or 26.x.

Core module descriptors and JSON profiles are independent of screen code. Settings are bounded, persisted atomically, and unreadable original files are preserved. The launcher and game use separate settings files.

The ClickGUI uses an antialiased Java2D surface and bundled Noto Sans. Native textures are updated only when screen state changes. Opening scale/translation is animated without rebuilding the raster. The HUD caches per-module surfaces; changing readings are sampled at 10 Hz. GPU resources for the menu are released on close. This is an initial cached-raster implementation, not an SDF/GPU vector renderer; benchmark CPU upload cost before expanding animation.

All 16 displayed modules have implementations. No dummy switches for motion blur, custom sky, capes or friends. Search is scoped by category; right-click opens details; Tab and Enter provide basic keyboard navigation. Full screen-reader narration is not yet implemented.

## Launcher
Self-contained .NET 8 Windows x64 WPF executable, CmlLib.Core 4.0.6 for official Minecraft metadata/runtime installation and process construction, Fabric metadata for loader installation, Modrinth CDN for compatible Fabric API/Sodium/Lithium. Downloads are checked against SHA-512 metadata; the bundled client is checked against its packaged checksum.

The isolated instance is %LOCALAPPDATA%/CowClient/instances/1.21.11. Only CowClient-managed mod names are replaced, with old files moved to backups. Other launchers' files, worlds and configurations are not touched. Installed verified dependency versions are reused rather than silently updated on every launch. This prototype does not resolve arbitrary third-party mod dependency graphs.

No background browser engine, remote UI content, telemetry, auto-updater or automatic paid services. Microsoft sign-in requires the project's own application registration; see AUTHENTICATION.md.

## Design
[Figma source](https://www.figma.com/design/ddrm6qleBaBBqlGkGFMN0W). Dark meadow theme, warm text, green accent, 12–24 pixel radii. The cow icon is original SVG geometry, not an image-model output or copied Minecraft texture; it is assistant-authored and is not claimed to be human-drawn.

## Verification
CI compiles/remaps the client, runs Java unit tests, opens the real development client under Xvfb, checks that CowScreen opens and captures its display. Windows CI runs C# tests, publishes a self-contained launcher, starts its real WPF window and captures it. These checks do not certify multiplayer gameplay, shader compatibility, long sessions or Microsoft account login.
