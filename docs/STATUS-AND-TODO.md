# Halloween 2026: where everything stands, and what is left

Written 2026-10-28. The event window opens **2026-10-31 04:00Z (midnight EDT)** and closes 2026-11-03 05:00Z, so about three days remain.
Sources: this session's work, the repos on branch `cmor/amazing-hopper-1r6071`, and (for the Halloween Tower) CobbleTowers' own design docs and the
transcript tail of your other session. "Verified" means I saw it run; "not verified" means exactly that.

## 1. What exists

### Spooky Surge (the new mod, repo `cmormods-mc/SpookySurge`)
A server-side Fabric mod (1.21.1, Cobblemon 1.8.2, Polymer 0.9.19). Every evening a 30-minute surge fills the server's ghost/dark/grass Pokémon spawns around every
player; catches and battle wins fill one shared counter with four tiers; Candy Corn pays out.

| Piece | State |
|---|---|
| Surge engine: schedule, countdown, own spawner, shared counter, 4 tiers, caps, restart persistence, `/surge ...` commands | Built. Verified with bots and a real client (spawning, catch and battle credit, tiers, offline claim, restart, load test). |
| Candy Corn item `spookysurge:candy_corn` (Polymer, own model) | Built and seen in a real client. **Give the vendor this id.** |
| Candy Hunt boss-bar art (pumpkin pail, ghost, tier pumpkins, banner, small timer) | Built and verified in a real client: art with the Polymer pack delivered by auto-host, plain fallback when declined, GUI scales 1-4, two bars at once. |
| **Boss Bar Skins** library (`bossbarskins/`, separate Gradle build) | Built. Several mods reskin vanilla bars by mod id without clashing; protects slots AscensionLib already skins (`pink`, `notched_20`). Verified with a test mod and AscensionLib's real sprites. |
| Tier 4 prize: **World Boss** instead of the Haunted Visitor (`tierPrize: worldboss`, fallback to the Visitor) | Built and verified with the real CobbleRaids jar. |
| CobbleRaids 0.8.191: `CobbleRaidsWorldBoss.start/isEnabled`, `spawn.auto_start`, **arena markers** (`world_boss_spawn`, `world_boss_entrance`), warp/return, `arena.radius` | Built, tests and all CI validators pass, verified live in a second dimension. |
| Docs: operator guide, Candy Corn guide, staging checklist (17 items), bossbar README + validation report | Written. |

### Halloween Tower "Hollow's Eve" (CobbleTowers)
From CobbleTowers' design docs P54, P55, P56 and its git log (the cloud clone's newest commit is `ea1f30a`; your Windows machine may be ahead, see section 3):

| Piece | State |
|---|---|
| P54 step 1: `cobbletowers:halloween` (10 floors cloned from Duskvale, stand-in bosses, 27-species spooky pool, no armor, `ascension: true`, date window `2026-10-31T04:00Z` to `2026-11-03T05:00Z`) | Built, unit-tested. The doc says "not run live". |
| Hollow's Eve web UI skin (dusk skyline, moon, pumpkin medallion, "Limited time" tag with closing time) | Built. |
| P55 step 1: **modifier pedestals** (click-to-vote holograms beside the vendor, gaze prompt, spectacle effects) | Built; 15/15 rig test with real bot clicks, 915 unit tests. Bug found and fixed: the vendor stood ~80 blocks from the hall. |
| P55 Phase A: macro architecture (Y schedule, integration contract) | Written; arithmetic checks pass. "Halls connected on foot" **fails**: the current tower is not walkable. |
| P56 steps 1-2: seeded **branching route map** between floors, atlas page in the Hollow's Eve look | Built. Your other session captured 10 real payloads from two live Halloween runs and rendered the pages; it hit the weekly limit (resets Oct 29, 7pm ET) while rebuilding the release bundle and running tests, "holding the push until you've looked". |

## 2. Still to do

### A. Spooky Surge and the World Boss: before 2026-10-31
1. **Staging run of the whole chain** (the 17-item checklist in `docs/STAGING-CHECKLIST.md`) with two real players, one declining the pack. Never done on your real server.
2. Install on staging: `SpookySurge`, `BossBarSkins`, `CobbleRaids 0.8.191`, `polymer-autohost` enabled, then `/polymer generate-pack`. Confirm `/bossbarskins list` shows `protected: pink` and `protected: notched_20` (AscensionLib's World Boss bar: only checked with a stand-in).
3. CobbleRaids `world_boss.json`: `enabled: true` and `spawn.auto_start: false` so the boss comes only from tier 4; `/cr worldboss reload`.
4. **Arena:** paste `MourningCourt_OpenSky_128x128x100.schem` in its own world, place `cobbleraids:world_boss_spawn` (and optionally `..._entrance`), check `/cr worldboss arena list`. A real paste, `arena scan` on one, and a custom dimension are **not verified**.
5. Check no other mod on the server uses `YELLOW` or `BLUE` boss bars.
6. Give the vendor the id `spookysurge:candy_corn` and set up the shop.
7. Back up `config/spookysurge/server.json` (new keys: `bossBar`, `tierPrize`, `prizeFallbackToVisitor`).

### B. Spooky Surge: open decisions and small gaps
- **Tier message:** chat only today; no on-screen title. You asked about "ghost type shiny boost reached" style messages: I proposed a title and subtitle per tier plus a sound, with the chat naming only the new perk. Not built; needs your wording per tier.
- The art bar no longer shows the counter or time-left numbers (the small timer does show time). Say if you want the counter back in the banner.
- **Spooky Essence payout:** AscensionLib is the only payer of event materials. Surge exposes `SurgeEndedCallback`; the AscensionLib listener that pays Spooky Essence from it is **not written**.
- Surge and the World Boss do not yet declare skins with Boss Bar Skins on the AscensionLib/CobbleRaids side (works without it, only Spooky Surge participates).
- Move `bossbarskins/` to its own repo if other mods should depend on it.
- Unverified: switch from plain to art when a player's pack finishes loading after joining; GUI scales above 4; a long soak with many players; the art has not been judged by players.
- Release hygiene: nothing merged, no PR open; version bumps and a changelog are not done.

### C. Halloween Tower (CobbleTowers), what the docs say is left
1. **Real bosses.** Floors use stand-ins. P54 wants Pumpkin Rowlet (F3), Witchchu (F5), Scarecrow Eevee (F7), Chucky Marshadow (F9), the Shiny Haunted Phantump / Pumpkin Togepi pair (F10) and Pumpkin Mew (Ascension). Each needs a CobbleRaids raid definition, which lives in CobbleRaids. **Blocked on your yes to add them there, and on the skin aspect ids** and whether a raid definition can set an aspect (unverified).
2. **Spooky Essence payout** (P54 step 3): AscensionLib has no tower-facing call; needs a new `settleEventEssence` in AscensionLib plus a reflective call and a pending-settlement kind in CobbleTowers. Not started; needs your go-ahead to edit AscensionLib.
3. **Trick or Treat doors** and **disguise scouting** (P54): not built. Trick-or-Treat encounters use the floor's normal pool; the **rare catchable encounter** from the CobbleRaids catch screen is wanted but not designed.
4. **Explorable physical tower (P55 steps 2-5):** the generator (`mcschematic`), the walk-graph validator, layout fields and gates behind a `walk` flag, a bot that walks the tower on the rig, then the ToT rooms and retheme. Today the party is still teleported between floors; the current halls are not connected on foot. Invisible barrier blocks for falls belong in the generator's wall spec.
5. **Pedestal polish:** hologram text is small from 7 blocks and long lines clip; names crowd the vendor tag. A **multi-player click in a real client is not seen yet.**
6. **Route map (P56):** finish, push and review what your other session was committing; later stages need new building schematics.
7. P54/P55 docs still say "proposal / nothing built": update their status lines before release.
8. Open question carried from P55: Trick-or-Treat trick pool vs the normal pool is answered (normal pool); event window is Eastern (done).

## 3. Things to check before continuing the tower
- **Which CobbleTowers commit is current?** The cloud clone ends at `ea1f30a` (route map). Your other session ran on your Windows machine (`L:\CobbleTowers`) and was mid-commit; check that its last work reached GitHub before I build on it.
- That session's weekly limit resets Oct 29, 7pm ET; this session can carry on in the meantime.
- With three days left, the safe core for the tower is: stand-in bosses are playable now, so decide whether the real bosses and Essence payout are in or out for 10/31, and cut the walkable tower (P55) to after the event if time is short.
