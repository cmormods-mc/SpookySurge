// Measures server tick time with many bots, with and without a surge running.
//
//   CONSOLE=... LOG=... node loadtest.js <botCount> [settleSeconds]
//
// `/tick query` reports the last 100 ticks (5 seconds): average, P50, P95 and P99 milliseconds per tick.
// 50 ms is the 20 TPS budget. The bots run on the same machine as the server unless you run this
// elsewhere, so they take CPU from it; treat the numbers as a worst case and watch the load average.
const fs = require('fs');
const { execSync } = require('child_process');
const { createSurgeBot, sleep } = require('./surgebot');
const H = require('./harness');

const N = Number(process.argv[2] || 20);
const SETTLE = Number(process.argv[3] || 60);
const OUT = process.env.OUT || './loadtest.json';

async function sampleTick() {
  const from = H.logOffset();
  H.cmd('tick query');
  await H.waitLog(/Percentiles: P50/, { from, timeoutMs: 5000 });
  const t = H.logText().slice(from);
  const avg = t.match(/Average time per tick: ([\d.]+)ms/);
  const p = t.match(/P50: ([\d.]+)ms P95: ([\d.]+)ms P99: ([\d.]+)ms/);
  return avg && p ? { avg: +avg[1], p50: +p[1], p95: +p[2], p99: +p[3] } : null;
}

function serverRssMb() {
  try {
    const pid = execSync('pgrep -f KnotServer | head -1').toString().trim();
    return Math.round(+execSync(`ps -o rss= -p ${pid}`).toString().trim() / 1024);
  } catch (e) { return null; }
}
const load1 = () => +fs.readFileSync('/proc/loadavg', 'utf8').split(' ')[0];

async function phase(name, samples, { withStatus = false } = {}) {
  const rows = [];
  for (let i = 0; i < samples; i++) {
    await sleep(5000);
    const t = await sampleTick();
    const row = { ...t, rssMb: serverRssMb(), load1: load1() };
    if (withStatus && i % 3 === 0) Object.assign(row, await H.status());
    rows.push(row);
  }
  const ok = rows.filter((r) => r.avg !== undefined);
  const mean = (k) => +(ok.reduce((a, r) => a + r[k], 0) / ok.length).toFixed(1);
  const max = (k) => +Math.max(...ok.map((r) => r[k])).toFixed(1);
  const alive = rows.map((r) => r.alive).filter((x) => x != null);
  const summary = {
    phase: name, players: N, samples: ok.length, avgMs: mean('avg'), p95Ms: mean('p95'), p99MaxMs: max('p99'), worstAvgMs: max('avg'),
    rssMb: Math.max(...rows.map((r) => r.rssMb || 0)), load1: max('load1'), alivePeak: alive.length ? Math.max(...alive) : undefined,
  };
  console.log(JSON.stringify(summary));
  return { summary, rows };
}

(async () => {
  const result = [];
  H.cmd('surge stop');
  await sleep(3000);
  result.push(await phase('idle (no bots, no surge)', 6));

  const bots = [];
  for (let i = 0; i < N; i++) {
    bots.push(createSurgeBot({ name: `Load${String(i).padStart(2, '0')}` }));
    await sleep(1500);
  }
  await sleep(5000);
  H.cmd('spreadplayers 0 0 60 600 false @a');
  await sleep(SETTLE * 1000);
  result.push(await phase(`${N} bots, no surge`, 6));

  H.cmd('surge start 0');
  await sleep(15000);
  result.push(await phase(`${N} bots, surge tier 0`, 12, { withStatus: true }));

  H.cmd('surge tier 1');
  await sleep(10000);
  result.push(await phase(`${N} bots, surge tier 1 (boosted spawns)`, 12, { withStatus: true }));

  const online = (H.logText().match(/\[Server thread\/INFO\]: Load\d\d\[.*logged in/g) || []).length;
  H.cmd('surge stop');
  for (const b of bots) { try { b.bot.quit(); } catch (e) { /* already gone */ } }
  await sleep(2000);
  fs.writeFileSync(OUT, JSON.stringify({ players: N, loggedIn: online, result }, null, 2));
  process.exit(0);
})();
