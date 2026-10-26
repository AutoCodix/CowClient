# Acid Client — Fabric 1.21.11 concept

**Status: design and prototype stage, NOT a compiled Minecraft mod.**

## Identity
Dark graphite surfaces, acid green `#ABFF4F`, elegant vector monogram A as the default crosshair. Every section uses independent configuration with local JSON profiles, bindable inputs, preview/undo, low-overhead rendering, and accessible controls.

## Main menu
Custom title screen with Singleplayer, Multiplayer, Acid Modules, Crosshair Studio, Emotes, Options, Quit, and user profile details. Minecraft branding is clearly distinguished from Acid branding.

## ClickGUI
Searchable categories: Visual, World, Player, Movement, Combat, Utility, Experimental, Client. Module cards support on/off, favorite, settings flyout, keybind, saved preset, and explanatory tooltip.

## Crosshair Studio
Preset "Acid / Elegant A"; green stroked A with clean open apex and crossbar. Controls: size, aspect, line thickness, color, outline, glow, pulse on hit, animation, dynamic spread, opacity, offset X/Y, autoscale, preset save/import/export. Restore vanilla crosshair option.

## Emotes
Wheel bound to G with 8 configurable slots, previews, keybinds, animation speed and cooldown. Include Wave, Clap, Dance, Sit, Point, Celebrate, Nod, Bow. Local/client visual actions only unless supported by a server; do not assume peers can view them.

## HUD and visuals
HUD Studio, custom sky/sky lyrics, world bloom, atmosphere editor, weather, crosshair, viewmodel, shader controls, particle studio, color filter, notifications, cosmetic outlines, and FPS-budget manager.

## World utilities
Chunk borders, loaded-chunk analyzer, configurable candidate highlights (never claim access to unloaded chunks), waypoints, storage overlays, and discovered-structure notebook.

## Experimental
Fly, Speed, NoClip, Timer, creative-style movement and target-dummy practice **strictly gated to integrated singleplayer worlds**. On remote servers, no activation or networking-side effects.

## Engineering constraints
Target Java 21+ / Fabric 1.21.11; original code and art, no copied proprietary assets or cracked binaries. Isolate client rendering modules and event subscriptions. Maintain local data under `.minecraft/config/acid-client/`; version JSON schema, never store passwords or tokens. Avoid reflection-based packet exploits, hidden networking, downloaders, and telemetry.

## Design
[Figma — Acid Client UI Kit & Screens](https://www.figma.com/design/0ioe7oZRp0EPA7B2Wd2kbO)

This branch is intentionally separate from CowClient main, and contains a blueprint, not a released JAR.
