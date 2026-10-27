# Staging checklist

Run this once on a test server before the event. Tick each line only when you saw it.

1. Boot. The log shows `Spooky Surge ready` and `Checked N configured species: all known`.
2. `/give @s spookysurge:candy_corn 5`. The item appears named "Candy Corn".
3. With Polymer's resource pack accepted, the item shows the tricolour model; without, a sugar item.
4. `/surge start 10`. A purple "Spooky Surge begins in ..." bossbar counts down, then the surge starts.
5. Ghost Pokemon appear around you with soul-fire particles within a minute. Several per player.
6. Catch one. The action bar says `+N Candy Corn`, the bossbar counter goes up by 1, the item is in
   your inventory, and the log has a `Surge credit:` line.
7. Defeat one in battle. The same happens.
8. `/surge tier 1` to `/surge tier 4`. Each announces itself once; tier 4 spawns a Haunted Visitor
   near you; the Candy Corn per catch doubles from tier 3.
9. Ask a friend to join mid-surge. They get the bossbar and their own spawns.
10. Leave a surge Pokemon alone for `lifetimeSeconds`. It despawns.
11. Restart the server mid-surge. After boot `/surge status` shows the same surge and counter.
12. `/surge stop`. The summary lists the top players; anyone with `participationMin` catches has the
    bonus. The log shows `Surge ... ended: spawned S, credited C, ...` with S above zero.
13. Log out with owed Candy Corn (after step 12), join again. It arrives, or `/surge claim` pays it.
14. Optional: set `xpBonusEnabled` to `true`, `/surge reload`, start a surge at tier 2 or above and
    check a battle's experience is about 25% higher.

## Bots

`tools/bot-test` has mineflayer bots and a scenario suite that exercise steps 4 to 13 (spawning, catch
payouts, tiers, the reward cap, the end bonus and an offline claim, the Visitor, expiry, and several players)
against a throwaway server. They do not replace a human run: they cannot see models, particles, the resource
pack, or battle wins. See `tools/bot-test/README.md`.
