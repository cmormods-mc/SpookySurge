#!/bin/bash
# Needs CONSOLE (file the server reads commands from), LOG (the server log) and OUT (where PNGs go).
: "${CONSOLE:?set CONSOLE}" "${LOG:?set LOG}" "${OUT:?set OUT}"
export DISPLAY=:99
R=$OUT; mkdir -p $R; rm -f $R/*.png
P=ShotCam
cmd() { echo "$*" >> $CONSOLE; sleep 0.4; }
shot() { import -window root "$R/$1.png"; echo "shot $1"; }
W=$(xdotool search --name "Minecraft" | head -1); xdotool windowfocus $W
sid() { local n=$(wc -c < $LOG); cmd "surge status"; sleep 1.5; tail -c +$((n+1)) $LOG | grep -o 'Surge id: [0-9a-f-]*' | tail -1 | awk '{print $3}'; }
credits() { grep -c "Surge credit: $P took" $LOG; }

cmd "surge stop"; sleep 1
cmd "op $P"; cmd "gamemode survival $P"; cmd "clear $P"
cmd "gamerule doDaylightCycle false"; cmd "time set 14500"; cmd "weather clear"
cmd "effect give $P minecraft:night_vision infinite 0 true"
cmd "give $P cobblemon:master_ball 16"; cmd "give $P spookysurge:candy_corn 3"; sleep 2
shot 00_hotbar_candy_corn_model
cmd "clear $P spookysurge:candy_corn"
cmd "surge start 0"; sleep 25
ID=$(sid); echo "surge id $ID"
SEL="@e[type=cobblemon:pokemon,nbt={Pokemon:{PersistentData:{spookysurge:\"$ID\"}}},limit=1,sort=nearest]"
RSEL="@e[type=cobblemon:pokemon,nbt={Pokemon:{PersistentData:{spookysurge:\"$ID\",spookysurge_rarity:\"rare\"}}},limit=1,sort=nearest]"
AHEAD="execute as $P at @s positioned ^ ^1.3 ^3.5 run"
place() { cmd "execute as $P at @s run tp @s ~ ~ ~ ~ 0"; sleep 0.6; cmd "$AHEAD tp $1 ~ ~ ~"; cmd "$AHEAD data merge entity $1 {NoAI:1b,NoGravity:1b}"; sleep 1.5; }
BEFORE=$(credits)
xdotool key 1; sleep 0.3
for attempt in 1 2 3; do
  place "$SEL"; [ $attempt = 1 ] && shot 01_surge_pokemon_in_front
  xdotool click 3; sleep 0.6; [ $attempt = 1 ] && shot 02_ball_thrown
  for i in 1 2 3 4 5 6 7 8; do sleep 1.1; [ $attempt = 1 ] && shot 03_capture_$i; [ $(credits) -gt $BEFORE ] && break; done
  [ $(credits) -gt $BEFORE ] && break
done
if [ $(credits) -gt $BEFORE ]; then echo "caught on attempt $attempt"; sleep 0.4; shot 04_caught_action_bar; sleep 1.5; shot 05_after_catch_counter; else echo "no catch"; fi
xdotool key e; sleep 1.5; xdotool mousemove 531 493; sleep 1; shot 06_inventory_candy_corn_tooltip
xdotool key Escape; sleep 1
cmd "surge tier 3"; sleep 2; shot 07_tier3
cmd "surge tier 4"; sleep 4
place "$RSEL"; shot 08_haunted_visitor_in_front
echo scene2-done
