"""Builds docs/bossbar/candyhunt-preview-pack.zip (pack_format 34, 1.21.1) and docs/bossbar/demo_commands.txt.

The pack holds only the CandyHunt art. Dropped into resourcepacks/ it replaces the YELLOW and BLUE boss-bar sprites and adds the font; the demo
commands then show the bar on a vanilla server with /bossbar (no mod needed), one block per health state.
"""
import json, os, sys, zipfile
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import title as T

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
OUT = os.path.join(ROOT, 'docs', 'bossbar')
FILES = [
    'assets/spookysurge/font/candyhunt.json',
    'assets/spookysurge/textures/font/candyhunt/frame_l.png',
    'assets/spookysurge/textures/font/candyhunt/frame_r.png',
    'assets/spookysurge/textures/font/candyhunt/tier_lit.png',
    'assets/spookysurge/textures/font/candyhunt/letters.png',
    'assets/spookysurge/textures/font/candyhunt/timer.png',
]
STATES = [(100, 4), (75, 3), (50, 2), (25, 1), (10, 0), (0, 0)]


def main():
    os.makedirs(OUT, exist_ok=True)
    zpath = os.path.join(OUT, 'candyhunt-preview-pack.zip')
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('pack.mcmeta', json.dumps({"pack": {"pack_format": 34, "description": "Spooky Surge Candy Hunt boss bar (preview)"}}, indent=1))
        for f in FILES: z.write(os.path.join(RES, f), f)
        V = 'assets/minecraft/textures/gui/sprites/boss_bar/'            # the preview pack has no Boss Bar Skins: the art goes straight onto YELLOW and BLUE
        S = os.path.join(RES, 'bossbarskins', 'candyhunt')
        for src, dst in (('background.png', 'yellow_background.png'), ('progress.png', 'yellow_progress.png'), ('progress.png.mcmeta', 'yellow_progress.png.mcmeta'),
                         ('transparent.png', 'blue_background.png'), ('transparent.png', 'blue_progress.png')):
            z.write(os.path.join(S, src), V + dst)
    lines = ['# Spooky Surge Candy Hunt: vanilla /bossbar demo. Needs candyhunt-preview-pack.zip loaded on the client.',
             '# Each state is three bars in this order: BLUE spacer, YELLOW art bar, BLUE tail spacer. Run on a server where you are an operator.',
             '# Replace @s with your name if you run it from the console. Remove everything afterwards with the cleanup block at the end.', '']
    name = 'SPOOKY SURGE CANDY HUNT'
    for pct, tier in STATES:
        i = f'demo{pct}'
        lines += [f'# --- {pct}% health, tier {tier} lights',
                  f'bossbar add minecraft:{i}a {{"text":""}}', f'bossbar set minecraft:{i}a color blue', f'bossbar set minecraft:{i}a players @s',
                  f'bossbar add minecraft:{i}b {T.component_json(name, tier, '12:34')}', f'bossbar set minecraft:{i}b color yellow',
                  f'bossbar set minecraft:{i}b max 100', f'bossbar set minecraft:{i}b value {pct}', f'bossbar set minecraft:{i}b players @s',
                  f'bossbar add minecraft:{i}c {{"text":""}}', f'bossbar set minecraft:{i}c color blue', f'bossbar set minecraft:{i}c players @s', '']
        lines += ['# to show the next state, first run (uncomment):'] + [f'# bossbar remove minecraft:{i}{k}' for k in 'abc'] + ['']
    lines.append('# --- cleanup')
    for pct, _ in STATES: lines += [f'bossbar remove minecraft:demo{pct}{k}' for k in 'abc']
    open(os.path.join(OUT, 'demo_commands.txt'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
    print('wrote', zpath, os.path.getsize(zpath), 'bytes')


if __name__ == '__main__':
    main()
