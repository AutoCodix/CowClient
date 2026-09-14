# CowClient

A fresh Minecraft Java **1.21.11** Fabric client and standalone **Windows x64** launcher.

[Builds and downloads](https://github.com/AutoCodix/CowClient/actions/workflows/build.yml) · [Figma design](https://www.figma.com/design/ddrm6qleBaBBqlGkGFMN0W)

## Get a build

Open the latest **successful** Build CowClient run and download its artifacts:

- **CowClient-Windows-x64**: extract the entire ZIP and run CowClient.exe. Keep the Payload folder and runtime files with the executable. You do not need to compile or install .NET.
- **CowClient-Fabric-1.21.11**: the remapped client jar for an existing Fabric 1.21.11 instance. Requires Fabric Loader >=0.18.1 and Fabric API >=0.141.1 for 1.21.11.
- Verification artifacts contain runtime screenshots, logs and test results.

The launcher is an unsigned alpha. No code-signing certificate is configured. Do not disable antivirus or system-wide security to run it.

**Standalone Microsoft sign-in needs CowClient's own registered/approved Microsoft application ID.** See [authentication setup](docs/AUTHENTICATION.md). Real account launch is not yet verified. There is no cracked login fallback.

## In-game

Right Shift opens CowClient. Search/filter modules; click to enable; right-click for settings. Use Arrange HUD to drag panels, and Esc to save. Change keybinds in Minecraft Options > Controls > CowClient.

Implemented modules: FPS, left/right CPS, keystrokes, ping, coordinates, armor durability, speed, direction, session timer, heap memory, local clock, smooth zoom, walking-bob suppression, hurt-tilt suppression, toggle sprint, reduced motion. Includes blur toggle, HUD scale and saved Default/Competitive/Chill profiles.

The launcher installs Minecraft/Fabric and the CowClient payload, with optional Sodium/Lithium, RAM selection, account sign-out, progress display and isolated game folder. Confirm individual server rules for zoom, toggle sprint and visual modifications.

## Scope

This is a first alpha, not Lunar/Feather feature parity. Custom skyboxes, custom main menu, motion blur, mod browsing, arbitrary Minecraft version selection, friends, capes and automatic updates are **not implemented**. No benchmarked FPS gains are promised. The prototype is for validating the core appearance and launcher pipeline before those additions.

## Developer builds

Use Java 21, Gradle 9.2.1 and .NET 8 SDK. The build_assets.py script needs CairoSVG/Pillow and downloads Noto Sans with its font license. Run it first, then gradle -p client build. See .github/workflows/build.yml for packaging and smoke tests. Source dependencies and versions are pinned in project files; CI action major tags and the font source should be locked further before a production release.

[Architecture and testing boundaries](docs/ARCHITECTURE.md) · [Third-party notices](THIRD_PARTY.md)

CowClient is not affiliated with Mojang, Microsoft, Lunar, Feather or FoxStudios. An owned copy of Minecraft Java is required.
