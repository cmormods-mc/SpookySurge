"""The Spooky Surge Candy Hunt: every pixel of the boss bar art, drawn by code.

Rendering strategy (see docs/bossbar/README.md): the vanilla boss bar keeps drawing its 182x5 background/progress sprites for the reserved
colour (YELLOW); this script replaces those two sprites, and draws one big FRAME glyph (262x56) that the server puts in the bar's title.
The frame has a 182x5 transparent window over the bar, so the native fill clipping is untouched and nothing important is ever cut by the fill.

Layout (canvas pixels, glyph top-left = 0,0):
    slot (the vanilla bar shows through)   x 40..221   y 31..35
    left medallion: jack-o'-lantern candy pail  x 0..44     y 1..55
    right medallion: ghost + candy bowl         x 224..261  y 6..55
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


WRAPPERS = [((176, 96, 230), (255, 152, 30)), ((96, 210, 130), (255, 248, 228)), ((255, 110, 56), (255, 214, 56)), ((90, 170, 240), (255, 248, 228))]


def ramp_of(col):
    return [mix(col, (0, 0, 0), 0.62), mix(col, (0, 0, 0), 0.34), col, mix(col, (255, 255, 255), 0.3), mix(col, (255, 255, 255), 0.62)]


def wrapped_candy(c, cx, cy, deg=0.0, wrap=0, length=12):
    """A twisted-wrapper sweet: a bevelled oval, a stripe across the middle and a fan of wrapper at each end."""
    col, stripe = WRAPPERS[wrap % len(WRAPPERS)]
    half = length / 2.0
    oval = [(cx + math.cos(a) * (half - 2), cy + math.sin(a) * 3.6) for a in [i * math.pi / 12 for i in range(24)]]
    ends = []
    for sgn in (-1, 1):
        ends.append([(cx + sgn * (half - 3), cy), (cx + sgn * (half + 2.2), cy - 3.4), (cx + sgn * (half + 2.2), cy + 3.4)])
    body = c.poly(rot(oval, deg, cx, cy))
    fans = [c.poly(rot(e, deg, cx, cy)) for e in ends]
    whole = body | fans[0] | fans[1]
    bevel(c, whole, ramp_of(col), edge=whole)
    ang = math.radians(-deg)
    for y, x in zip(*np.nonzero(body)):
        lx = (x + 0.5 - cx) * math.cos(ang) - (y + 0.5 - cy) * math.sin(ang)
        if abs(lx) < 1.3: c.put(x, y, stripe + (255,))
    c.outline(OUT, base=whole, over=True)


def lollipop(c, cx, cy, r=5.2, stick=(3, 14)):
    c.paint(c.line([(cx, cy + r - 1), (cx + stick[0], cy + stick[1])], 1.4), IVORY[2] + (255,))
    disc = c.ellipse(cx, cy, r, r)
    for y, x in zip(*np.nonzero(disc)):
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        t = (math.atan2(dy, dx) / (2 * math.pi) + math.hypot(dx, dy) / r * 1.4) % 1.0
        shade = 0.0 if (dx + dy) < 0 else -0.25
        base = ORANGE[2] if t < 0.5 else IVORY[3]
        c.put(x, y, mix(base, (0, 0, 0), -shade if shade < 0 else 0) + (255,))
    c.outline(OUT, base=disc, over=True)
    c.put(int(cx - 2), int(cy - 3), (255, 255, 255, 255))


def pumpkin_pail(c, cx=22):
    """Left medallion: a jack-o'-lantern trick-or-treat pail brimming with candy, under a licorice handle."""
    handle = [(cx + 17.5 * math.cos(math.radians(a)), 25 + 22.0 * math.sin(math.radians(a))) for a in range(180, 361, 10)]
    rope_pixels(c, handle, 2.8)
    # the candy pile, drawn behind the rim
    draw_candy(c, cx - 11, 7, 15, 9, 2.6, deg=-34, over=True)
    draw_candy(c, cx + 11, 6, 15, 9, 2.6, deg=32, over=True)
    lollipop(c, cx + 9, 14, 5.0, stick=(5, 12))
    wrapped_candy(c, cx - 8, 18, deg=-18, wrap=0)
    draw_candy(c, cx, 5, 17, 10, 3, deg=4, over=True)
    wrapped_candy(c, cx + 2, 21, deg=10, wrap=1, length=11)
    wrapped_candy(c, cx - 14, 22, deg=24, wrap=3, length=10)
    # the pumpkin pail
    body = c.ellipse(cx - 1, 40, 18.6, 15.2)
    bevel(c, body, ORANGE, edge=body)
    for k in (-13, -6.5, 6.5, 13):                                         # ribs follow the curve of the pumpkin
        for y in range(26, 56):
            f = 1 - ((y - 40) / 15.2) ** 2
            if f <= 0: continue
            x = int(round(cx + k * math.sqrt(f)))
            if 0 <= x < c.w and body[y, x]:
                c.put(x, y, tuple4(ORANGE[1])); c.put(x + (1 if k < 0 else -1), y, tuple4(ORANGE[3]))
    c.outline(OUT, base=body)
    # carved face, lit from inside
    glow, edge = (255, 232, 130, 255), (120, 44, 10, 255)
    for sgn in (-1, 1):
        eye = c.poly([(cx + sgn * 4.2, 41.2), (cx + sgn * 11.2, 41.2), (cx + sgn * 7.7, 34.6)])
        c.paint(eye, glow); c.outline(edge, base=eye)
        c.put(cx + sgn * 7, 39, (255, 255, 240, 255))
    nose = c.poly([(cx - 2, 43.2), (cx + 2, 43.2), (cx, 40.8)])
    c.paint(nose, glow)
    mouth = [(x, 45.0 + 4.2 * (1 - ((x - cx) / 13.0) ** 2)) for x in np.linspace(cx - 13, cx + 13, 14)] + \
            [(x, 47.8 + 6.0 * (1 - ((x - cx) / 13.5) ** 2)) for x in np.linspace(cx + 13, cx - 13, 14)]
    m = c.poly(mouth)
    c.paint(m, glow); c.outline(edge, base=m)
    for i in range(6):                                                      # the grin's teeth
        x = cx - 10.5 + i * 4.2
        y0 = 45.0 + 4.2 * (1 - ((x - cx) / 13.0) ** 2) + 0.6
        t = c.poly([(x, y0), (x + 2.6, y0), (x + 1.3, y0 + 2.4)]) & m
        c.paint(t, edge)
    for i in range(5):
        x = cx - 8.4 + i * 4.2
        y1 = 47.8 + 6.0 * (1 - ((x - cx) / 13.5) ** 2) - 0.6
        t = c.poly([(x, y1), (x + 2.6, y1), (x + 1.3, y1 - 2.4)]) & m
        c.paint(t, edge)
    # the pail's rim: a charcoal band with rivets, in front of the candy and the pumpkin top
    rim = c.poly([(cx - 20, 24), (cx + 20, 24), (cx + 18.6, 29.4), (cx - 18.6, 29.4)])
    bevel(c, rim, CHAR, edge=rim); c.outline(OUT, base=rim, over=True)
    for x in (cx - 15, cx - 5, cx + 5, cx + 15): c.put(x, 26, tuple4(CHAR[4])); c.put(x, 27, tuple4(CHAR[0]))


def ghost_bowl(c, cx=242):
    """Right medallion: a friendly spectral ghost rising from a licorice bowl of wrapped candy."""
    ghost_pts = [(cx - 10, 36)] + [(cx - 10 * math.cos(a), 16 - 11 * math.sin(a)) for a in [i * math.pi / 14 for i in range(15)]] + [(cx + 10, 36)]
    ghost = c.poly(ghost_pts)
    bevel(c, ghost, [(96, 84, 140), (150, 138, 196), (212, 206, 236), (240, 238, 252), (255, 255, 255)], edge=ghost)
    c.outline(OUT, base=ghost)
    for sgn in (-1, 1):
        eye = c.ellipse(cx + sgn * 4.0, 16.5, 1.9, 2.8)
        c.paint(eye, (30, 14, 50, 255))
        c.put(cx + sgn * 4, 17, tuple4(GREEN)); c.blend(cx + sgn * 4, 18, (150, 255, 170, 110))
    c.paint(c.ellipse(cx, 23.4, 1.5, 2.0), (30, 14, 50, 255))
    for sgn in (-1, 1):                                                      # little arms reaching over the rim
        arm = c.line([(cx + sgn * 9, 30), (cx + sgn * 13, 37)], 3.0)
        bevel(c, arm, [(96, 84, 140), (150, 138, 196), (212, 206, 236), (240, 238, 252), (255, 255, 255)], edge=arm)
        c.outline(OUT, base=arm, over=True)
    # sweets heaped in front of the ghost, then the bowl
    wrapped_candy(c, cx - 10, 41, deg=-24, wrap=2, length=11)
    wrapped_candy(c, cx + 10, 40, deg=22, wrap=0, length=11)
    draw_candy(c, cx - 2, 31, 15, 9, 2.6, deg=-8, over=True)
    draw_candy(c, cx + 5, 34, 12, 8, 2.4, deg=24, over=True)
    wrapped_candy(c, cx, 44, deg=4, wrap=1, length=12)
    bowl = c.poly([(cx - 18, 44), (cx + 18, 44), (cx + 15, 52), (cx + 8, 55.4), (cx - 8, 55.4), (cx - 15, 52)])
    bevel(c, bowl, LIC, edge=bowl); c.outline(OUT, base=bowl, over=True)
    for x in range(int(cx - 16), int(cx + 16), 4):                          # twisted-licorice stripes on the bowl
        for y in range(46, 55): c.put(x + (y - 46) // 3, y, tuple4(LIC[4])) if bowl[y, min(c.w - 1, x + (y - 46) // 3)] else None


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
    ghost_bowl(c)
    pumpkin_pail(c)
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
