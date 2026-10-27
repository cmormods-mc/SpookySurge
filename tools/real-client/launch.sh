#!/bin/bash
# Starts the client under software OpenGL. Set CLIENT_DIR (from fetch_client.py) and FABRIC_LIBS (a folder of
# fabric-loader, asm, sponge-mixin, tiny-remapper, mapping-io, class-tweaker and intermediary-1.21.1 jars).
: "${CLIENT_DIR:?set CLIENT_DIR}" "${FABRIC_LIBS:?set FABRIC_LIBS}"
export DISPLAY="${DISPLAY:-:99}" LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450
GAME="$CLIENT_DIR/game"; mkdir -p "$GAME/mods" "$CLIENT_DIR/natives"
FL=$(ls "$FABRIC_LIBS"/*.jar | tr '\n' ':')
ML=$(sed "s|^|$CLIENT_DIR/|" "$CLIENT_DIR/libs.txt" | tr '\n' ':')
cd "$GAME" && exec java -Xmx3G -Djava.library.path="$CLIENT_DIR/natives" -Dfabric.gameJarPath="$CLIENT_DIR/client.jar" \
  -cp "$FL$ML$CLIENT_DIR/client.jar" net.fabricmc.loader.impl.launch.knot.KnotClient \
  --username "${SHOT_NAME:-ShotCam}" --version 1.21.1 --gameDir "$GAME" --assetsDir "$CLIENT_DIR/assets" --assetIndex 17 \
  --uuid 00000000-0000-0000-0000-0000000000a1 --accessToken 0 --userType legacy --width 1280 --height 720 \
  ${SHOT_ARGS:---quickPlayMultiplayer localhost:25565}
