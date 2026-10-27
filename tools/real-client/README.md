# Real-client screenshots

Runs a real Minecraft 1.21.1 Fabric client with Cobblemon under Xvfb with software OpenGL, joins a test
server, and captures screenshots while a script drives a surge. Linux only. It is slow and fiddly; the bot
tests in `../bot-test` cover most behaviour. Use this when you need to see the actual game.

Needs: Xvfb, ImageMagick (`import`), `xdotool`, Mesa software GL, Java 21, Python 3, and network access to
Mojang's download hosts. The test server must be in offline mode and read console commands from a file.

1. `python3 fetch_client.py <dir>` downloads the 1.21.1 client jar, libraries and asset index into `<dir>`.
2. Put Fabric API and the Cobblemon jar in `<dir>/game/mods`. Do not add Polymer or this mod: the client
   does not need them. For the Candy Corn model, run `/polymer generate-pack` on the server and copy
   `polymer/resource_pack.zip` to `<dir>/game/resourcepacks/polymer.zip`, then list it in `options.txt`.
3. `CLIENT_DIR=<dir> FABRIC_LIBS=<dir with fabric-loader, asm, mixin, intermediary jars> ./launch.sh` starts
   the client on display `:99` (start `Xvfb :99 -screen 0 1280x720x24 +extension GLX +render -noreset` first).
4. `CONSOLE=<server console file> LOG=<server log> OUT=<dir> ./scene.sh` runs the scene and writes PNGs.

The scene uses test conveniences (operator, night vision, a surge Pokemon teleported in front of the player).

## Battles

`battle.sh` makes one attempt to start a real battle against a surge Pokemon and, with `PLAY_MOVE=1`, plays the
first move. It worked end to end: a Garchomp (Lv 90) beat a surge Duskull and the mod paid +3 Candy Corn. Things
that were not obvious:

- **Hold the key.** Cobblemon acts on key *release* and only if it saw the key down during a game tick, so
  `xdotool key r` (a tap) does nothing. Use `keydown`, a 0.15 s sleep, then `keyup`.
- **Level the view with absolute angles** (`tp @s ~ ~ ~ 0 0`). A relative pitch leaves the camera looking up.
- **Put the target at eye height** (`^ ^1.2 ^3`) so a small Pokemon sits on the crosshair. Flying Pokemon drift, so
  an attempt can miss: run it again. `NoAI` does not stop Cobblemon's Pokemon moving, and `/freezepokemon` did not
  take an argument as expected, so neither is used.
- **Wait for the "Press R to battle" label** above the Pokemon before pressing R.
- **The unicode font** (`fetch_client.py` downloads it) is needed or the battle text renders as boxes. In a
  running client, F3+T reloads resources.
- Battle menu coordinates (1280x720): Fight (95,575); first move (125,568).
