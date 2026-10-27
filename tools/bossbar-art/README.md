# bossbar-art

Generates every pixel of the Candy Corn Revenant boss bar. Python 3, Pillow and numpy only.

| Script | Purpose |
|---|---|
| `build_assets.py` | writes the sprites, glyph PNGs, font JSON and metrics into `src/main/resources` (run from anywhere) |
| `revenant.py`, `pix.py` | the art and a small pixel toolkit (supersampled masks, bevel shading, ordered dither) |
| `title.py` | Python twin of `RevenantTitle.java`; `python3 title.py` rewrites the golden file the Java test compares against |
| `preview.py` | simulated compositing sheet (sprite, clipped fill, title with drop shadow) |
| `make_preview_pack.py` | `docs/bossbar/revenant-preview-pack.zip` and `demo_commands.txt` |
| `capture_states.py`, `real_sheet.py` | drive a running test client and assemble the real-client sheet |

After changing the art: `python3 tools/bossbar-art/build_assets.py && python3 tools/bossbar-art/title.py && ./gradlew build`.
See `docs/bossbar/README.md`.
