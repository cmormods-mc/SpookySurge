# Candy Corn: guide for the vendor

| | |
|---|---|
| Item id | `spookysurge:candy_corn` |
| Name | Candy Corn |
| Stack size | 64 |
| Looks like | A custom tricolour model with Polymer's resource pack; a sugar item without it. |

It is a real registered item, not a vanilla item with tags, so it can be matched by id anywhere an
item id is accepted: a vendor or shop, a loot table, a recipe, `/give @s spookysurge:candy_corn 5`.

## Giving it out

The mod pays Candy Corn by itself: straight into the inventory of whoever catches or defeats a surge
Pokemon (dropped at their feet if the inventory is full), and a bonus when a surge ends. Change the
amounts in `config/spookysurge/server.json` (`candyCommon`, `candyUncommon`, `candyRare`,
`doubleCandyMultiplier`, `participationBonus`) and run `/surge reload`.

## Selling or accepting it

Set your vendor to take `spookysurge:candy_corn` as payment or to sell it. This mod does not run a
shop; it only provides the item.

## Notes

- The item only exists while this mod is installed. If the mod is removed, existing Candy Corn is
  lost by Minecraft, so keep the mod for as long as the item is in use.
- Candy Corn has no use outside the shops you set up.
