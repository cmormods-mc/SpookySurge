"""Writes every runtime asset of the Spooky Surge Candy Hunt bar into the mod's resources, plus the metrics file the Java layout uses.

    python3 tools/bossbar-art/build_assets.py            # from the repo root

Outputs (all under src/main/resources):
    assets/minecraft/textures/gui/sprites/boss_bar/yellow_background.png, yellow_progress.png(+.mcmeta)   reserved colour YELLOW
    assets/minecraft/textures/gui/sprites/boss_bar/blue_background.png,   blue_progress.png               reserved colour BLUE: fully transparent spacer
    assets/spookysurge/font/candyhunt.json
    assets/spookysurge/textures/font/candyhunt/{frame_l,frame_r,tier_lit,letters}.png
    assets/spookysurge/hud/candyhunt_metrics.json   (not shipped to clients: glyph code points and advances for the server-side layout)
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import candyhunt as R
from PIL import Image

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
SPR = os.path.join(RES, 'assets', 'minecraft', 'textures', 'gui', 'sprites', 'boss_bar')
FNT = os.path.join(RES, 'assets', 'spookysurge', 'textures', 'font', 'candyhunt')
HUD = os.path.join(RES, 'assets', 'spookysurge', 'hud')
NS = 'spookysurge'
FRAME_L_CP, STUD_CP, FRAME_R_CP = 0xE000, 0xE001, 0xE002
SPLIT = 131           # the 262 px frame is two glyphs: a font glyph may not exceed 256x256 (it is baked into a 256x256 atlas page)
BAR_TOP = 31          # the real bar is the SECOND row: row 0 is the spacer, rows are 19 px apart, the first starts at y = 12


def ascent_for(top):
    """A glyph's top edge lands at (text top + 7 - ascent) and the title's text top is bar_top - 9, so top = bar_top - 2 - ascent."""
    return BAR_TOP - 2 - top


def main():
    for d in (SPR, FNT, HUD, os.path.join(RES, 'assets', NS, 'font')): os.makedirs(d, exist_ok=True)
    # sprites
    R.sprite_background().save(f'{SPR}/yellow_background.png')
    prog = R.sprite_progress(); prog.save(f'{SPR}/yellow_progress.png')
    json.dump({"animation": {"frametime": 4, "width": 182, "height": 5}}, open(f'{SPR}/yellow_progress.png.mcmeta', 'w'))
    for n in ('blue_background', 'blue_progress'): Image.new('RGBA', (182, 5), (0, 0, 0, 0)).save(f'{SPR}/{n}.png')
    # glyph textures
    frame = R.compose().image()
    left, right = frame.crop((0, 0, SPLIT, frame.height)), frame.crop((SPLIT, 0, frame.width, frame.height))
    import numpy as np
    arr = np.array(left)
    if not arr[:, -1, 3].any(): arr[-1, -1] = (0, 0, 0, 1)         # pin the width: bitmap fonts trim empty right-hand columns
    left = Image.fromarray(arr, 'RGBA')
    left.save(f'{FNT}/frame_l.png'); right.save(f'{FNT}/frame_r.png')
    frame.save(os.path.join(HERE, 'frame_full.png'))               # for previews only; never shipped
    stud, box = R.lit_stud(); stud.save(f'{FNT}/tier_lit.png')
    sheet, rows, metrics = R.font_sheet(); sheet.save(f'{FNT}/letters.png')
    providers = [
        {"type": "bitmap", "file": f"{NS}:font/candyhunt/frame_l.png", "ascent": ascent_for(0), "height": frame.height, "chars": [chr(FRAME_L_CP)]},
        {"type": "bitmap", "file": f"{NS}:font/candyhunt/frame_r.png", "ascent": ascent_for(0), "height": frame.height, "chars": [chr(FRAME_R_CP)]},
        {"type": "bitmap", "file": f"{NS}:font/candyhunt/tier_lit.png", "ascent": ascent_for(box[1]), "height": stud.height, "chars": [chr(STUD_CP)]},
        {"type": "bitmap", "file": f"{NS}:font/candyhunt/letters.png", "ascent": ascent_for(16), "height": R.CELL_H, "chars": rows},
    ]
    adv = {}
    for k in range(8):
        v = 1 << k
        adv[chr(0xE200 + v)] = v; adv[chr(0xE300 + v)] = -v
    adv[chr(metrics[' ']['cp'])] = R.SPACE_ADV
    providers.append({"type": "space", "advances": adv})
    json.dump({"providers": providers}, open(f'{RES}/assets/{NS}/font/candyhunt.json', 'w'), indent=1)
    meta = {
        'frame': {'cpLeft': FRAME_L_CP, 'cpRight': FRAME_R_CP, 'advLeft': left.width + 1, 'advRight': right.width + 1, 'split': SPLIT,
                  'width': frame.width, 'height': frame.height},
        'tier_lit': {'cp': STUD_CP, 'adv': stud.width + 1, 'lefts': [x - 6 for x in R.STUD_X]},
        'window': {'x': R.SLOT_X, 'y': R.SLOT_Y, 'w': R.SLOT_W, 'h': R.SLOT_H},
        'plate': {'x0': 58, 'x1': 205}, 'canvasCenter': 131, 'titleWidth': 262,
        'letters': metrics,
    }
    json.dump(meta, open(f'{HUD}/candyhunt_metrics.json', 'w'), indent=1)
    print('wrote', frame.size, stud.size, sheet.size, prog.size)


if __name__ == '__main__':
    main()
