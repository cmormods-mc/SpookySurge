# Bot tests

mineflayer bots that join a test server and record what a player would see during a surge. They are
for a throwaway test server (offline mode, you can op the bot from the console), not for production.

A vanilla-protocol client cannot parse Cobblemon's custom recipe types, so `surgebot.js` treats the
recipes packet as opaque bytes. That is the only tweak a bot needs to join a Cobblemon server.

    npm install mineflayer@4.39.0          # Node 22 or newer
    export NODE_PATH=$PWD/node_modules

- `surgebot.js`: the library. Records chat, action bar, titles, boss bars and inventory to JSON lines.
- `observe.js <name> <seconds> <outPrefix>`: join, wait, write the events and a final snapshot.
- `catch.js <name> <surgeId>`: join, get a Master Ball, have a surge Pokemon placed in front of the
  bot, throw, and check from the server log that the catch was paid. Needs `CONSOLE` (a file the
  server reads commands from), `LOG` (the server log) and optionally `OUT`.

Find the surge id with `/surge status` as an operator. Bots cannot tell Cobblemon's entity types
apart, so the scripts pick a surge Pokemon on the server by its NBT tag and teleport it in front of
the bot. They cannot open Cobblemon's battle screen, so battle wins are not covered.

## The scenario suite

`scenarios.js` runs each behaviour against a throwaway server and prints PASS or FAIL with the evidence,
writing JSON results and a screenshot panel per scenario:

    CONSOLE=<file the server reads commands from> LOG=<server log> \
    CONFIG=<config/spookysurge/server.json> OUT=./out node scenarios.js [scenario names]

| Scenario | Checks |
|----------|--------|
| `catch_pays` | A Master Ball catch pays the rarity's Candy Corn; action bar `+N Candy Corn`; boss bar counter moves. |
| `tier3_doubles` | From tier 3 the payout is doubled. |
| `reward_cap` | With `perPlayerRewardCap` 2, the third catch pays 0 but still counts (`Surge counter +1`). |
| `end_bonus_and_offline_claim` | A player who left before the surge ended gets the participation bonus on rejoining. |
| `visitor_at_tier4` | Tier 4 announces a Haunted Visitor; a rare catch pays 12 at tier 4. |
| `untouched_pokemon_expire` | With `lifetimeSeconds` 30, untouched surge Pokemon expire and are replaced. |
| `three_players_share_the_counter` | Three bots share one counter and one boss bar; `/surge top` lists all three. |

The scenarios edit the server's config and reload it, so run them only against a throwaway server.

`panel.js` renders what a bot received (boss bar, action bar, chat, inventory, nearby entities) to a PNG.
It is a picture of the data the bot got, not a render from a Minecraft client, so it cannot show Cobblemon
models, particles or the Candy Corn texture.

What the bots cannot cover: battle wins (no battle screen), the Candy Corn resource-pack model, and how
anything looks in a real client.
