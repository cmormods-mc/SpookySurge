# Spooky Surge

A server-side Halloween event for Cobblemon. On a schedule, ghost Pokemon surge around every online
player, every catch or defeat fills one shared counter, and the whole server unlocks four tiers of
bonuses together. Players earn **Candy Corn**, a custom item you can sell or accept in a vendor.

- Minecraft 1.21.1, Fabric, Cobblemon 1.8.2, Polymer 0.9.19
- Server-side only; vanilla clients can join. The Candy Corn model is served through Polymer's
  resource pack.

## Docs

- [Operator guide](docs/OPERATOR-GUIDE.md): how a surge plays, commands, and every config key.
- [Candy Corn guide](docs/CANDY-CORN.md): the item id and how to use it in a vendor.
- [Boss bar artwork](docs/bossbar/README.md): the Spooky Surge Candy Hunt bar, how it is rendered, reserved colours, pack delivery, limits ([validation report](docs/bossbar/VALIDATION.md)).
- [Staging checklist](docs/STAGING-CHECKLIST.md): what to check on a test server before the event.

## Screenshots

Real-client screenshots (a Minecraft 1.21.1 Fabric client with Cobblemon, joined to a test server) are in
[docs/screenshots](docs/screenshots/README.md): the countdown, a catch and a battle win paying Candy Corn, the
item's model and tooltip, and the Haunted Visitor.

## Candy Corn

The item id is `spookysurge:candy_corn`.

## Build

    ./gradlew build

Unit tests cover the rules (tier maths, rewards, caps, the schedule clock, config repair, and the
saved state). Catching and rewards need a real player and are covered by the staging checklist.
