# Validation report: Spooky Surge Candy Hunt boss bar

Date of checks: 2026-10-27. Environment: Minecraft 1.21.1, Fabric Loader 0.17.2, Fabric API 0.116.6, Cobblemon 1.8.2, Polymer 0.9.19+1.21.1
(+ `polymer-autohost` 0.9.19+1.21.1 for the delivery test), Java 21. A headless Fabric dedicated server and a real 1.21.1 Fabric client
(Mesa software GL under Xvfb, 1280 x 720) on the same machine.

## A. Checked and passing

| Check | How | Result |
|---|---|---|
| Builds on 1.21.1 with the repo's pinned Polymer, Fabric API and Loom | `./gradlew build` | success, 41 tests, 0 failures |
| Resources survive the build | `unzip -l build/libs/SpookySurge-0.1.0.jar` | all 9 pack files and the metrics file are in the jar |
| Resources reach the generated Polymer pack | `polymer generate-pack`, `unzip -l polymer/resource_pack.zip` | yellow/blue sprites, `.mcmeta`, font JSON, `frame_l/frame_r/tier_lit/letters` present; the metrics file is **not** in the pack |
| Dedicated server starts with no client-only class loading | real dedicated server boot with the mod, then surge, preview, respawn, dimension change | clean; no `CandyHunt bar` errors in the log |
| PNG dimensions and alpha | `CandyHuntAssetsTest` | yellow sprites 182 x 5 (progress 182 x 80, 16 frames) fully opaque; blue sprites 182 x 5 fully transparent; frame window (182 x 5) fully transparent; corners transparent; > 35% of the frame transparent |
| Metadata and font references | `CandyHuntAssetsTest` | `.mcmeta` valid; every provider file exists, lowercase, ascent <= height, equal rows, cell height = image height (1:1, no resampling), every PNG <= 256 x 256, no duplicate or undefined code points, `space` advances +-1..128 |
| Metrics equal the pixels | `CandyHuntAssetsTest.metricsMatchThePixels` | each letter's advance recomputed from the PNG (rightmost inked column + 2) equals the metrics the server uses |
| The title is always 262 px wide and centred | `CandyHuntTitleTest` | 9 names x tiers 0-4 (empty, long, accents, CJK, 80 chars...), exact for every `spaces(n)` from -400 to 400 |
| Java layout == the Python twin | `CandyHuntTitleGoldenTest` | 11 golden titles identical |
| Timer layout | `CandyHuntTitleTest`, golden titles | titles stay exactly 262 px wide with 0 to 8 timer characters (non-digits dropped); the timer is right-aligned to x = 209 |
| Progress is clamped | by construction (`setProgress`, `setHealth`) | NaN, negatives, > 1, zero or infinite max all become a valid 0..1 |
| Updates only on change | `ServerBossEvent` bytecode (`setProgress`/`setName` compare first; `addPlayer`/`removePlayer` act only on set changes); `CandyHuntBar` also skips identical names and tiers | |

## B. Observed in a real client (screen captures, not simulations)

| Observation | Capture |
|---|---|
| The art renders; frame window sits on the vanilla bar; fill clips natively; the empty cavity is clearly different from the fill, at 100/75/50/25/10/0 %, by day and by night | [health-states-real-client.png](captures/health-states-real-client.png) |
| Alignment holds at GUI scales 1, 2 (odd GUI width 639), 3 (odd GUI width 427) and 4 | [gui-scales-1-to-4.png](captures/gui-scales-1-to-4.png) |
| Two CandyHunt bars at once, plus an unrelated vanilla purple Wither bar: no overlap, the Wither bar is untouched; a long name is cut with `...` | [two-bars-long-name-vanilla-bar.png](captures/two-bars-long-name-vanilla-bar.png) |
| A live surge: the countdown timer under the bar ticks, and 0 / 2 / 3 / 4 tier lights glow as the tier rises | [live-surge-tiers.png](captures/live-surge-tiers.png) |
| **Pack delivery by Polymer auto-host**: the client got the server's prompt (screenshot), accepted, and `hasMainPack` turned the bar to ART | [polymer-autohost-prompt.png](captures/polymer-autohost-prompt.png) |
| **Pack declined**: one vanilla yellow bar with a plain-text title, no spacer, no missing-glyph boxes; the vanilla Wither bar is normal | [pack-declined-fallback.png](captures/pack-declined-fallback.png) |
| The bar survives a dimension change (Nether) and a death and respawn, with no duplicate | observed on screen during the session |
| A first attempt with one 262 px frame glyph showed a missing-glyph box, which led to the two-glyph frame (131 px halves render correctly). `FontTexture.SIZE = 256` in the 1.21.1 bytecode supports the cause; the box itself was observed on screen | `CandyHuntAssetsTest` now fails the build for any glyph over 256 px |

## B2. Boss Bar Skins (several mods, one colour), real client and dedicated server

| Observation | Evidence |
|---|---|
| Two mods asking for `YELLOW` through the plain API each get their own look; Candy Hunt was relocated to `GREEN` without any code change | [two-mods-same-colour.png](captures/two-mods-same-colour.png), `/bossbarskins list`, pack contents (`green_*` = Candy Hunt art, `yellow_*` = the other mod's) |
| An unskinned `PURPLE` bar from the other mod and a vanilla `/bossbar` purple bar are untouched | same capture |
| An overlay slot works: a skin pinned to `YELLOW/NOTCHED_6` draws correctly and Candy Hunt keeps `YELLOW` | [two-mods-overlay-slot.png](captures/two-mods-overlay-slot.png) |
| Spooky Surge alone (no library): sprites written on `yellow_*` / `blue_*` as before | pack listing on a server without the library |
| Mixins load on a dedicated server; assignments persist in `config/bossbarskins/assignments.json` | server log, file contents |
| Allocation rules, order independence, persistence, pins, overlay eligibility, JSON and PNG validation | 17 unit tests in `bossbarskins/lib` |

| AscensionLib's World Boss bar (`PINK` + `NOTCHED_20`, static sprites copied into the Polymer pack) is untouched and keeps its art; a skin from another mod pinned to `PINK` is refused and moved | [world-boss-coexists.png](captures/world-boss-coexists.png); pack bytes for `pink_*` and `notched_20_*` equal AscensionLib's source files (175, 206, 115, 115 bytes); startup log `protected: pink / notched_20` |

The World Boss check used AscensionLib's real sprites and the same Polymer call, in a stand-in mod: AscensionLib's own jar could not be built in this environment (its Kotlin plugin is not cached), and CobbleRaids was not run.

Not verified for the library: attribution through Kotlin, lambdas or helper libraries in other mods' jars (the `StackWalker` and
class-to-jar lookup handle them in principle), more than the two real mods tried here, and AscensionLib / CobbleRaids themselves.

## C. Not verified: needs a person, a different setup, or more time

* **Pack finishing loading after the player has joined** (the `ON_RESOURCE_PACK_STATUS_CHANGE` switch from plain to art). Coded, not
  triggered: in the test the pack was already loaded when the player joined. Check on staging with a slow connection.
* **A pack hosted outside Polymer** (`server.properties`, a CDN): by design `auto` falls back to plain because Polymer cannot see it; use
  `"bossBar": "art"`. Not exercised.
* **GUI scales above 4, windows narrower than 1280 px, 4K / high-DPI**, and the 3 x 3 row limit (`guiHeight / 3`) on very small windows.
* **Other resource packs** that also replace boss-bar sprites or the default font; and **other mods' YELLOW or BLUE bars** (none are used
  in this workspace's repositories, but the live server was not inspected).
* **Bedrock / Geyser players**, mobile clients, a Fabric client with Sodium/Iris-style renderers (the test client is vanilla rendering).
* **Long soak**: hours of a real surge with many players. The bar adds no per-tick work beyond the existing once-a-second refresh; sending
  is change-driven, but this was not measured under load.
* Hardware-GL rendering: all captures used Mesa software GL; colours are the same pipeline but were not compared on a GPU.
* The art is judged by me from captures; it has not been reviewed by the server's players.
