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
