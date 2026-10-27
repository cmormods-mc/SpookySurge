"""A tiny pixel-art toolkit: crisp masks from supersampled shapes, bevel shading with a fixed top-left light, ordered dithering,
outlines. Pure Pillow + numpy, no randomness except explicitly seeded generators, so a clean checkout reproduces the same pixels."""
import math
import numpy as np
from PIL import Image, ImageDraw

SS = 8                                  # supersampling used only to find which pixels a shape covers
BAYER = np.array([[0, 2], [3, 1]]) / 4.0


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 4), np.uint8)

    # ------------------------------------------------------------------ masks
    def mask(self, draw_fn):
        """draw_fn(ImageDraw, scale) draws white on black at SS x resolution; returns a boolean mask at 1x."""
        im = Image.new('L', (self.w * SS, self.h * SS), 0)
        draw_fn(ImageDraw.Draw(im), SS)
        return np.asarray(im.resize((self.w, self.h), Image.BOX)) > 127

    def poly(self, pts):
        return self.mask(lambda d, s: d.polygon([(x * s, y * s) for x, y in pts], fill=255))

    def ellipse(self, cx, cy, rx, ry):
        return self.mask(lambda d, s: d.ellipse([(cx - rx) * s, (cy - ry) * s, (cx + rx) * s - 1, (cy + ry) * s - 1], fill=255))

    def rect(self, x0, y0, x1, y1):
        m = np.zeros((self.h, self.w), bool); m[max(0, y0):y1 + 1, max(0, x0):x1 + 1] = True; return m

    def line(self, pts, width=1.0):
        def f(d, s):
            d.line([(x * s, y * s) for x, y in pts], fill=255, width=max(1, int(width * s)), joint='curve')
            r = width * s / 2
            for x, y in pts: d.ellipse([x * s - r, y * s - r, x * s + r, y * s + r], fill=255)
        return self.mask(f)

    # ------------------------------------------------------------------ painting
    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.a[int(y), int(x)] = c if len(c) == 4 else (*c, 255)

    def paint(self, mask, c):
        ys, xs = np.nonzero(mask)
        for y, x in zip(ys, xs): self.a[y, x] = c if len(c) == 4 else (*c, 255)

    def blend(self, x, y, c):
        """Source-over for a single pixel (used for translucent glows and webs)."""
        if not (0 <= x < self.w and 0 <= y < self.h): return
        sa = (c[3] if len(c) == 4 else 255) / 255.0
        d = self.a[int(y), int(x)].astype(float); da = d[3] / 255.0
        oa = sa + da * (1 - sa)
        if oa <= 0: return
        for i in range(3): d[i] = (c[i] * sa + d[i] * da * (1 - sa)) / oa
        d[3] = oa * 255
        self.a[int(y), int(x)] = np.clip(np.round(d), 0, 255)

    def paste(self, other, x0, y0):
        for y in range(other.h):
            for x in range(other.w):
                p = other.a[y, x]
                if p[3]: self.blend(x0 + x, y0 + y, tuple(int(v) for v in p))

    def alpha(self):
        return self.a[:, :, 3] > 0

    def outline(self, color, base=None, diag=False, over=False):
        """1 px outline around everything painted so far (or around `base`, a mask), painted only on empty pixels."""
        m = self.alpha() if base is None else base
        grown = np.zeros_like(m)
        grown[1:, :] |= m[:-1, :]; grown[:-1, :] |= m[1:, :]; grown[:, 1:] |= m[:, :-1]; grown[:, :-1] |= m[:, 1:]
        if diag:
            grown[1:, 1:] |= m[:-1, :-1]; grown[:-1, :-1] |= m[1:, 1:]; grown[1:, :-1] |= m[:-1, 1:]; grown[:-1, 1:] |= m[1:, :-1]
        self.paint(grown & ~(m if over else self.alpha()), color)

    def image(self):
        return Image.fromarray(self.a, 'RGBA')


def shade_pixels(mask, ramp, edge=None, light_edge=True, dark_edge=True):
    """Returns {(x, y): colour} for a bevelled fill. `ramp` = (shadow, dark, base, light, highlight).
    Pixels on an upper/left boundary take the highlight, lower/right boundary the shadow, the interior steps dark -> base -> light
    with a 2x2 ordered dither on the step boundaries, light coming from the top left."""
    edge = mask if edge is None else edge
    ys, xs = np.nonzero(mask)
    if len(xs) == 0: return {}
    ey, ex = np.nonzero(edge)
    b = (ex.min(), ey.min(), ex.max() + 1, ey.max() + 1)
    pad = np.pad(edge, 1)
    out = {}
    for y, x in zip(ys, xs):
        up, left = not pad[y, x + 1], not pad[y + 1, x]
        down, right = not pad[y + 2, x + 1], not pad[y + 1, x + 2]
        w, h = b[2] - b[0], b[3] - b[1]
        t = 0.55 * (y - b[1]) / max(1, h - 1) + 0.45 * (x - b[0]) / max(1, w - 1)       # 0 = lit corner, 1 = shaded corner
        t += (BAYER[y % 2, x % 2] - 0.4) * 0.13
        if light_edge and (up or left) and not (down or right): c = ramp[4] if (up and left) or t < 0.55 else ramp[3]
        elif dark_edge and (down or right) and not (up or left): c = ramp[0] if t > 0.45 else ramp[1]
        else: c = ramp[3] if t < 0.22 else (ramp[2] if t < 0.58 else ramp[1])
        out[(x, y)] = c
    return out


def bevel(canvas, mask, ramp, **kw):
    for (x, y), c in shade_pixels(mask, ramp, **kw).items(): canvas.put(x, y, c)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def rot(pts, deg, cx=0.0, cy=0.0):
    r = math.radians(deg); c, s = math.cos(r), math.sin(r)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]


def walk(seed, x, y, steps, bias=(0.0, 1.0), wobble=0.9):
    """A deterministic jittery path for cracks: returns integer pixel positions."""
    import random
    rnd = random.Random(seed)
    pts = [(x, y)]
    ang = math.atan2(bias[1], bias[0])
    for _ in range(steps):
        ang += rnd.uniform(-wobble, wobble)
        x += math.cos(ang) * 1.0; y += math.sin(ang) * 1.0
        pts.append((round(x), round(y)))
    return pts
