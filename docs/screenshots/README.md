# Screenshots

Taken from a real Minecraft 1.21.1 Fabric client with Cobblemon 1.8.2, software-rendered under Xvfb, joined
to a throwaway test server running this mod. Test conveniences were used: the player was an operator,
night vision was on, and a surge Pokemon was placed in front of the player so the camera could see it.
The catch itself was a real Master Ball throw from the client, paid by the mod.

| File | Shows |
|------|-------|
| `countdown.png` | The purple boss bar counting down to a surge, and the announcement in chat. |
| `candy-corn-in-hotbar.png` | Candy Corn with its own model (from Polymer's resource pack) next to the Master Balls. |
| `catch-pays.png` | After a catch: `+1 Candy Corn` on the action bar, the boss bar at `1 / 100`, the caught Litwick in Cobblemon's party panel. |
| `candy-corn-tooltip.png` | The item's name and lore line in the inventory. |
| `tier-4.png` | The tier announcements in chat, the full boss bar, and the Haunted Visitor announcement. |
| `haunted-visitor.png` | A Haunted Dragapult, a rare surge Pokemon, in front of the player at tier 4. |

The Candy Corn model only appears when the client loads Polymer's resource pack (here it was copied in
by hand). Without it the item looks like sugar with the same name.

## A battle win

Driven through the real client with real key presses and mouse clicks. The player's Garchomp (Lv 90) fought a
wild surge Duskull (Lv 17), and the win was credited by the mod through Cobblemon's battle-victory event.

| File | Shows |
|------|-------|
| `battle-challenge-prompt.png` | Cobblemon's own prompt above a wild surge Pokemon on the crosshair: "Lv. 17 - Press R to battle". |
| `battle-menu.png` | The battle menu: Fight, Switch, Catch, Run. |
| `battle-moves.png` | The move list, with the party Pokemon out and both health bars at the top. |
| `battle-win.png` | The battle log: "Duskull fainted! ShotCam won!" |
| `battle-win-pays.png` | After the win: `+3 Candy Corn` on the action bar, the boss bar at `1 / 100`, the Candy Corn in the hotbar, and `+37 EXP` on the Garchomp. The server logged `Surge credit: ShotCam took a uncommon Duskull (paid 3 Candy Corn, counter 1)`. |

Test conveniences: the player was an operator, was given the Garchomp with `/givepokemon`, and the wild Pokemon was
teleported in front of the player so it stayed on the crosshair.
