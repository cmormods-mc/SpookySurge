# Staging checklist

Run this once on a test server before the event. Tick each line only when you saw it.

1. Boot. The log shows `Spooky Surge ready` and `Checked N configured species: all known`.
2. `/give @s spookysurge:candy_corn 5`. The item appears named "Candy Corn".
3. With Polymer's resource pack accepted, the item shows the tricolour model; without, a sugar item.
4. `/surge start 10`. A purple "Spooky Surge begins in ..." bossbar counts down, then the surge starts.
5. Ghost Pokemon appear around you with soul-fire particles within a minute. Several per player.
6. Catch one. The action bar says `+N Candy Corn`, the bossbar counter goes up by 1, the item is in
   your inventory, and the log has a `Surge credit:` line.
7. Defeat one in battle (stand near it, hold R on it for the "Press R to battle" prompt). The same happens. This was verified with a real client: the win paid, the action bar showed `+N Candy Corn`.
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

15. Boss bar artwork: with the pack accepted, `/surge bar preview 100 4` shows the candy-corn frame with four lit
    pumpkins and `/surge bar preview 20 1 A very long boss name that does not fit` cuts the name with `...`.
    Decline the pack on a second account (or clear `servers.dat`): the preview must say it shows the plain
    fallback and you see one ordinary yellow bar. Details in [bossbar/VALIDATION.md](bossbar/VALIDATION.md).
16. Confirm no other mod on the server uses YELLOW or BLUE boss bars (the artwork replaces those sprites for everyone).

17. With CobbleRaids 0.8.191+ and the World Boss `enabled: true`, `auto_start: false`: `/surge start`, then `/surge tier 4`. The chat says the World Boss is
    summoned, CobbleRaids announces the boss and its coordinates, and the log has `tier 4 summoned the World Boss`. Walk to it: its bar appears under the Candy Hunt bar.
    With the World Boss disabled, tier 4 spawns the Haunted Visitor instead and the log says why.

## Bots

`tools/bot-test` has mineflayer bots and a scenario suite that exercise steps 4 to 13 (spawning, catch
payouts, tiers, the reward cap, the end bonus and an offline claim, the Visitor, expiry, and several players)
against a throwaway server. They do not replace a human run: they cannot see models, particles, the resource
pack, or battle wins. See `tools/bot-test/README.md`. `tools/real-client` can drive a real client under
Xvfb for screenshots (a catch, a battle win, the Candy Corn model and tooltip, the Visitor).
