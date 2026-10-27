"""The Candy Corn Revenant: every pixel of the boss bar art, drawn by code.

Rendering strategy (see docs/bossbar/README.md): the vanilla boss bar keeps drawing its 182x5 background/progress sprites for the reserved
colour (YELLOW); this script replaces those two sprites, and draws one big FRAME glyph (262x56) that the server puts in the bar's title.
The frame has a 182x5 transparent window over the bar, so the native fill clipping is untouched and nothing important is ever cut by the fill.

Layout (canvas pixels, glyph top-left = 0,0):
    slot (the vanilla bar shows through)   x 40..221   y 31..35
    left medallion: the Revenant's face     x 0..44     y 1..55
    right medallion: candy cluster + hook   x 224..261  y 12..55
    name banner (name letters are overlaid)  x 55..207   y 12..27
"""
import json, math, os, random, sys
import numpy as np
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from pix import Canvas, bevel, shade_pixels, mix, rot, walk

CW, CH = 262, 56
SLOT_X, SLOT_Y, SLOT_W, SLOT_H = 40, 31, 182, 5

# ---- palette: ramps run (shadow, dark, base, light, highlight) ----
OUT = (20, 10, 24, 255)
IVORY = [(138, 118, 100), (196, 178, 150), (236, 224, 196), (255, 246, 224), (255, 255, 244)]
ORANGE = [(110, 36, 8), (178, 66, 12), (240, 116, 20), (255, 158, 46), (255, 206, 112)]
YELLOW = [(140, 90, 8), (208, 142, 14), (250, 196, 36), (255, 224, 90), (255, 244, 162)]
CHAR = [(10, 7, 14), (26, 18, 34), (46, 34, 58), (74, 58, 92), (118, 98, 138)]
PLUM = [(18, 8, 26), (40, 18, 54), (66, 32, 86), (96, 56, 120), (140, 96, 164)]
LIC = [(6, 4, 9), (16, 12, 20), (30, 24, 38), (62, 54, 78), (120, 108, 140)]
VIOLET, GREEN = (176, 120, 255), (150, 255, 170)
CRACK = (96, 40, 18, 255)
RAMPS = [IVORY, ORANGE, YELLOW]


def tuple4(c, a=255): return (*c[:3], a)


# ---------------------------------------------------------------------------------------------------------- candy corn
def candy_pts(cx, top, h, bw, tw, bulge=0.9):
    """Outline of one candy corn: rounded tip, slightly convex sides, softened base corners."""
    n = 16
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = (tw + (bw - tw) * t) / 2 + bulge * math.sin(math.pi * t)
        left.append((cx - half, top + h * t)); right.append((cx + half, top + h * t))
    cap = [(cx + math.cos(math.pi + a * math.pi / 8) * tw / 2, top - math.sin(a * math.pi / 8) * tw * 0.42) for a in range(0, 9)]
    left[-1] = (left[-1][0] + 1.6, left[-1][1]); right[-1] = (right[-1][0] - 1.6, right[-1][1])
    left.insert(-1, (left[-1][0] - 1.6, left[-1][1] - 1.5)); right.insert(-1, (right[-1][0] + 1.6, right[-1][1] - 1.5))
    return cap + right + list(reversed(left))


def draw_candy(c, cx, top, h, bw, tw, deg=0.0, pivot=None, ramps=RAMPS, bands=(0.31, 0.66), outline=OUT, over=False):
    """Draws a bevelled three-band candy corn; returns its mask. Rotation is about `pivot` (default the middle of the candy)."""
    pivot = pivot or (cx, top + h / 2)
    pts = rot(candy_pts(cx, top, h, bw, tw), deg, *pivot)
    shape = c.poly(pts)
    ys, xs = np.nonzero(shape)
    ang = math.radians(-deg)
    band_of = {}
    for y, x in zip(ys, xs):
        dx, dy = x + 0.5 - pivot[0], y + 0.5 - pivot[1]
        ly = pivot[1] + dx * math.sin(ang) + dy * math.cos(ang)
        lx = pivot[0] + dx * math.cos(ang) - dy * math.sin(ang)
        v = (ly - top) / h + 0.012 * math.sin(lx * 0.7)
        band_of[(x, y)] = 0 if v < bands[0] else (1 if v < bands[1] else 2)
    for b in range(3):
        m = np.zeros_like(shape)
        for (x, y), k in band_of.items():
            if k == b: m[y, x] = True
        bevel(c, m, ramps[b], edge=shape)
    if outline is not None: c.outline(outline, base=shape, over=over)
    return shape


# ---------------------------------------------------------------------------------------------------------- the face
def revenant_face(c, cx=22):
    """Left medallion: a charcoal shield with candy-corn teeth round a carved, cracked, grinning candy corn."""
    # shield: a larger candy-corn silhouette in charcoal, with teeth on both slanted sides
    shield = c.poly(candy_pts(cx, 1, 54, 40, 10, 0.4))
    # teeth: small candy corns pointing outward from the slanted sides
    for i, y in enumerate((14, 26, 38, 49)):
        t = (y - 1) / 54.0
        half = (10 + 30 * t) / 2
        for sgn in (-1, 1):
            tx = cx + sgn * (half - 1)
            tooth_pts = [(tx, y - 2.6), (tx + sgn * 4.2, y + 0.4), (tx, y + 2.6)]
            tm = c.poly(tooth_pts)
            ramp = RAMPS[i % 3]
            bevel(c, tm, ramp)
            c.outline(OUT, base=tm)
    bevel(c, shield, CHAR, edge=shield)
    c.outline(OUT, base=shield)
    # shield rivets
    for x, y in ((cx, 5), (cx - 17, 50), (cx + 17, 50)):
        c.put(x, y, CHAR[4]); c.put(x + 1, y + 1, CHAR[0])
    # the candy corn itself, inset
    face = draw_candy(c, cx, 7, 46, 34, 7, outline=OUT)
    # --- carving: brows, eyes, nose, grin
    def poly_paint(pts, col): c.paint(c.poly(pts) & face, col)
    for sgn in (-1, 1):
        poly_paint([(cx + sgn * 10.5, 22.5), (cx + sgn * 1.5, 27.3), (cx + sgn * 1.5, 28.6), (cx + sgn * 10.5, 24.2)], tuple4((62, 22, 48)))       # angry brow
        socket = [(cx + sgn * 9.8, 28.2), (cx + sgn * 2.4, 30.6), (cx + sgn * 3.2, 33.2), (cx + sgn * 9.0, 30.4)]                               # slit eye
        poly_paint(socket, tuple4((22, 8, 34)))
        for k in range(6):                                                                                                                        # spectral core
            x = cx + sgn * (8.0 - k * 1.05); y = 29.2 + k * 0.4 + 0.2
            c.put(round(x), round(y), tuple4(GREEN if k > 1 else VIOLET))
            if k % 2 == 0: c.blend(round(x), round(y) - 1, (150, 255, 170, 110))
        c.blend(cx + sgn * 10, 29, (150, 255, 170, 90)); c.blend(cx + sgn * 10, 30, (150, 255, 170, 70))
    poly_paint([(cx - 2.4, 33.8), (cx + 2.4, 33.8), (cx, 37.2)], tuple4((34, 12, 30)))                                                        # nose
    # grin: dark mouth between two zig-zag edges, ivory teeth
    def ytop(x): return 41.0 + 3.2 * (1 - ((x - cx) / 14.0) ** 2)
    mouth = [(x, ytop(x) - 0.2) for x in np.linspace(cx - 14.5, cx + 14.5, 30)] + [(x, ytop(x) + 7.4) for x in np.linspace(cx + 13, cx - 13, 28)]
    poly_paint(mouth, tuple4((22, 8, 28)))
    for i in range(7):                                                                                                                       # upper fangs
        x0 = cx - 13.3 + i * 4.45
        tooth = [(x0, ytop(x0) - 0.4), (x0 + 3.7, ytop(x0 + 3.7) - 0.4), (x0 + 1.85, ytop(x0 + 1.85) + 3.6)]
        tm = c.poly(tooth) & face
        bevel(c, tm, IVORY)
    for i in range(6):                                                                                                                       # lower teeth
        x0 = cx - 11.0 + i * 4.45
        tooth = [(x0, ytop(x0) + 7.6), (x0 + 3.7, ytop(x0 + 3.7) + 7.6), (x0 + 1.85, ytop(x0 + 1.85) + 4.2)]
        tm = c.poly(tooth) & face
        bevel(c, tm, IVORY)
    # haunted-sugar cracks (jagged dark lines with a pale lip), clipped to the candy
    for seed, (x, y, steps, bias) in enumerate(((cx + 2, 8, 17, (0.3, 1)), (cx - 12, 52, 11, (0.5, -1)), (cx + 10, 36, 8, (0.1, 1)))):
        pts = walk(seed + 11, x, y, steps, bias, 0.75)
        for (px, py) in pts:
            if 0 <= py < c.h and 0 <= px < c.w and face[py, px]:
                c.put(px, py, CRACK); c.blend(px + 1, py, (255, 238, 200, 90)) if face[py, min(c.w - 1, px + 1)] else None
    # glossy streak on the lit side
    for k in range(8):
        x, y = cx - 12 + (k * 0.2), 36 + k
        if face[y, int(x)]: c.blend(int(x), y, (255, 255, 240, 150))
    return face


# ---------------------------------------------------------------------------------------------------------- right ornament
def rope_pixels(c, path, width, ramp=LIC, period=4):
    """A twisted licorice rope along `path`: diagonal gloss stripes, a lit upper-left edge and a dark lower-right edge."""
    dense = []
    for (x0, y0), (x1, y1) in zip(path, path[1:]):
        n = max(2, int(math.hypot(x1 - x0, y1 - y0) * 2))
        for i in range(n): dense.append((x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n))
    m = c.line(path, width)
    edge = m.copy()
    s_len = np.cumsum([0] + [math.hypot(dense[i + 1][0] - dense[i][0], dense[i + 1][1] - dense[i][1]) for i in range(len(dense) - 1)])
    ys, xs = np.nonzero(m)
    pad = np.pad(m, 1)
    for y, x in zip(ys, xs):
        d = [(x + 0.5 - px) ** 2 + (y + 0.5 - py) ** 2 for px, py in dense]
        i = int(np.argmin(d)); s = s_len[i]
        side = (x + 0.5 - dense[i][0]) * 0 + (y + 0.5 - dense[i][1])                    # across-the-rope offset, roughly
        stripe = int((s + side * 1.6) // period) % 2
        col = ramp[3] if stripe else ramp[2]
        up, down = not pad[y, x + 1], not pad[y + 2, x + 1]
        left, right = not pad[y + 1, x], not pad[y + 1, x + 2]
        if (up or left) and not (down or right): col = ramp[4] if stripe else ramp[3]
        elif down or right: col = ramp[1] if stripe else ramp[0]
        c.put(x, y, col)
    c.outline(OUT, base=m)
    return m


def candy_cluster(c, cx=243, base_y=53):
    # a licorice hook arching over the cluster like a cane handle, curling into a bead; drawn first so the candies stand inside it
    hook = [(cx + 12.0 * math.cos(math.radians(a)) + 1.5, 36 + 21.0 * math.sin(math.radians(a))) for a in range(-212, -6, 9)]
    rope_pixels(c, hook, 3.2)
    end = hook[-1]
    bead = c.ellipse(end[0], end[1] + 1.0, 2.9, 2.9)
    bevel(c, bead, LIC, edge=bead); c.outline(OUT, base=bead)
    # three separate candies fanned from one base
    draw_candy(c, cx - 8, 28, 22, 11, 3, deg=-24, pivot=(cx - 3, base_y - 1), over=True)
    draw_candy(c, cx + 8, 28, 22, 11, 3, deg=24, pivot=(cx + 3, base_y - 1), over=True)
    draw_candy(c, cx, 15, 37, 14, 4, over=True)
    # a tiny trapped spectral eye pair in the centre candy's orange band
    for sgn in (-1, 1):
        c.put(cx + sgn * 2, 31, tuple4(GREEN)); c.blend(cx + sgn * 2, 32, (150, 255, 170, 110)); c.blend(cx + sgn * 3, 31, (150, 255, 170, 70))


# ---------------------------------------------------------------------------------------------------------- banner, rails, details
def banner(c):
    pts = [(48.5, 19.5), (54.5, 12), (207.5, 12), (213.5, 19.5), (207.5, 27), (54.5, 27)]
    m = c.poly(pts)
    bevel(c, m, PLUM, edge=m)
    c.outline(OUT, base=m)
    # stitched inner border
    inner = c.poly([(52, 19.5), (56.5, 14), (205.5, 14), (210, 19.5), (205.5, 25.5), (56.5, 25.5)])
    edge = inner & ~np.pad(inner, 1)[2:, 1:-1] | inner & ~np.pad(inner, 1)[:-2, 1:-1] | inner & ~np.pad(inner, 1)[1:-1, 2:] | inner & ~np.pad(inner, 1)[1:-1, :-2]
    ys, xs = np.nonzero(edge)
    for y, x in zip(ys, xs):
        if (x + y) % 2 == 0: c.put(x, y, tuple4(PLUM[4]))
    # the plate behind the letters is darker so ivory text pops
    plate = c.rect(58, 15, 204, 24) & inner
    for y, x in zip(*np.nonzero(plate)): c.put(x, y, tuple4(PLUM[0] if (y + x) % 3 else PLUM[1]))
    # candy-corn spikes along the top edge
    for i, x in enumerate(range(62, 200, 9)):
        if abs(x - 131) < 8: continue
        sm = c.poly([(x - 2.8, 12.2), (x + 2.8, 12.2), (x, 6.2)])
        bevel(c, sm, RAMPS[i % 3]); c.outline(OUT, base=sm)
    # crest: a larger candy corn on the centre line
    draw_candy(c, 131, 1, 13, 10, 2.6)
    # cracked sugar enamel: hairline cracks on the plum
    for seed, (x, y, n) in enumerate(((78, 13, 7), (176, 26, 6), (112, 26, 5), (150, 13, 5))):
        for (px, py) in walk(seed + 40, x, y, n, (0.2, 1 if y < 20 else -1), 0.9):
            if m[py, px]: c.blend(px, py, (255, 240, 220, 70))
    return m


def rails(c):
    x0, x1 = 34, 228
    for (top, flip) in ((27, False), (37, True)):
        for x in range(x0, x1):
            for k in range(3):
                y = top + k
                stripe = ((x + (k * 2 if not flip else (2 - k) * 2)) // 4) % 2
                if k == (0 if not flip else 2): col = LIC[4] if stripe else LIC[3]
                elif k == 1: col = LIC[3] if stripe else LIC[2]
                else: col = LIC[1] if stripe else LIC[0]
                c.put(x, y, col)
    for y in (26, 40):
        for x in range(x0, x1): c.put(x, y, OUT)
    # Row 30 stays EMPTY on purpose: Minecraft draws a title's drop shadow one pixel down and right, so the rope above it
    # casts a dark line exactly onto row 30, which becomes the top lip of the window without touching the bar's own pixels.
    for x in range(x0, x1): c.put(x, 36, OUT)               # bottom lip
    for y in (31, 35):                                      # a 1px candy-gold keyline inside the window edge is drawn by the sprites, not here
        pass


def web(c, ax, ay, bx, by, flip):
    """A tiny cobweb fanning from the corner (ax, ay); flip mirrors it for the right-hand side."""
    sgn = -1 if flip else 1
    rays = [(sgn * 16, 1), (sgn * 14, 6), (sgn * 10, 11), (sgn * 5, 14), (0, 15)]
    for rx, ry in rays:
        n = int(max(abs(rx), abs(ry)))
        for i in range(n + 1):
            c.blend(ax + round(rx * i / n), ay + round(ry * i / n), (240, 236, 255, 150))
    for r in (0.45, 0.75):
        prev = None
        for rx, ry in rays:
            p = (ax + rx * r, ay + ry * r)
            if prev:
                n = int(max(abs(p[0] - prev[0]), abs(p[1] - prev[1]))) or 1
                for i in range(n + 1): c.blend(round(prev[0] + (p[0] - prev[0]) * i / n), round(prev[1] + (p[1] - prev[1]) * i / n + 0.8 * math.sin(i)), (240, 236, 255, 105))
            prev = p


def trapped_eyes(c):
    for x in (78, 184):
        slit = c.rect(x - 3, 28, x + 3, 28) | c.rect(x - 4, 29, x + 4, 29)
        c.paint(c.rect(x - 4, 28, x + 4, 29), tuple4((14, 8, 20)))
        for dx in (-2, 2):
            c.put(x + dx, 28, tuple4(GREEN)); c.put(x + dx, 29, tuple4(VIOLET))
        c.blend(x, 28, (150, 255, 170, 70))


def _unused_pumpkin_studs(c, lit=False, xs=None):
    """Four jack-o'-lantern studs hanging from the lower rail: they mark the tiers. `lit` draws the glowing version (used as overlay glyph)."""
    body, glow = (ORANGE if lit else [(26, 22, 30), (40, 34, 46), (62, 54, 70), (88, 78, 100), (120, 108, 134)]), None
    pumpkin = []
    for k in range(4):
        cx = SLOT_X + (45, 91, 136, 181)[k]
        pumpkin.append(cx)
    return pumpkin


def tier_studs(c, cx, lit):
    ramp = ORANGE if lit else [(22, 18, 28), (52, 44, 60), (82, 72, 96), (114, 102, 132), (150, 138, 170)]
    m = c.ellipse(cx, 45, 4.6, 3.6)
    bevel(c, m, ramp, edge=m)
    c.outline(OUT, base=m)
    c.paint(c.rect(cx - 1, 41, cx, 42), tuple4((70, 120, 40) if lit else (44, 56, 40)))              # stem
    eye = tuple4((255, 238, 130) if lit else (18, 12, 24))
    c.put(cx - 2, 44, eye); c.put(cx + 2, 44, eye)
    for dx in (-1, 0, 1): c.put(cx + dx, 46, eye)


def fangs(c):
    """Candy-corn teeth hanging under the lower rail between the studs."""
    for i, x in enumerate(range(58, 210, 15)):
        if any(abs(x - (SLOT_X + o)) < 9 for o in (45, 91, 136, 181)): continue
        tm = c.poly([(x - 2.6, 40.2), (x + 2.6, 40.2), (x, 46.2)])
        bevel(c, tm, RAMPS[i % 3]); c.outline(OUT, base=tm)


def compose():
    c = Canvas(CW, CH)
    banner(c)
    rails(c)
    web(c, 45, 41, 0, 0, False); web(c, 217, 41, 0, 0, True)
    fangs(c)
    for k in range(4): tier_studs(c, SLOT_X + (45, 91, 136, 181)[k], False)
    trapped_eyes(c)
    candy_cluster(c)
    revenant_face(c)
    # keep the window (the 182x5 slot) completely transparent so the vanilla bar shows through, pixel for pixel
    inside = int(c.a[SLOT_Y:SLOT_Y + SLOT_H, SLOT_X:SLOT_X + SLOT_W, 3].astype(bool).sum())
    if inside: print('intruding pixels:', inside, np.argwhere(c.a[SLOT_Y:SLOT_Y + SLOT_H, SLOT_X:SLOT_X + SLOT_W, 3] > 0)[:12].tolist())
    c.a[CH - 1, CW - 1] = c.a[CH - 1, CW - 1] if c.a[CH - 1, CW - 1, 3] else (0, 0, 0, 1)   # pin the glyph's width (bitmap fonts trim empty right columns)
    return c



STUD_X = [SLOT_X + o for o in (45, 91, 136, 181)]


def lit_stud():
    """The glowing tier light: drawn exactly where the unlit stud sits in the frame, cropped to its own small glyph."""
    c = Canvas(CW, CH)
    tier_studs(c, STUD_X[0], True)
    box = (STUD_X[0] - 6, 39, STUD_X[0] + 7, 51)
    img = c.image().crop(box)
    a = np.array(img)
    if a[-1, -1, 3] == 0: a[-1, -1] = (0, 0, 0, 1)
    return Image.fromarray(a, 'RGBA'), box


# ---------------------------------------------------------------------------------------------------------- bar sprites
def sprite_background():
    c = Canvas(182, 5)
    rows = [(18, 10, 24), (34, 24, 44), (44, 32, 54), (52, 38, 62), (24, 14, 30)]       # drained plum cavity, deeper at the rim
    for y in range(5):
        for x in range(182):
            col = rows[y]
            if y in (1, 2, 3) and (x // 3 + y) % 7 == 0: col = mix(col, (60, 46, 74), 0.5)              # faint grain
            c.put(x, y, col)
    for seed, (x, y, n) in enumerate(((14, 1, 5), (47, 3, 5), (83, 1, 6), (121, 3, 5), (157, 1, 5), (28, 2, 3), (104, 2, 4))):  # hairline cracks
        for (px, py) in walk(seed + 70, x, y, n, (1, 0.35 if y < 2 else -0.35), 0.6):
            if 1 <= py <= 3 and 0 <= px < 182: c.put(px, py, (74, 58, 90, 255))
    for x in range(182):                                                                  # dull inner rim light on the bottom edge
        c.put(x, 4, mix(rows[4], (60, 40, 70), 0.35 if x % 2 else 0.0))
    return c.image()


def sprite_progress(frames=16):
    """Layered candy-corn cross-section: ivory crest, orange body, yellow base, burnt shadow; diagonal seams every 26 px read as separate
    candies so a half-full bar still looks like a row of candy corn. A faint sheen walks along it (restrained: +/- a few levels)."""
    layers = [(255, 248, 226), (255, 170, 60), (238, 112, 20), (250, 196, 38), (176, 98, 12)]
    sheet = Image.new('RGBA', (182, 5 * frames), (0, 0, 0, 0))
    rnd = random.Random(5)
    speck = {(rnd.randrange(182), rnd.randrange(1, 4)) for _ in range(40)}
    for f in range(frames):
        c = Canvas(182, 5)
        sheen_x = (f / frames) * (182 + 40) - 20
        for y in range(5):
            for x in range(182):
                col = layers[y]
                seam = (x + y * 2) % 26
                if seam == 0: col = mix(col, (90, 30, 8), 0.55)
                elif seam == 1 and y < 4: col = mix(col, (255, 244, 200), 0.25)
                if (x, y) in speck: col = mix(col, (255, 255, 240), 0.5)
                d = abs((x + y * 1.5) - sheen_x)
                if d < 7: col = mix(col, (255, 252, 226), 0.28 * (1 - d / 7.0) * (1.0 if y < 3 else 0.5))
                c.put(x, y, col)
        for y in range(5): c.put(181, y, mix(layers[y], (40, 12, 4), 0.5))                  # soft shaded tip so a nearly empty bar still has a round end
        sheet.paste(c.image(), (0, 5 * f))
    return sheet


# ---------------------------------------------------------------------------------------------------------- name font
PAT = {
    'A': ['.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'], 'B': ['####.', '#...#', '#...#', '####.', '#...#', '#...#', '####.'],
    'C': ['.###.', '#...#', '#....', '#....', '#....', '#...#', '.###.'], 'D': ['####.', '#...#', '#...#', '#...#', '#...#', '#...#', '####.'],
    'E': ['#####', '#....', '#....', '####.', '#....', '#....', '#####'], 'F': ['#####', '#....', '#....', '####.', '#....', '#....', '#....'],
    'G': ['.###.', '#...#', '#....', '#.###', '#...#', '#...#', '.###.'], 'H': ['#...#', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'],
    'I': ['###', '.#.', '.#.', '.#.', '.#.', '.#.', '###'], 'J': ['..###', '...#.', '...#.', '...#.', '...#.', '#..#.', '.##..'],
    'K': ['#...#', '#..#.', '#.#..', '##...', '#.#..', '#..#.', '#...#'], 'L': ['#....', '#....', '#....', '#....', '#....', '#....', '#####'],
    'M': ['#...#', '##.##', '#.#.#', '#.#.#', '#...#', '#...#', '#...#'], 'N': ['#...#', '##..#', '#.#.#', '#..##', '#...#', '#...#', '#...#'],
    'O': ['.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'], 'P': ['####.', '#...#', '#...#', '####.', '#....', '#....', '#....'],
    'Q': ['.###.', '#...#', '#...#', '#...#', '#.#.#', '#..#.', '.##.#'], 'R': ['####.', '#...#', '#...#', '####.', '#.#..', '#..#.', '#...#'],
    'S': ['.####', '#....', '#....', '.###.', '....#', '....#', '####.'], 'T': ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..'],
    'U': ['#...#', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'], 'V': ['#...#', '#...#', '#...#', '#...#', '#...#', '.#.#.', '..#..'],
    'W': ['#...#', '#...#', '#...#', '#.#.#', '#.#.#', '##.##', '#...#'], 'X': ['#...#', '#...#', '.#.#.', '..#..', '.#.#.', '#...#', '#...#'],
    'Y': ['#...#', '#...#', '.#.#.', '..#..', '..#..', '..#..', '..#..'], 'Z': ['#####', '....#', '...#.', '..#..', '.#...', '#....', '#####'],
    '0': ['.###.', '#...#', '#..##', '#.#.#', '##..#', '#...#', '.###.'], '1': ['..#..', '.##..', '..#..', '..#..', '..#..', '..#..', '.###.'],
    '2': ['.###.', '#...#', '....#', '...#.', '..#..', '.#...', '#####'], '3': ['####.', '....#', '....#', '.###.', '....#', '....#', '####.'],
    '4': ['...#.', '..##.', '.#.#.', '#..#.', '#####', '...#.', '...#.'], '5': ['#####', '#....', '####.', '....#', '....#', '#...#', '.###.'],
    '6': ['.###.', '#....', '#....', '####.', '#...#', '#...#', '.###.'], '7': ['#####', '....#', '...#.', '..#..', '.#...', '.#...', '.#...'],
    '8': ['.###.', '#...#', '#...#', '.###.', '#...#', '#...#', '.###.'], '9': ['.###.', '#...#', '#...#', '.####', '....#', '....#', '.###.'],
    '.': ['.', '.', '.', '.', '.', '.', '#'], ',': ['.', '.', '.', '.', '.', '#', '#'], "'": ['#', '#', '.', '.', '.', '.', '.'],
    '!': ['#', '#', '#', '#', '#', '.', '#'], '?': ['.###.', '#...#', '....#', '...#.', '..#..', '.....', '..#..'], ':': ['.', '.', '#', '.', '.', '#', '.'],
    '-': ['...', '...', '...', '###', '...', '...', '...'], '&': ['.##..', '#..#.', '.##..', '.#.#.', '#..##', '#..#.', '.##.#'],
    '/': ['....#', '...#.', '...#.', '..#..', '.#...', '.#...', '#....'],
}
ORDER = list('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,\'!?:-&/')
COLS, CELL_W, CELL_H = 12, 5, 14
GLYPH_ROWS = [(255, 252, 236), (255, 244, 208), (255, 230, 168), (255, 214, 130), (255, 190, 96), (246, 160, 70), (226, 130, 52)]   # ivory crest to candy-orange foot
SPACE_ADV = 4
PUA_BASE = 0xE100


def font_sheet():
    rows = (len(ORDER) + COLS - 1) // COLS
    sheet = Image.new('RGBA', (COLS * CELL_W, rows * CELL_H), (0, 0, 0, 0))
    metrics = {}
    chars = [['\u0000'] * COLS for _ in range(rows)]
    for i, ch in enumerate(ORDER):
        pat = PAT[ch]; w = len(pat[0])
        cx, cy = (i % COLS) * CELL_W, (i // COLS) * CELL_H
        for y, row in enumerate(pat):
            for x, b in enumerate(row):
                if b == '#': sheet.putpixel((cx + x, cy + y), (*GLYPH_ROWS[y], 255))
        cp = PUA_BASE + i
        chars[i // COLS][i % COLS] = chr(cp)
        right = max(x for row in pat for x, b in enumerate(row) if b == '#')
        metrics[ch] = {'cp': cp, 'adv': right + 2}                     # Minecraft trims empty RIGHT columns: advance = rightmost inked column + 1, plus 1
    metrics[' '] = {'cp': PUA_BASE + 0xFF, 'adv': SPACE_ADV}
    return sheet, [''.join(r) for r in chars], metrics
