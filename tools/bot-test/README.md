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
