# Acid Client V2 — Visual Direction
The V1 dashboard treatment is **archived**, not the design to implement. This V2 direction replaces large, squarish dashboard tiles with a floating Minecraft in-game UI language: 26–41 px capsules, inset panels with 20–36 px radius, restrained emerald bloom, radial selectors, tight typography, and a minimal **acid-green A** monogram used as the custom crosshair.

## Five newly drawn editable Figma screens
1. **Cinematic Main Menu** — custom Minecraft menu with brand halo, curved glass action panel and navigation.
2. **Floating ClickGUI** — mod categories in pill navigation, tight module rows, search lozenge, contextual settings panel, status and keybind.
3. **Crosshair Studio** — custom original SVG A reticle, real-time mock Minecraft preview, color swatches, size slider, glow amount, hit pulse, outline settings, local presets.
4. **Emotes Wheel** — radial selector with eight independently bindable gestures; hover/selection concept.
5. **HUD Studio** — draggables for watermark, array list, keystrokes, session stats; editable opacity and scaling.

## Design tokens
- Acid: `#B9FF5C`
- Dark surface: `#080D0B`
- Raised: `#233429`
- Border: `#344938`
- Text: `#F4F9F1`
- Muted: `#93A599`

The A crosshair is original SVG vector art stored in `design/acid-a-crosshair.svg`. It has an open-cut angular apex, parallel arms and simple crossbar, with transparent background. Desired in-game size ~30–38 px, with 1–3 px stroke scaled to user setting, subtle pulsing shader, no giant glowing cursor.

**Figma:** https://www.figma.com/design/0ioe7oZRp0EPA7B2Wd2kbO
**Replit concept:** https://replit.com/replid/c2722651-b661-477d-aff5-b8ffcb0b945f

The Figma screens are editable design artifacts. The Replit project is an interactive browser preview and is not a compiled, tested Minecraft client. Experimental gameplay-changing controls should be singleplayer-only, disabled on multiplayer. No cracked client archives incorporated in Acid's implementation.

## Build bar
Before claiming a Minecraft release, implement and compile Java/Fabric 1.21.11 from scratch, test GUI at 1920x1080 and UI scales 2–4, verify mouse input, IME/text rendering, shader compatibility, focus/ESC behavior, persistence, performance, and non-leaking config.
