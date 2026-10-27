// Runs the surge scenarios against a throwaway server and prints PASS/FAIL for each, with the evidence.
//
//   CONSOLE=... LOG=... CONFIG=... OUT=./out node scenarios.js [names...]
//
// CONFIG is the server's config/spookysurge/server.json, edited between scenarios and reloaded.
const fs = require('fs');
const path = require('path');
const { createSurgeBot, sleep } = require('./surgebot');
const H = require('./harness');
const { renderPanel } = require('./panel');

const CONFIG = process.env.CONFIG, OUT = process.env.OUT || './out';
if (!CONFIG) throw new Error('set CONFIG (the server config/spookysurge/server.json)');
fs.mkdirSync(OUT, { recursive: true });

const BASE_CANDY = { common: 1, uncommon: 3, rare: 6 };
const DEFAULTS = JSON.parse(fs.readFileSync(CONFIG, 'utf8'));

async function setConfig(patch) {
  fs.writeFileSync(CONFIG, JSON.stringify({ ...DEFAULTS, ...patch }, null, 2));
  H.cmd('surge reload');
  await sleep(800);
}

async function freshSurge() {
  H.cmd('surge stop');
  await sleep(1500);
  const from = H.logOffset();
  H.cmd('surge start 0');
  await H.waitLog(/The Spooky Surge has begun/, { from });
  await sleep(500);
  return H.surgeId();
}

async function join(name, tag) {
  const s = createSurgeBot({ name, outFile: path.join(OUT, `${tag}.${name}.events.jsonl`) });
  await new Promise((r) => s.bot.once('spawn', r));
  await sleep(2500);
  H.cmd(`op ${name}`);
  H.cmd(`give ${name} cobblemon:master_ball 8`);
  await sleep(1500);
  return s;
}

async function leave(s) {
  s.bot.quit();
  await sleep(700);
}

const lastAction = (s) => (s.events.filter((e) => e.kind === 'actionBar').slice(-1)[0] || {}).value;
const candyIn = (s) => s.inventory().filter((i) => i.name === 'sugar').reduce((n, i) => n + i.count, 0);
const barOf = (s) => (s.snapshot().bossBars[0] || {}).title || '';

async function shot(s, tag, title) {
  const snap = s.snapshot();
  fs.writeFileSync(path.join(OUT, `${tag}.${s.bot.username}.snapshot.json`), JSON.stringify(snap, null, 2));
  await renderPanel(snap, title, path.join(OUT, `${tag}.${s.bot.username}.png`));
}

const scenarios = {
  async catch_pays() {
    const id = await freshSurge();
    const s = await join('BotCatch', 'catch_pays');
    const before = candyIn(s);
    const c = await H.throwAtSurgePokemon(s, 'BotCatch', id);
    await sleep(1500);
    await shot(s, 'catch_pays', 'A catch pays Candy Corn');
    const ok = !!c && c.paid === BASE_CANDY[c.rarity] && candyIn(s) === before + c.paid
      && lastAction(s) === `+${c.paid} Candy Corn` && /\b1 \/ \d+/.test(barOf(s));
    await leave(s);
    return { ok, credit: c, candyBefore: before, candyAfter: candyIn(s), actionBar: lastAction(s), bar: barOf(s) };
  },

  async tier3_doubles() {
    const id = await freshSurge();
    H.cmd('surge tier 3');
    await sleep(800);
    const s = await join('BotTier3', 'tier3_doubles');
    const c = await H.throwAtSurgePokemon(s, 'BotTier3', id);
    await sleep(1500);
    await shot(s, 'tier3_doubles', 'Tier 3: Candy Corn is doubled');
    const ok = !!c && c.paid === BASE_CANDY[c.rarity] * 2;
    await leave(s);
    return { ok, credit: c, expected: c ? BASE_CANDY[c.rarity] * 2 : null, bar: barOf(s) };
  },

  async reward_cap() {
    await setConfig({ perPlayerRewardCap: 2 });
    const id = await freshSurge();
    const s = await join('BotCap', 'reward_cap');
    const got = [];
    for (let i = 0; i < 3; i++) got.push(await H.throwAtSurgePokemon(s, 'BotCap', id));
    await sleep(1500);
    await shot(s, 'reward_cap', 'Third catch is over the cap: counts, pays nothing');
    const ok = got.every(Boolean) && got[0].paid > 0 && got[1].paid > 0 && got[2].paid === 0
      && got[2].counter === 3 && lastAction(s) === 'Surge counter +1';
    await leave(s);
    await setConfig({});
    return { ok, credits: got, actionBar: lastAction(s) };
  },

  async end_bonus_and_offline_claim() {
    await setConfig({ participationMin: 2, participationBonus: 5 });
    const id = await freshSurge();
    let s = await join('BotClaim', 'claim');
    for (let i = 0; i < 2; i++) await H.throwAtSurgePokemon(s, 'BotClaim', id);
    const candyAtQuit = candyIn(s);
    await leave(s);
    const from = H.logOffset();
    H.cmd('surge stop');
    await H.waitLog(/Surge .* ended/, { from });
    await sleep(1000);
    s = createSurgeBot({ name: 'BotClaim', outFile: path.join(OUT, 'claim.BotClaim.rejoin.events.jsonl') });
    await new Promise((r) => s.bot.once('spawn', r));
    await sleep(3500);
    const msg = s.events.find((e) => e.kind === 'chat' && /You received \d+ Candy Corn/.test(e.value));
    await shot(s, 'claim', 'Rejoining after the surge: the owed bonus arrives');
    const ok = !!msg && candyIn(s) === candyAtQuit + 5;
    await leave(s);
    await setConfig({});
    return { ok, message: msg && msg.value, candyAtQuit, candyAfterRejoin: candyIn(s) };
  },

  async visitor_at_tier4() {
    const id = await freshSurge();
    const s = await join('BotVisitor', 'visitor');
    const from = H.logOffset();
    H.cmd('surge tier 4');
    const ann = await H.waitLog(/A Haunted (\w+) has appeared near BotVisitor/, { from });
    await sleep(1000);
    const c = await H.throwAtSurgePokemon(s, 'BotVisitor', id, { extraNbt: ',spookysurge_rarity:"rare"' });
    await sleep(1500);
    await shot(s, 'visitor', 'Tier 4: a Haunted Visitor appears; a rare catch pays 12');
    const sawBroadcast = s.events.some((e) => e.kind === 'chat' && /A Haunted \w+ has appeared near BotVisitor/.test(e.value));
    const ok = !!ann && sawBroadcast && !!c && c.rarity === 'rare' && c.paid === 12;
    await leave(s);
    return { ok, announced: ann && ann[0], credit: c, botSawBroadcast: sawBroadcast };
  },

  async untouched_pokemon_expire() {
    await setConfig({ lifetimeSeconds: 30 });
    await freshSurge();
    const s = await join('BotExpire', 'expire');
    await sleep(14000);
    const early = await H.status();
    await sleep(45000);
    const late = await H.status();
    await leave(s);
    await setConfig({});
    return { ok: early.spawned > 0 && late.expired > 0, early, late };
  },

  async three_players_share_the_counter() {
    const id = await freshSurge();
    const names = ['BotA', 'BotB', 'BotC'];
    const bots = [];
    for (const n of names) bots.push(await join(n, 'multi'));
    await sleep(6000);
    const credits = [];
    for (let i = 0; i < bots.length; i++) credits.push(await H.throwAtSurgePokemon(bots[i], names[i], id));
    await sleep(2000);
    const from = H.logOffset();
    H.cmd('surge top');
    await H.waitLog(/3\. \w+ - 1/, { from, timeoutMs: 4000 });
    const top = H.logText().slice(from).match(/\d\. Bot\w - \d+/g) || [];
    const bars = bots.map(barOf);
    await shot(bots[0], 'multi', 'Three players, one shared counter');
    const ok = credits.every(Boolean) && top.length === 3 && bars.every((b) => /\b3 \/ \d+/.test(b));
    for (const b of bots) await leave(b);
    return { ok, credits, top, bars };
  },
};

(async () => {
  const wanted = process.argv.slice(2);
  const names = wanted.length ? wanted : Object.keys(scenarios);
  const results = {};
  for (const n of names) {
    const t0 = Date.now();
    let r;
    try { r = await scenarios[n](); } catch (e) { r = { ok: false, error: String(e && e.stack || e).slice(0, 500) }; }
    r.seconds = Math.round((Date.now() - t0) / 1000);
    results[n] = r;
    console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${n}  (${r.seconds}s)`);
    fs.writeFileSync(path.join(OUT, 'suite.results.json'), JSON.stringify(results, null, 2));
  }
  H.cmd('surge stop');
  const failed = Object.values(results).filter((r) => !r.ok).length;
  console.log(failed ? `${failed} FAILED` : 'ALL PASSED');
  process.exit(failed ? 1 : 0);
})();
