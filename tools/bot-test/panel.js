// Renders what a bot received into a PNG. This is a picture of the DATA the bot got (boss bar, action
// bar, chat, inventory, nearby entities), laid out like a HUD. It is NOT a render from a Minecraft client.
const { chromium } = require('playwright');

const COLORS = { purple: '#b04cff', pink: '#ff6fb5', red: '#ff4d4d', green: '#4dd66a', blue: '#4d8fff', yellow: '#ffd23f', white: '#eeeeee' };
const esc = (t) => String(t).replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));

function html(snap, title) {
  const bars = (snap.bossBars || []).map((b) => `
    <div class="bar"><div class="bt">${esc(b.title)}</div>
    <div class="track"><div class="fill" style="width:${Math.round(b.progress * 100)}%;background:${COLORS[b.color] || '#b04cff'}"></div></div></div>`).join('');
  const slots = (snap.inventory || []).map((i) => {
    const label = i.itemName || i.display || i.name;
    const sugar = i.name === 'sugar' && i.itemName;
    return `<div class="slot ${sugar ? 'candy' : ''}"><div class="ic">${sugar ? '🍬' : '⚪'}</div><div class="n">${i.count}</div><div class="l">${esc(label)}${i.modelData !== undefined ? ` <i>(model ${i.modelData})</i>` : ''}</div></div>`;
  }).join('') || '<div class="empty">empty</div>';
  const chat = (snap.lastChat || []).map((c) => `<div>${esc(c)}</div>`).join('');
  const me = snap.position || { x: 0, z: 0 };
  const dots = (snap.nearby || []).slice(0, 60).map((e) => {
    const dx = (e.x - me.x) * 5, dz = (e.z - me.z) * 5;
    return `<div class="dot ${e.dist < 8 ? 'near' : ''}" style="left:${150 + dx}px;top:${150 + dz}px" title="id ${e.id}"></div>`;
  }).join('');
  return `<!doctype html><meta charset="utf-8"><style>
  body{margin:0;background:#15121c;color:#e8e6ef;font:14px/1.35 ui-monospace,Menlo,Consolas,monospace;width:960px}
  .wrap{padding:16px}
  h1{font-size:15px;margin:0 0 2px} .sub{color:#f0b429;font-size:12px;margin-bottom:12px}
  .row{display:flex;gap:14px} .col{flex:1}
  .box{background:#221d2e;border:1px solid #3a3350;border-radius:6px;padding:10px;margin-bottom:12px}
  .box h2{font-size:12px;color:#9c93b8;margin:0 0 8px;text-transform:uppercase;letter-spacing:.06em}
  .bt{text-align:center;margin-bottom:4px} .track{height:10px;background:#0c0a12;border:1px solid #000;border-radius:2px}
  .fill{height:100%} .ab{text-align:center;font-size:16px;color:#ffd23f;min-height:22px}
  .chat div{padding:1px 0;color:#d6d3e4} .slots{display:flex;flex-wrap:wrap;gap:8px}
  .slot{width:116px;background:#2b2539;border:1px solid #4a4266;border-radius:4px;padding:6px;text-align:center}
  .slot.candy{border-color:#ff9d1c;background:#3a2b16} .ic{font-size:22px} .n{font-weight:bold} .l{font-size:11px;color:#bdb6d6} i{color:#888}
  .radar{position:relative;width:300px;height:300px;background:#0c0a12;border:1px solid #3a3350;border-radius:4px;overflow:hidden}
  .me{position:absolute;left:146px;top:146px;width:8px;height:8px;background:#5ad1ff;border-radius:50%}
  .dot{position:absolute;width:5px;height:5px;background:#8a82a8;border-radius:50%} .dot.near{background:#ff6fb5}
  .empty{color:#777}
  </style><div class="wrap">
  <h1>${esc(title)} — ${esc(snap.bot)}</h1>
  <div class="sub">Reconstructed from the packets the bot received. Not a render from a Minecraft client.</div>
  <div class="box"><h2>Boss bar</h2>${bars || '<div class="empty">none</div>'}</div>
  <div class="box"><h2>Action bar</h2><div class="ab">${esc(snap.lastActionBar || '')}</div></div>
  <div class="row"><div class="col"><div class="box"><h2>Chat</h2><div class="chat">${chat || '<div class="empty">none</div>'}</div></div>
  <div class="box"><h2>Inventory</h2><div class="slots">${slots}</div></div></div>
  <div class="box"><h2>Entities near the bot (top-down)</h2><div class="radar"><div class="me"></div>${dots}</div></div></div></div>`;
}

async function renderPanel(snap, title, outPng) {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 960, height: 720 } });
  await page.setContent(html(snap, title));
  await page.screenshot({ path: outPng, fullPage: true });
  await browser.close();
}

module.exports = { renderPanel };
