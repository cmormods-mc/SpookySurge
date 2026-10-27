"""Static preview renderer: composes the bar the way the 1.21.1 client does (sprite, clipped fill, then the title text with its drop shadow)
so the art can be judged before it ever reaches a client. This is a simulation, not a screenshot; real-client captures are separate."""
import json, math, os, random, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import candyhunt as R
import build_assets as B

META = json.load(open(os.path.join(B.HUD, 'candyhunt_metrics.json')))
FRAME = Image.open(os.path.join(HERE, 'frame_full.png')).convert('RGBA')
LIT = Image.open(f'{B.FNT}/tier_lit.png').convert('RGBA')
LETTERS = Image.open(f'{B.FNT}/letters.png').convert('RGBA')
BG_SPRITE = Image.open(f'{B.SPR}/yellow_background.png').convert('RGBA')
PROG = Image.open(f'{B.SPR}/yellow_progress.png').convert('RGBA')
ORDER = R.ORDER


def glyph_image(ch):
    i = ORDER.index(ch)
    cx, cy = (i % R.COLS) * R.CELL_W, (i // R.COLS) * R.CELL_H
    w = len(R.PAT[ch][0])
    return LETTERS.crop((cx, cy, cx + w, cy + R.CELL_H))


def name_run(name):
    """(glyph char, advance) for the printable part of the name; unsupported characters are dropped (same rule as the Java side)."""
    out = []
    for ch in name.upper():
        if ch == ' ': out.append((' ', R.SPACE_ADV))
        elif ch in R.PAT: out.append((ch, META['letters'][ch]['adv']))
    return out


def title_items(name, tier):
    """Mirror of CandyHuntTitle.java: [(image, x, y_top)] in title pixels, plus the total width. Order = draw order."""
    items = [(FRAME, 0, 0)]
    for k in range(min(4, tier)): items.append((LIT, META['tier_lit']['lefts'][k], 39))
    run = name_run(name)
    width = sum(a for _, a in run) - (1 if run and run[-1][0] != ' ' else 0)
    x = META['canvasCenter'] - width // 2
    for ch, adv in run:
        if ch != ' ': items.append((glyph_image(ch), x, 16))
        x += adv
    return items


def draw_title(scene, items, x0, y0):
    """The client draws the whole string's drop shadow first (+1,+1, colour * 0.25), then the string itself."""
    for img, x, y in items:
        a = np.array(img).astype(float)
        a[:, :, :3] *= 0.25
        scene.alpha_composite(Image.fromarray(a.astype(np.uint8), 'RGBA'), (x0 + x + 1, y0 + y + 1))
    for img, x, y in items:
        scene.alpha_composite(img, (x0 + x, y0 + y))


def scene_bg(kind, w, h, seed=3):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (w, h), (0, 0, 0, 255))
    px = im.load()
    for y in range(h):
        for x in range(w):
            if kind == 'dark':
                v = 14 + (rnd.random() * 10) + (8 if (x // 8 + y // 8) % 2 else 0)
                px[x, y] = (int(v * 0.9), int(v), int(v * 1.2), 255)
            else:
                t = y / h
                px[x, y] = (int(120 + 80 * t), int(180 + 50 * t), int(235 + 15 * t), 255)
    if kind == 'bright':
        d = ImageDraw.Draw(im)
        for cx, cy, r in ((40, 20, 18), (60, 24, 14), (250, 12, 20), (270, 16, 12)): d.ellipse((cx - r, cy - r * 0.5, cx + r, cy + r * 0.5), fill=(250, 252, 255, 255))
    return im


def render(name, progress, tier, kind, w=320, h=62, frame=0):
    scene = scene_bg(kind, w, h)
    bar_x, bar_y = w // 2 - 91, 31
    scene.alpha_composite(BG_SPRITE, (bar_x, bar_y))
    fill = int(math.floor(progress * 182))
    if fill > 0: scene.alpha_composite(PROG.crop((0, 5 * frame, fill, 5 * frame + 5)), (bar_x, bar_y))
    draw_title(scene, title_items(name, tier), w // 2 - META['titleWidth'] // 2, 0)
    return scene


def font(sz, bold=False):
    p = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    return ImageFont.truetype(p, sz) if os.path.exists(p) else ImageFont.load_default()


STATES = [(1.0, 4, '100%'), (0.75, 3, '75%'), (0.5, 2, '50%'), (0.25, 1, '25%'), (0.10, 0, '10%'), (0.0, 0, 'Empty')]


def sheet(path, name='SPOOKY SURGE CANDY HUNT'):
    S, Z = 2, 5
    W_, H_ = 320, 62
    pad, lab = 24, 120
    col_w = W_ * S
    rows = len(STATES)
    zoom_w, zoom_h = 100 * Z, 62 * Z
    out = Image.new('RGB', (lab + 2 * col_w + 3 * pad + zoom_w, 110 + rows * (H_ * S + pad) + 60), (24, 22, 32))
    d = ImageDraw.Draw(out)
    d.text((pad, 20), 'The Spooky Surge Candy Hunt: health states', font=font(26, True), fill=(250, 240, 220))
    d.text((pad, 58), 'Simulated 1.21.1 compositing (sprite + clipped fill + title glyphs with drop shadow). GUI scale 2 on the left, 5x pixel inspection of the left end on the right.',
           font=font(13), fill=(170, 164, 190))
    d.text((lab + pad, 88), 'dark background', font=font(14, True), fill=(220, 210, 240)); d.text((lab + 2 * pad + col_w, 88), 'bright background', font=font(14, True), fill=(220, 210, 240))
    d.text((lab + 3 * pad + 2 * col_w, 88), 'left end, x5', font=font(14, True), fill=(220, 210, 240))
    y = 112
    for p, tier, label in STATES:
        d.text((pad, y + 56), label, font=font(22, True), fill=(255, 190, 90))
        for i, kind in enumerate(('dark', 'bright')):
            im = render(name, p, tier, kind).resize((W_ * S, H_ * S), Image.NEAREST)
            out.paste(im.convert('RGB'), (lab + pad + i * (col_w + pad), y))
        z = render(name, p, tier, 'dark').crop((10, 0, 110, 62)).resize((zoom_w, zoom_h), Image.NEAREST)
        out.paste(z.convert('RGB'), (lab + 3 * pad + 2 * col_w, y))
        y += H_ * S + pad
    out.save(path)


if __name__ == '__main__':
    sheet(sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'preview_sheet.png'))
