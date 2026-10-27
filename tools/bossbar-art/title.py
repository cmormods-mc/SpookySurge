"""Python twin of src/main/java/com/spookysurge/hud/CandyHuntTitle.java. It exists so the preview pack and the demo commands can be made
without a server, and so a golden file can prove the Java and Python layouts are identical (see make_golden.py and CandyHuntTitleGoldenTest)."""
import json, os, unicodedata
HERE = os.path.dirname(os.path.abspath(__file__))
META = json.load(open(os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'spookysurge', 'hud', 'candyhunt_metrics.json')))
M = META


def sanitize(name):
    plain = ''.join(c for c in unicodedata.normalize('NFD', name or '') if not unicodedata.combining(c)).upper()
    out, pending = [], False
    for c in plain:
        if c.isspace() or c == '_': pending = bool(out)
        elif c in M['letters'] and c != ' ':
            if pending: out.append(' ')
            pending = False
            out.append(c)
    return ''.join(out)


def adv(run): return sum(M['letters'][c]['adv'] for c in run)
def visual(run): return 0 if not run else adv(run) - 1


def fit(run):
    maxw = M['plate']['x1'] - M['plate']['x0']
    if visual(run) <= maxw: return run
    cut = run
    while cut and visual(cut.rstrip() + '...') > maxw: cut = cut[:-1]
    return cut.rstrip() + '...'


def spaces(n):
    base = 0xE200 if n >= 0 else 0xE300
    a, out = abs(n), ''
    for bit in range(7, -1, -1):
        v = 1 << bit
        while a >= v: out += chr(base + v); a -= v
    return out


def timer_text(text):
    return ''.join(c for c in (text or '') if c in M['timer']['glyphs'])[:8]


def title(name, tier, timer=None):
    out, cur = '', 0
    f = M['frame']
    out += chr(f['cpLeft']); cur += f['advLeft']
    out += spaces(f['split'] - cur); cur = f['split']
    out += chr(f['cpRight']); cur += f['advRight']
    for i in range(max(0, min(4, tier))):
        left = M['tier_lit']['lefts'][i]
        out += spaces(left - cur) + chr(M['tier_lit']['cp']); cur = left + M['tier_lit']['adv']
    run = fit(sanitize(name))
    if run:
        start = M['canvasCenter'] - visual(run) // 2
        out += spaces(start - cur); cur = start
        for c in run:
            out += chr(M['letters'][c]['cp']); cur += M['letters'][c]['adv']
    t = timer_text(timer)
    if t:
        g = M['timer']['glyphs']
        start = M['timer']['right'] - (sum(g[c]['adv'] for c in t) - 1)
        out += spaces(start - cur); cur = start
        for c in t:
            out += chr(g[c]['cp']); cur += g[c]['adv']
    out += spaces(M['titleWidth'] - cur)
    return out


def component_json(name, tier, timer=None):
    """The same title as a vanilla text component (what /bossbar set <id> name expects)."""
    return json.dumps({"text": title(name, tier, timer), "font": "spookysurge:candyhunt", "color": "white"})


CASES = [('', 0, None), ('SPOOKY SURGE CANDY HUNT', 0, '27:00'), ('SPOOKY SURGE CANDY HUNT', 4, '0:05'), ('Spooky Surge 1234/5000', 2, '1:59:59'), ('a', 3, None),
         ('CANDY HUNT BEGINS IN 45S', 1, '0:45'), ('x' * 80, 4, '12:34'), ('Pok\u00e9mon  Trainer', 2, 'ab1:2c'), ('W' * 30, 0, '99:99:99:99'),
         ('\u4e2d\u6587 name', 1, ''), ("It's the -END- & more: really?!", 4, '8:08')]

if __name__ == '__main__':
    out = os.path.join(HERE, '..', '..', 'src', 'test', 'resources', 'candyhunt_golden.json')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    json.dump([{'name': n, 'tier': t, 'timer': tm, 'title': title(n, t, tm)} for n, t, tm in CASES], open(out, 'w'), indent=1)
    print('wrote', len(CASES), 'golden titles')
