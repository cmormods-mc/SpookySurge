# Boss Bar Skins

A small server-side Fabric library (Minecraft 1.21.1, Polymer 0.9.19+1.21.1) so that **several mods can each reskin boss bars while still
calling the plain vanilla API**. A mod keeps writing `new ServerBossEvent(name, BossBarColor.YELLOW, BossBarOverlay.PROGRESS)`; this mod
works out which mod made the bar and which look that mod declared, and sends the client the colour and overlay of a sprite slot that mod owns.
Two mods that both ask for `YELLOW` get two different bars, with no code change in either.

## What the client can and cannot do (verified in the 1.21.1 bytecode)

`BossHealthOverlay` picks sprites by two enums only: `boss_bar/<colour>_background|progress`, then, for a notched overlay, a second pair
`boss_bar/notched_N_background|progress` drawn *afterwards* over the whole bar. There is no per-bar texture id. So the distinct looks a
server can show are:

* a **colour slot**: `YELLOW`, `GREEN`, `WHITE`, `BLUE` (+ `PINK`, `PURPLE`, `RED`, which vanilla's own bosses use);
* an **overlay slot**: `NOTCHED_6`, `_10`, `_12`, `_20`. An overlay pair that is fully opaque hides the colour sprites under it, so it is a
  complete skin of its own, whatever colour the bar has.

That is at most 7 free skins by default (4 colours, one of which is shared by every mod's invisible spacer, plus 3 overlays; `NOTCHED_20` is
reserved because CobbleRaids' World Boss uses it). Anything beyond that cannot be done on an unmodified client.

## How it works

1. **Declare.** A mod ships `bossbarskins.json` at the root of its jar (below). No code dependency: the file is the whole integration.
2. **Attribute.** A mixin on `BossEvent` records the mod whose class called the constructor (`StackWalker`, mapped to a mod id by which jar
   contains the class). A bar built by vanilla code has no owner and is never touched, even if a mod's code triggered it.
3. **Allocate.** At startup `SlotAllocator` gives every skin a slot, in this order: a pin from the config, the slot it had last time
   (`config/bossbarskins/assignments.json`, so adding a mod never reshuffles the others), the slot the mod asked for, then any free pool slot
   (colours first, then notched overlays, which need fully opaque sprites). A mod alone on a colour is **not remapped at all**.
4. **Remap.** Mixins on the two places a boss-event packet reads the style (`AddOperation`, `createUpdateStylePacket`) write the assigned
   colour and overlay instead of the requested ones. Only requests that exactly match a declared skin are changed.
5. **Pack.** The sprites are added to Polymer's resource pack under the slot's file names, so the one generated pack carries every mod's art.

## Declaring a skin

```json
{ "skins": [
  { "id": "candy_hunt", "match": "YELLOW",
    "background": "bossbarskins/candyhunt/background.png",
    "progress":   "bossbarskins/candyhunt/progress.png",
    "meta":       "bossbarskins/candyhunt/progress.png.mcmeta" },
  { "id": "spacer", "match": "BLUE", "transparent": true }
] }
```

* `match`: what the mod's code asks for, `COLOUR` or `COLOUR/OVERLAY` (for example `YELLOW`, or `PINK/NOTCHED_20`).
* `background` is a PNG 182 x 5. `progress` is 182 x 5, or a stack of 5 px frames with a `.mcmeta` (`"animation": {"frametime", "width": 182, "height": 5}`).
* `transparent: true` asks for an invisible bar (a spacer that pushes the next bar down). Every mod's transparent request shares one slot.
* Skins with any translucent pixel are colour-slot only. Invalid sprites are skipped with a log line saying why.
* Pack files live under the mod's own folder; the library writes them to the vanilla sprite names. A mod can still fall back to writing
  the vanilla names itself when this mod is absent (Spooky Surge does).

## Operating it

* `/bossbarskins list` (op level 2): every skin, what it asked for, where it landed (`(relocated)` marks a clash), or `NO SLOT`.
* `config/bossbarskins/config.json`: `reserved` (slot names nobody is relocated into; default `pink`, `purple`, `red`, `notched_20`) and
  `pins` (`"modid:skinid": "YELLOW"` or `"YELLOW/NOTCHED_6"`).
* Startup warnings name every relocation and every skin that found no slot.
* Optional API for shared helper code that creates bars on behalf of another mod: `BossBarSkinsApi.claim(event, "modid")`.

## Limits and honest caveats

* **Players without the pack** see the slot's vanilla look, so a relocated bar shows the relocated colour (a bar asked as `YELLOW` but placed on
  `GREEN` is plain green). A mod alone on its colour is unaffected. Mods that care should give non-pack players their own plainly worded bar.
* A skin that overlays only part of a bar (translucent notch ticks, as AscensionLib's World Boss uses on `PINK/NOTCHED_20`) is not supported
  as a skin: it is not opaque, so it cannot take an overlay slot. Leave that mod on its static pack, and keep `notched_20` reserved.
* Slots used by bars of mods that do **not** declare anything are unknown to this mod. Put them in `reserved`.
* One slot per (mod, requested look): a mod cannot declare two different skins for the same request.
* Replacing a vanilla boss colour (`PINK`, `PURPLE`, `RED`) is allowed when a mod asks for it, and then affects vanilla's own bosses too.
* The owner is the first non-vanilla caller of the constructor. A mod calling through a library it does not own should use `claim`.

## Build and test

```bash
cd bossbarskins && ./gradlew build        # lib: 17 unit tests; testmod: a deliberately naive second mod
```

`testmod` (not for production) asks for `YELLOW` through the plain API and ships a teal skin. With Spooky Surge also asking for `YELLOW`, the
real-client capture [two-mods-same-colour.png](../docs/bossbar/captures/two-mods-same-colour.png) shows both at once, a third mod's `PURPLE` bar
unchanged, and vanilla's own Wither-coloured bar unchanged. [two-mods-overlay-slot.png](../docs/bossbar/captures/two-mods-overlay-slot.png)
pins the teal skin to `YELLOW/NOTCHED_6` to show an overlay slot working.
