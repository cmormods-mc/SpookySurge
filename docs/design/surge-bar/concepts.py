"""Four surge-bar concepts, drawn with Pillow into a resource pack, plus the title layout the server would send.

Bossbar sprites (182x5) can be animated and replaced per colour. Art bigger than 5 px comes from a custom font:
glyphs in the bar's title, placed at exact pixel offsets with 'space' glyphs, riding the bar's fill edge.

  A  Candle Vigil   YELLOW   warm wax-and-flame, a flickering flame rides the fill edge
  B  Spirit Tide    BLUE     flowing ectoplasm, a bobbing ghost rides the fill edge
  C  Candy Corn     GREEN    sliding candy-corn stripes, a sparkling candy corn rides the fill edge
  D  Harvest Moon   WHITE    twinkling night sky, a moon whose phase grows with the tier
"""
import json, math, os, random
from PIL import Image

W, H = 182, 5
NS = 'spookysurge'
OUT = os.environ.get('PACK', 'pack')
SPR = f'{OUT}/assets/minecraft/textures/gui/sprites/boss_bar'
FONT_DIR = f'{OUT}/assets/{NS}/textures/font'

def new(w, h): return Image.new('RGBA', (w, h), (0, 0, 0, 0))
def put(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height: im.putpixel((int(x), int(y)), c)
def clamp(v): return max(0, min(255, int(v)))
def lift(c, d, a=None):
    alpha = (c[3] if len(c) > 3 else 255) if a is None else a
    return (clamp(c[0]+d), clamp(c[1]+d), clamp(c[2]+d), alpha)
def mix(a, b, t): return tuple(clamp(a[i] + (b[i]-a[i])*t) for i in range(len(a)))
def lock(im):
    """Make the glyph's width deterministic: bitmap fonts trim empty right-hand columns, so mark the last one."""
    if im.getpixel((im.width-1, im.height-1))[3] == 0: im.putpixel((im.width-1, im.height-1), (255, 255, 255, 1))
    return im

# ----------------------------------------------------------------------------------------------- bars
def strip(frames, draw):
    s = new(W, H*frames)
    for f in range(frames):
        fr = draw(f)
        s.paste(fr, (0, f*H))
    return s

def bar_a():
    bg = new(W, H)
    rows = [(62, 42, 30, 255), (38, 25, 19, 255), (31, 21, 16, 255), (27, 18, 14, 255), (16, 10, 8, 255)]
    for y in range(H):
        for x in range(W):
            c = rows[y]
            if x % 9 == 0 and 0 < y < 4: c = lift(c, -6)
            put(bg, x, y, c)
    for x in (45, 91, 136, 181):                      # tier posts: unlit candle sticks standing in the rail
        for y in range(1, 4): put(bg, x, y, (96, 78, 60, 255))
        put(bg, x, 0, (140, 112, 78, 255))
    def prog(f):
        fr = new(W, H)
        base = [(255, 228, 140), (255, 180, 58), (246, 130, 26), (208, 88, 14), (132, 48, 9)]
        for x in range(W):
            band = max(0.0, math.sin((x/17.0) - f*0.785))        # heat shimmer drifting right
            for y in range(H):
                c = base[y]
                lit = band * (46 if y < 3 else 14)
                put(fr, x, y, (clamp(c[0]+lit), clamp(c[1]+lit*0.8), clamp(c[2]+lit*0.4), 255))
        rnd = random.Random(7)
        for k in range(9):                                        # embers rising through the wax
            x = (k*23 + f*5) % W; y = (3 - ((f + k) % 3)) % 4
            put(fr, x, max(0, y), (255, 244, 196, 255))
        return fr
    return bg, strip(8, prog), 3

def bar_b():
    bg = new(W, H)
    rows = [(16, 26, 54, 236), (24, 38, 72, 226), (19, 31, 60, 226), (15, 24, 50, 226), (9, 14, 32, 236)]
    rnd = random.Random(3)
    for y in range(H):
        for x in range(W): put(bg, x, y, rows[y])
    for _ in range(26): put(bg, rnd.randrange(W), rnd.randrange(1, 4), (60, 96, 150, 230))
    def prog(f):
        fr = new(W, H)
        base = [(196, 255, 250), (118, 228, 232), (84, 176, 238), (100, 116, 224), (72, 62, 176)]
        for x in range(W):
            for y in range(H):
                wave = math.sin(2*math.pi*(x/26.0 - f/8.0) + y*0.9)
                c = base[y]
                k = 38*wave
                put(fr, x, y, (clamp(c[0]+k), clamp(c[1]+k), clamp(c[2]+k*0.6), 244))
        return fr
    return bg, strip(8, prog), 3

def bar_c():
    bg = new(W, H)
    rows = [(34, 14, 48, 255), (58, 28, 80, 255), (46, 22, 66, 255), (40, 18, 58, 255), (24, 9, 34, 255)]
    for y in range(H):
        for x in range(W): put(bg, x, y, rows[y])
    for x in range(W):                                          # stitching along the top and bottom edge
        if (x // 3) % 2 == 0: put(bg, x, 0, (170, 130, 186, 255)); put(bg, x, 4, (110, 76, 130, 255))
    def prog(f):
        fr = new(W, H)
        cols = [(255, 248, 228, 255), (255, 152, 30, 255), (255, 214, 56, 255)]
        for x in range(W):
            for y in range(H):
                i = ((x + y*2 - f*3) // 6) % 3
                c = cols[i]
                if y == 0: c = lift(c, 14)
                if y == 4: c = mix(c, (150, 70, 8, 255), 0.45)
                put(fr, x, y, c)
        return fr
    return bg, strip(6, prog), 3

def bar_d():
    def bg_frame(f):
        fr = new(W, H)
        rows = [(8, 11, 34, 255), (15, 20, 52, 255), (12, 17, 44, 255), (10, 13, 36, 255), (6, 8, 22, 255)]
        for y in range(H):
            for x in range(W): put(fr, x, y, rows[y])
        rnd = random.Random(11)
        for k in range(22):
            x, y = rnd.randrange(W), rnd.randrange(0, 5)
            lit = (k + f) % 4 == 0
            put(fr, x, y, (255, 252, 226, 255) if lit else (120, 130, 176, 255))
        return fr
    def prog(f):
        fr = new(W, H)
        base = [(252, 254, 255), (216, 228, 255), (178, 198, 242), (140, 162, 216), (98, 118, 172)]
        for x in range(W):
            cloud = 0.5 + 0.5*math.sin(x/21.0 - f*0.5236)           # slow cloud shadows drifting past
            for y in range(H):
                c = base[y]
                put(fr, x, y, (clamp(c[0]-26*cloud), clamp(c[1]-22*cloud), clamp(c[2]-12*cloud), 255))
        return fr
    return strip(4, bg_frame), strip(12, prog), 4

# ----------------------------------------------------------------------------------------------- glyphs
def span(im, y, x0, x1, c):
    for x in range(int(math.ceil(x0)), int(math.floor(x1))+1): put(im, x, y, c)

def flame(f, palette):
    im = new(16, 16)
    widths = [0, 1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8, 7, 6, 4, 2]
    out, mid, core = palette
    for y, w in enumerate(widths):
        if w == 0: continue
        sway = (1 if f == 0 else -1) if y < 6 else (0 if y < 9 else 0)
        cx = 7.5 + sway*(1 if y < 4 else 0.5)
        span(im, y, cx - w/2 + 0.5, cx + w/2 - 0.5, out)
        if w > 2: span(im, y, cx - w/2 + 1.5, cx + w/2 - 1.5, mid)
        if y > 7 and w > 4: span(im, y, cx - w/2 + 2.5, cx + w/2 - 2.5, core)
    return lock(im)

def ghost(f):
    im = new(16, 16)
    body, shade, outline, dark = (234, 247, 255, 255), (168, 200, 238, 255), (130, 214, 255, 150), (24, 32, 86, 255)
    for y in range(1, 15):
        for x in range(1, 15):
            dx, dy = x - 7.5, y - 6.0
            dome = (dx*dx)/(6.2*6.2) + (dy*dy)/(5.4*5.4) <= 1.0 and y <= 6
            trunk = 6 <= y <= 12 and 1.5 <= x <= 13.5
            hem = y in (13, 14) and 1.5 <= x <= 13.5 and ((x + f + (y-13)*2) // 2) % 2 == 0
            if dome or trunk or hem:
                put(im, x, y, shade if x >= 12 else body)
    for y in range(16):                                  # soft outline
        for x in range(16):
            if im.getpixel((x, y))[3] == 0:
                for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 0 <= x+ox < 16 and 0 <= y+oy < 16 and im.getpixel((x+ox, y+oy))[3] == 255: put(im, x, y, outline); break
    for x, y in ((5, 5), (6, 5), (5, 6), (6, 6), (5, 7), (9, 5), (10, 5), (9, 6), (10, 6), (9, 7)): put(im, x, y, dark)
    for x, y in ((7, 9), (8, 9), (7, 10), (8, 10)): put(im, x, y, dark)
    if f == 1: put(im, 2, 8, body); put(im, 13, 8, body)  # little arms up/down between frames
    return lock(im)

def candy(f):
    im = new(16, 16)
    cols = [(255, 248, 228, 255), (255, 152, 30, 255), (255, 214, 56, 255)]
    edge = [(176, 160, 130, 255), (170, 84, 10, 255), (180, 130, 20, 255)]
    for y in range(2, 15):
        w = 2 + (y-2)
        sec = 0 if y < 7 else (1 if y < 11 else 2)
        x0 = 7.5 - w/2 + 0.5; x1 = 7.5 + w/2 - 0.5
        span(im, y, x0, x1, cols[sec])
        put(im, int(math.ceil(x0)), y, edge[sec]); put(im, int(math.floor(x1)), y, edge[sec])
    span(im, 14, 2, 13, edge[2])
    put(im, 6, 5, (255, 255, 255, 255)); put(im, 5, 9, (255, 196, 110, 255))
    if f == 1:
        for x, y in ((13, 2), (13, 1), (13, 3), (12, 2), (14, 2)): put(im, x, y, (255, 255, 255, 255))
    else:
        put(im, 13, 2, (255, 244, 200, 255))
    return lock(im)

def moon(phase):
    im = new(16, 16)
    lit, dark, crater = (240, 244, 255, 255), (30, 36, 72, 215), (206, 214, 238, 255)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            if dx*dx + dy*dy <= 7.3*7.3:
                on = True
                if phase == 0: on = (dx + 3.4)**2 + dy*dy > 6.4**2 and dx > -4           # thin crescent
                elif phase == 1: on = dx >= -0.5                                          # half
                elif phase == 2: on = (dx + 7.0)**2 + dy*dy > 8.6**2                       # gibbous
                put(im, x, y, lit if on else dark)
    if phase >= 1:
        for x, y in ((9, 5), (10, 6), (8, 10), (11, 9)): put(im, x, y, crater)
    return lock(im)

def pumpkin():
    im = new(16, 16)
    o, d, g, glow = (255, 138, 24, 255), (200, 84, 10, 255), (86, 148, 50, 255), (255, 226, 110, 255)
    for y in range(4, 15):
        for x in range(1, 15):
            dx, dy = (x - 7.5)/6.8, (y - 9.5)/5.4
            if dx*dx + dy*dy <= 1.0: put(im, x, y, d if x in (4, 7, 8, 11) else o)
    for x, y in ((7, 2), (8, 2), (7, 3), (8, 3), (9, 3)): put(im, x, y, g)
    for x, y in ((5, 7), (4, 8), (5, 8), (6, 8), (10, 7), (9, 8), (10, 8), (11, 8), (5, 11), (6, 12), (7, 11), (8, 12), (9, 11), (10, 12)): put(im, x, y, glow)
    return lock(im)

def wrapped_candy():
    im = new(16, 16)
    for y in range(5, 11):
        for x in range(4, 12):
            if ((x-7.5)/4.2)**2 + ((y-7.5)/3.4)**2 <= 1.0: put(im, x, y, (255, 152, 30, 255) if (x + y) % 5 else (255, 248, 228, 255))
    for x, y in ((1, 4), (2, 5), (2, 6), (1, 7), (2, 8), (2, 9), (1, 10), (14, 4), (13, 5), (13, 6), (14, 7), (13, 8), (13, 9), (14, 10), (3, 6), (3, 7), (3, 8), (12, 6), (12, 7), (12, 8)):
        put(im, x, y, (176, 96, 230, 255))
    return lock(im)

def star(bright):
    im = new(16, 16)
    c = (255, 244, 190, 255) if bright else (96, 104, 140, 255)
    for x, y in ((7, 1), (8, 1), (7, 2), (8, 2), (7, 3), (8, 3), (7, 4), (8, 4), (1, 7), (1, 8), (2, 7), (2, 8), (3, 7), (3, 8), (4, 7), (4, 8),
                 (11, 7), (11, 8), (12, 7), (12, 8), (13, 7), (13, 8), (14, 7), (14, 8), (7, 11), (8, 11), (7, 12), (8, 12), (7, 13), (8, 13), (7, 14), (8, 14),
                 (5, 5), (6, 6), (9, 6), (10, 5), (5, 10), (6, 9), (9, 9), (10, 10), (6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 5), (8, 5), (7, 6), (8, 6), (7, 9), (8, 9), (7, 10), (8, 10)):
        put(im, x, y, c)
    return lock(im)

def pip(kind, lit):
    im = new(8, 8)
    if kind == 'A':
        c = (255, 190, 70, 255) if lit else (110, 92, 76, 255)
        for y in range(3, 8):
            put(im, 3, y, (232, 220, 196, 255) if lit else (120, 110, 100, 255)); put(im, 4, y, (200, 186, 160, 255) if lit else (96, 88, 80, 255))
        if lit:
            for x, y in ((3, 1), (4, 1), (3, 2), (4, 2), (3, 0)): put(im, x, y, (255, 200, 80, 255))
            put(im, 4, 2, (255, 250, 210, 255))
    elif kind == 'B':
        c = (150, 236, 255, 255) if lit else (60, 80, 120, 255)
        for y in range(8):
            for x in range(8):
                d = (x-3.5)**2 + (y-3.5)**2
                if (lit and d <= 11) or (not lit and 6 <= d <= 12): put(im, x, y, c)
        if lit: put(im, 3, 3, (255, 255, 255, 255)); put(im, 4, 3, (255, 255, 255, 255))
    elif kind == 'C':
        cols = [(255, 248, 228, 255), (255, 152, 30, 255), (255, 214, 56, 255)] if lit else [(120, 110, 130, 255)]*3
        for y in range(1, 8):
            w = 1 + (y-1)//2*1
            sec = 0 if y < 3 else (1 if y < 5 else 2)
            for x in range(4 - w//2 - (0 if w % 2 else 0), 4 + (w+1)//2): put(im, x, y, cols[sec])
    else:
        c = (255, 244, 190, 255) if lit else (84, 92, 128, 255)
        for x, y in ((3, 0), (4, 0), (3, 1), (4, 1), (0, 3), (1, 3), (6, 3), (7, 3), (0, 4), (1, 4), (6, 4), (7, 4), (3, 6), (4, 6), (3, 7), (4, 7), (2, 2), (5, 2), (2, 5), (5, 5), (3, 3), (4, 3), (3, 4), (4, 4), (2, 3), (5, 3), (2, 4), (5, 4)):
            put(im, x, y, c)
    return lock(im)

DIGITS = {'0': '111101101101111', '1': '010110010010111', '2': '111001111100111', '3': '111001111001111', '4': '101101111001001',
          '5': '111100111001111', '6': '111100111101111', '7': '111001010010010', '8': '111101111101111', '9': '111101111001111', '/': '001001010100100'}
def digits_strip():
    keys = '0123456789/'
    im = new(3*len(keys), 5)
    for i, k in enumerate(keys):
        for j, bit in enumerate(DIGITS[k]):
            if bit == '1': put(im, i*3 + j % 3, j // 3, (255, 255, 255, 255))
    return im

# ----------------------------------------------------------------------------------------------- the font and the layout
CODES = {}
def code(name, base):
    CODES[name] = chr(base); return CODES[name]

def build():
    os.makedirs(SPR, exist_ok=True); os.makedirs(FONT_DIR, exist_ok=True)
    for old in os.listdir(SPR): os.remove(os.path.join(SPR, old))
    for old in os.listdir(FONT_DIR): os.remove(os.path.join(FONT_DIR, old))
    open(f'{OUT}/pack.mcmeta', 'w').write('{"pack":{"pack_format":34,"description":"surge bar concepts"}}')
    bars = {'A': ('yellow', bar_a), 'B': ('blue', bar_b), 'C': ('green', bar_c), 'D': ('white', bar_d)}
    for key, (color, fn) in bars.items():
        bg, prog, ft = fn()
        for name, img, frames in ((f'{color}_background', bg, None), (f'{color}_progress', prog, ft)):
            img.save(f'{SPR}/{name}.png')
            frames_n = img.height // H
            if frames_n > 1:
                open(f'{SPR}/{name}.png.mcmeta', 'w').write(json.dumps({"animation": {"frametime": ft, "width": W, "height": H}}))
    providers = []
    def glyph(name, img, ch, ascent, height):
        name = name.lower()          # resource locations may only contain a-z 0-9 / . _ -
        img.save(f'{FONT_DIR}/{name}.png')
        providers.append({"type": "bitmap", "file": f"{NS}:font/{name}.png", "ascent": ascent, "height": height, "chars": [ch]})
    n = 0xE000
    def nxt():
        nonlocal n; n += 1; return chr(n)
    PAL_A, PAL_B = [(240, 110, 20, 255), (255, 200, 60, 255), (255, 246, 204, 255)], [(40, 120, 220, 255), (96, 200, 255, 255), (228, 250, 255, 255)]
    for f in (0, 1):
        for key, img in (('A', flame(f, PAL_A)), ('B', ghost(f)), ('C', candy(f))):
            ch = nxt(); CODES[f'head{key}{f}'] = ch; glyph(f'head_{key}{f}', img, ch, 4, 16)
    for ph in range(4):
        ch = nxt(); CODES[f'headD{ph}'] = ch; glyph(f'head_D{ph}', moon(ph), ch, 4, 16)
    for key, img in (('A', pumpkin()), ('B', flame(0, PAL_B)), ('C', wrapped_candy()), ('D', star(True))):
        ch = nxt(); CODES[f'emblem{key}'] = ch; glyph(f'emblem_{key}', img, ch, 4, 16)
    for key in 'ABCD':
        for lit in (0, 1):
            ch = nxt(); CODES[f'pip{key}{lit}'] = ch; glyph(f'pip_{key}{lit}', pip(key, lit), ch, 7, 8)
    keys = '0123456789/'
    chars = ''.join(nxt() for _ in keys)
    for k, c in zip(keys, chars): CODES['d' + k] = c
    digits_strip().save(f'{FONT_DIR}/digits.png')
    providers.append({"type": "bitmap", "file": f"{NS}:font/digits.png", "ascent": 8, "height": 10, "chars": [chars]})
    adv = {}
    for k in range(8):
        v = 1 << k; adv[chr(0xE100 + v)] = v; adv[chr(0xE200 + v)] = -v
    providers.append({"type": "space", "advances": adv})
    os.makedirs(f'{OUT}/assets/{NS}/font', exist_ok=True)
    open(f'{OUT}/assets/{NS}/font/bar.json', 'w').write(json.dumps({"providers": providers}))
    json.dump(CODES, open(f'{OUT}/codes.json', 'w'))

def spaces(n):
    out = ''; base = 0xE100 if n >= 0 else 0xE200; a = abs(int(round(n)))
    for k in range(7, -1, -1):
        v = 1 << k
        while a >= v: out += chr(base + v); a -= v
    return out

ADV = {'head': 17, 'emblem': 17, 'pip': 9, 'digit': 7}
def layout(items):
    """items: (left_px_from_bar_left, glyph_char, advance, colour) -> a title component list that puts them there."""
    items = sorted(items, key=lambda i: i[0])
    first_left = items[0][0]
    total = items[-1][0] + items[-1][2] - first_left
    k0 = 2*(first_left - 91) + total                    # the title is centred on the bar, so offset by half its own width
    comps, cursor = [], None
    for left, ch, adv, colour in items:
        gap = k0 if cursor is None else left - cursor
        comps.append({"text": spaces(gap) + ch, "font": f"{NS}:bar", "color": colour})
        cursor = left + adv
    return comps

def title(concept, value, maximum, tier, frame=0, accent='#ffffff'):
    """The whole title for one concept: emblem, tier pips, the head glyph on the fill edge, and the counter digits."""
    c = json.load(open(f'{OUT}/codes.json')) if not CODES else CODES
    p = max(8, min(174, W * value / maximum))
    items = [(-21, c[f'emblem{concept}'], 17, '#ffffff')]
    for i, x in enumerate((42, 87, 133, 178)):
        items.append((x, c[f'pip{concept}{1 if tier > i else 0}'], 9, '#ffffff'))
    head = c[f'headD{min(3, tier)}'] if concept == 'D' else c[f'head{concept}{frame % 2}']
    items.append((p - 8, head, 17, '#ffffff'))
    x = 190
    for chx in f'{value}/{maximum}':
        items.append((x, c['d' + chx], 7, accent)); x += 7
    return json.dumps(layout(items))

if __name__ == '__main__':
    build()
    print('built', len(CODES), 'glyphs into', OUT)
