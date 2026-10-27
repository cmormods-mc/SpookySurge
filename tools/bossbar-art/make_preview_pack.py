"""Builds docs/bossbar/revenant-preview-pack.zip (pack_format 34, 1.21.1) and docs/bossbar/demo_commands.txt.

The pack holds only the Revenant art. Dropped into resourcepacks/ it replaces the YELLOW and BLUE boss-bar sprites and adds the font; the demo
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
    'assets/minecraft/textures/gui/sprites/boss_bar/yellow_background.png',
    'assets/minecraft/textures/gui/sprites/boss_bar/yellow_progress.png',
    'assets/minecraft/textures/gui/sprites/boss_bar/yellow_progress.png.mcmeta',
    'assets/minecraft/textures/gui/sprites/boss_bar/blue_background.png',
    'assets/minecraft/textures/gui/sprites/boss_bar/blue_progress.png',
    'assets/spookysurge/font/revenant.json',
    'assets/spookysurge/textures/font/revenant/frame_l.png',
    'assets/spookysurge/textures/font/revenant/frame_r.png',
    'assets/spookysurge/textures/font/revenant/tier_lit.png',
    'assets/spookysurge/textures/font/revenant/letters.png',
]
STATES = [(100, 4), (75, 3), (50, 2), (25, 1), (10, 0), (0, 0)]


def main():
    os.makedirs(OUT, exist_ok=True)
    zpath = os.path.join(OUT, 'revenant-preview-pack.zip')
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('pack.mcmeta', json.dumps({"pack": {"pack_format": 34, "description": "Candy Corn Revenant boss bar (preview)"}}, indent=1))
        for f in FILES: z.write(os.path.join(RES, f), f)
    lines = ['# Candy Corn Revenant: vanilla /bossbar demo. Needs revenant-preview-pack.zip loaded on the client.',
             '# Each state is three bars in this order: BLUE spacer, YELLOW art bar, BLUE tail spacer. Run on a server where you are an operator.',
             '# Replace @s with your name if you run it from the console. Remove everything afterwards with the cleanup block at the end.', '']
    name = 'THE CANDY CORN REVENANT'
    for pct, tier in STATES:
        i = f'demo{pct}'
        lines += [f'# --- {pct}% health, tier {tier} lights',
                  f'bossbar add minecraft:{i}a {{"text":""}}', f'bossbar set minecraft:{i}a color blue', f'bossbar set minecraft:{i}a players @s',
                  f'bossbar add minecraft:{i}b {T.component_json(name, tier)}', f'bossbar set minecraft:{i}b color yellow',
                  f'bossbar set minecraft:{i}b max 100', f'bossbar set minecraft:{i}b value {pct}', f'bossbar set minecraft:{i}b players @s',
                  f'bossbar add minecraft:{i}c {{"text":""}}', f'bossbar set minecraft:{i}c color blue', f'bossbar set minecraft:{i}c players @s', '']
        lines += ['# to show the next state, first run (uncomment):'] + [f'# bossbar remove minecraft:{i}{k}' for k in 'abc'] + ['']
    lines.append('# --- cleanup')
    for pct, _ in STATES: lines += [f'bossbar remove minecraft:demo{pct}{k}' for k in 'abc']
    open(os.path.join(OUT, 'demo_commands.txt'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
    print('wrote', zpath, os.path.getsize(zpath), 'bytes')


if __name__ == '__main__':
    main()
