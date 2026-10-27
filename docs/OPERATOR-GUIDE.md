# Spooky Surge: operator guide

A server-side Halloween event for Cobblemon. At scheduled times a **surge** runs for a set time:
ghost Pokemon spawn around every online player, every catch or defeat adds to one shared counter,
and the counter unlocks four tiers for the whole server. Players are paid **Candy Corn**.

Needs Minecraft 1.21.1, Fabric Loader 0.17.2+, Fabric API, Cobblemon 1.8.2+ and Polymer 0.9.19+.
Vanilla clients can join; nothing is installed on the client. Players who accept Polymer's resource
pack see the Candy Corn model; everyone else sees a sugar item with the same name.

## Install

Drop `SpookySurge-<version>.jar` in `mods/` next to Cobblemon and Polymer. On first start the mod
writes `config/spookysurge/server.json` with every key at its default.

## The event in one minute

1. At a scheduled time (default 20:00 America/New_York) a 60 second countdown runs on a bossbar.
2. For 30 minutes, surge Pokemon spawn near each online player in the overworld. They are ordinary
   wild Pokemon, tagged on the Pokemon so the tag survives a catch.
3. Catching or defeating one pays Candy Corn to that player and adds 1 to the shared counter.
4. The counter target is `targetPerPlayer` times the players online, kept between `targetMin` and
   `targetMax`. Tiers unlock at `tierFractions` of the target (25%, 50%, 75%, 100%):

| Tier | Effect |
|------|--------|
| 1 | Surge Pokemon spawn faster (`boostedSpawnIntervalTicks`). |
| 2 | Shiny odds multiplied by `shinyMultiplier` for surge Pokemon. Optional XP bonus (see `xpBonusEnabled`). |
| 3 | Candy Corn payouts multiplied by `doubleCandyMultiplier`. |
| 4 | One rare "Haunted Visitor" spawns near the current top contributor. |

5. When the surge ends, leftover surge Pokemon are removed and a summary lists the top five.
   Anyone with at least `participationMin` catches or defeats gets `participationBonus` Candy Corn.
   Players who are offline get it when they next join, or with `/surge claim`.

## Commands

| Command | Who | What it does |
|---------|-----|--------------|
| `/surge` or `/surge status` | everyone | Phase, tier, counter, time left, and your own count. |
| `/surge top` | everyone | The ten players with the most catches this surge. |
| `/surge claim` | everyone | Collect Candy Corn owed to you. |
| `/surge start [delaySeconds]` | op level 2 | Start a surge now (or after a countdown). |
| `/surge stop` | op level 2 | End the surge, pay bonuses, remove leftover Pokemon. |
| `/surge reload` | op level 2 | Re-read the config and re-check species ids. |
| `/surge add <n>` | op level 2 | Add to (or subtract from) the counter. For testing. |
| `/surge tier <1-4>` | op level 2 | Raise the counter to a tier's threshold. For testing. |

## Config reference: `config/spookysurge/server.json`

A bad value is clamped to its allowed range and logged as `Config repaired: ...`; the server still
starts. Species ids are checked against Cobblemon at start and on reload; an unknown id logs one
warning and never spawns. After upgrading the mod, new keys take their defaults. Delete
`server.json` to have it rewritten with every current key.

| Key | Default | Range | Meaning |
|-----|---------|-------|---------|
| `enabled` | `true` | | Master switch for scheduled surges. |
| `windowStart`, `windowEnd` | 2026-10-31T04:00Z, 2026-11-03T05:00Z | ISO instants | Scheduled surges only start inside this window. |
| `timezone` | `America/New_York` | any Java zone id | Zone `startTimes` are read in. |
| `startTimes` | `["20:00"]` | `HH:mm` list | Local start times, each day of the window. |
| `countdownSeconds` | 60 | 0-600 | Countdown before a scheduled surge. |
| `durationMinutes` | 30 | 1-240 | Length of the active phase. |
| `spawnIntervalTicks` | 100 | 20-1200 | Ticks between spawn attempts per player. |
| `boostedSpawnIntervalTicks` | 60 | 20 to `spawnIntervalTicks` | The same, from tier 1. |
| `perPlayerCap` | 4 | 1-20 | Live surge Pokemon near one player. |
| `serverCap` | 40 | 1-200 | Live surge Pokemon on the whole server. |
| `maxSpawnsPerSecond` | 5 | 1-100 | Spawns started per second across all players. Spreads the first wave so a surge start is not one slow tick. |
| `minSpawnDistance`, `maxSpawnDistance` | 12, 32 | 4-64, min+4 to 96 | Ring around the player they spawn in. |
| `lifetimeSeconds` | 300 | 30-3600 | Untouched surge Pokemon despawn after this. |
| `levelMin`, `levelMax` | 10, 35 | 1-100 | Wild level range. |
| `targetPerPlayer`, `targetMin`, `targetMax` | 60, 100, 1000 | | Shared counter target. |
| `tierFractions` | `[0.25, 0.5, 0.75, 1.0]` | four increasing values in (0, 1] | When tiers 1 to 4 unlock. |
| `candyCommon`, `candyUncommon`, `candyRare` | 1, 3, 6 | 0-64 | Candy Corn per catch by species rarity. |
| `doubleCandyTier`, `doubleCandyMultiplier` | 3, 2 | 1-4, 1-10 | When and how much payouts are multiplied. |
| `participationBonus`, `participationMin` | 5, 3 | 0-64, 0-1000 | End-of-surge bonus and the actions needed for it. |
| `perPlayerRewardCap` | 60 | 1-10000 | Catches paid per player per surge. Beyond it, a catch still moves the counter but pays nothing. |
| `shinyMultiplier`, `shinyTier` | 3.0, 2 | 1-100, 1-4 | Shiny boost for surge Pokemon. |
| `visitorTier`, `visitorSpecies` | 4, a four-species list | | The Haunted Visitor. |
| `xpBonusEnabled`, `xpBonusPercent`, `xpBonusTier` | `false`, 25, 2 | 0-500, 1-4 | Optional XP boost while a surge runs. Off by default. |
| `species` | 17 spooky species | | `{ "id": "gastly", "weight": 10, "rarity": "common" }`. Rarity is `common`, `uncommon` or `rare` and sets the payout. |

## Logs

- `Surge credit: <player> took a <rarity> <species> (paid N Candy Corn, counter C)` for each reward.
- `Surge <id> ended: spawned S, credited C, expired E, counter N, tier T` when a surge ends. If this
  says `spawned 0` while players were online, spawning is failing; report the log.
- `Config repaired: ...` and `Config species '<id>' is not a known Cobblemon species` on bad config.

## For other mods

`com.spookysurge.api.SurgeEndedCallback.EVENT` fires on the server thread when a running surge ends,
with the surge id, final tier, counter, target and a map of player to catch count. A listener that
throws is logged and cannot stop the surge. This lets AscensionLib, for example, pay Spooky Essence
without this mod depending on it.

## Restarts

Surge state is saved with the overworld. A restart mid-surge resumes the same surge on its original
deadline. Leftover surge Pokemon from before the restart are removed when they load.
