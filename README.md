# Spooky Surge

A server-side Halloween event for Cobblemon. On a schedule, ghost Pokemon surge around every
online player, and a shared server counter unlocks bonus tiers for everyone.

- Minecraft 1.21.1, Fabric, Cobblemon 1.8.2, Polymer 0.9.19
- Server-side only; vanilla clients can join. Item art is served through Polymer's resource pack.

## Candy Corn

The event currency is a registered item, `spookysurge:candy_corn`, usable anywhere an item ID is
accepted (vendors, shops, loot tables, `/give`).

## Status

Early scaffold: the Candy Corn item builds. The surge itself (state, scheduler, spawner, rewards)
is not written yet.

## Build

    ./gradlew build
