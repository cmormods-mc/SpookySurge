"""Assembles the REAL-client captures (capture_states.py output) into one sheet. Nothing here is simulated.

    python3 real_sheet.py <day-dir> <night-dir> <out.png>
"""
import os, sys
from PIL import Image, ImageDraw, ImageFont

DAY, NIGHT, OUT = sys.argv[1:4]
STATES = [(100, 'Tier 4 lights'), (75, 'Tier 3'), (50, 'Tier 2'), (25, 'Tier 1'), (10, 'no tier'), (0, 'empty')]

def font(sz, bold=False):
    p = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    return ImageFont.truetype(p, sz) if os.path.exists(p) else ImageFont.load_default()

first = Image.open(f'{DAY}/state_100.png')
w, h = first.size
Z = 4
pad, lab, top = 24, 150, 100
sheet = Image.new('RGB', (lab + 2 * w + 3 * pad, top + len(STATES) * (h + 12) + 30 + (h * 1) + 220), (24, 22, 32))
d = ImageDraw.Draw(sheet)
d.text((pad, 18), 'The Spooky Surge Candy Hunt: health states in a real Minecraft 1.21.1 client', font=font(24, True), fill=(250, 240, 220))
d.text((pad, 52), 'Screen captures at GUI scale 2 (1280x720), 1:1. Left: daylight sky. Right: midnight sky. Tier lights follow the surge tier.', font=font(13), fill=(170, 164, 190))
y = top
for pct, note in STATES:
    d.text((pad, y + h // 2 - 22), f'{pct}%', font=font(22, True), fill=(255, 190, 90))
    d.text((pad, y + h // 2 + 6), note, font=font(12), fill=(170, 164, 190))
    for i, src in enumerate((DAY, NIGHT)):
        sheet.paste(Image.open(f'{src}/state_{pct:03d}.png').convert('RGB'), (lab + pad * (i + 1) + i * w, y))
    y += h + 12
d.text((pad, y + 6), 'Pixel inspection (nearest-neighbour x4): left medallion and the fill edge at 50%, right medallion at 100%', font=font(13, True), fill=(220, 210, 240))
y += 30
left = Image.open(f'{DAY}/state_050.png').crop((0, 0, 230, 120)).resize((230 * Z, 120 * Z), Image.NEAREST)
right = Image.open(f'{DAY}/state_100.png').crop((w - 150, 0, w, 120)).resize((150 * Z, 120 * Z), Image.NEAREST)
sheet = sheet.crop((0, 0, sheet.width, y + 120 * Z + 20)) if sheet.height < y + 120 * Z + 20 else sheet
canvas = Image.new('RGB', (max(sheet.width, left.width + right.width + 3 * pad), y + 120 * Z + 20), (24, 22, 32))
canvas.paste(sheet.crop((0, 0, sheet.width, min(sheet.height, y))), (0, 0))
canvas.paste(left.convert('RGB'), (pad, y)); canvas.paste(right.convert('RGB'), (pad * 2 + left.width, y))
canvas.save(OUT)
print('wrote', OUT, canvas.size)
