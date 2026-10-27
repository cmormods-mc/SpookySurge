#!/bin/bash
# One attempt to start a real Cobblemon battle against a surge Pokemon, through the client.
# Run it again if the screenshot shows the Pokemon drifted off the crosshair; Cobblemon's own
# "Press R to battle" label above the Pokemon means the target is right.
#
# Needs: the client running and joined (see README), a surge already running, CONSOLE, LOG and OUT set, and a
# strong Pokemon in the party's second slot:  execute as ShotCam run givepokemon ShotCam garchomp level=90
: "${CONSOLE:?set CONSOLE}" "${LOG:?set LOG}" "${OUT:?set OUT}"
export DISPLAY="${DISPLAY:-:99}"
P="${PLAYER:-ShotCam}"; mkdir -p "$OUT"
cmd() { echo "$*" >> "$CONSOLE"; }
shot() { import -window root "$OUT/$1.png"; echo "shot $1"; }
W=$(xdotool search --name "Minecraft" | head -1); xdotool windowfocus "$W"
n=$(wc -c < "$LOG"); cmd "surge status"; sleep 1.5
ID=$(tail -c +$((n+1)) "$LOG" | grep -o 'Surge id: [0-9a-f-]*' | tail -1 | awk '{print $3}')
[ -n "$ID" ] || { echo "no surge running"; exit 1; }
SEL="@e[type=cobblemon:pokemon,nbt={Pokemon:{PersistentData:{spookysurge:\"$ID\"}}},limit=1,sort=nearest]"

[ -n "$SELECT_NEXT_SLOT" ] && xdotool key Down   # the party selection moves with the arrow keys

cmd "execute as $P at @s run tp @s ~ ~ ~ 0 0"; sleep 0.8        # absolute 0 0 levels the view; a relative pitch does not
cmd "execute as $P at @s positioned ^ ^1.2 ^3 run tp $SEL ~ ~ ~"; sleep 1.3   # 1.2 up puts a small Pokemon on the crosshair
shot "battle_1_prompt"
# HOLD the key. A tap is released within one game tick and Cobblemon, which acts on key release, never sees it.
xdotool keydown r; sleep 0.15; xdotool keyup r; sleep 2
shot "battle_2_started"

# Once the battle menu is up (R opens it): Fight at (95,575), first move at (125,568), then wait for the result.
if [ -n "$PLAY_MOVE" ]; then
  xdotool keydown r; sleep 0.15; xdotool keyup r; sleep 2
  xdotool mousemove 95 575; sleep 0.4; xdotool click 1; sleep 2; shot "battle_3_moves"
  xdotool mousemove 125 568; sleep 0.4; xdotool click 1
  for s in 2 4 7 10 14; do sleep 2; shot "battle_4_turn_$s"; done
fi
