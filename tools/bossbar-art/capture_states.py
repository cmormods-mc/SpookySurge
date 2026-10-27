"""Drives a running test server + real client (see tools/real-client) and photographs the bar at each health state.

    DISPLAY=:99 python3 capture_states.py <console-file> <out-dir> [player=ShotCam] [name...]

Needs the player to be an operator (the preview command is level 2) and the client to be in game. Each state is shown with
`/surge bar preview <percent> <tier> <name>`; the crop is the top-centre of the screen. Nothing here is simulated: the PNGs are the screen.
"""
import os, subprocess, sys, time

CONSOLE, OUT = sys.argv[1], sys.argv[2]
PLAYER = sys.argv[3] if len(sys.argv) > 3 else 'ShotCam'
NAME = ' '.join(sys.argv[4:]) or 'SPOOKY SURGE CANDY HUNT'
STATES = [(100, 4), (75, 3), (50, 2), (25, 1), (10, 0), (0, 0)]
CROP = os.environ.get('CROP', '620x130+330+0')
os.makedirs(OUT, exist_ok=True)

def cmd(line):
    with open(CONSOLE, 'a') as f: f.write(line + '\n')

for pct, tier in STATES:
    cmd(f'execute as {PLAYER} run surge bar preview {pct} {tier} {NAME}')
    time.sleep(2.5)
    path = f'{OUT}/state_{pct:03d}.png'
    subprocess.run(['import', '-window', 'root', '-crop', CROP, '+repage', path], check=True)
    print('captured', path)
cmd(f'execute as {PLAYER} run surge bar off')
