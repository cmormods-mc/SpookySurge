// Helpers shared by the bot scenarios: talk to the server console, read its log, and make a bot
// throw a Master Ball at a surge Pokemon.
const fs = require('fs');
const { sleep } = require('./surgebot');

const CONSOLE = process.env.CONSOLE, LOG = process.env.LOG;
if (!CONSOLE || !LOG) throw new Error('set CONSOLE (the file the server reads commands from) and LOG (the server log)');

const cmd = (c) => fs.appendFileSync(CONSOLE, c + '\n');
const logText = () => fs.readFileSync(LOG, 'utf8');
const logOffset = () => logText().length;

/** Waits until a line matching `re` appears in the server log after `from`; returns the match or null. */
async function waitLog(re, { from = 0, timeoutMs = 15000 } = {}) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    const m = logText().slice(from).match(re);
    if (m) return m;
    await sleep(250);
  }
  return null;
}

async function surgeId() {
  const from = logOffset();
  cmd('surge status');
  const m = await waitLog(/Surge id: ([0-9a-f-]{36})/, { from });
  return m ? m[1] : null;
}

async function status() {
  const from = logOffset();
  cmd('surge status');
  await waitLog(/Surge Pokemon alive|No surge is running/, { from });
  const t = logText().slice(from);
  const run = t.match(/Surge running: tier (\d+), (\d+) \/ (\d+)/);
  const live = t.match(/alive: (\d+) \(spawned (\d+), credited (\d+), expired (\d+)\)/);
  return {
    tier: run ? +run[1] : null, counter: run ? +run[2] : null, target: run ? +run[3] : null,
    alive: live ? +live[1] : null, spawned: live ? +live[2] : null, credited: live ? +live[3] : null, expired: live ? +live[4] : null,
  };
}

const CREDIT = (who) => new RegExp(`Surge credit: ${who} took a (\\w+) (\\w+) \\(paid (\\d+) Candy Corn, counter (\\d+)\\)`, 'g');
const credits = (who) => [...logText().matchAll(CREDIT(who))].map((m) => ({ rarity: m[1], species: m[2], paid: +m[3], counter: +m[4] }));

/**
 * Puts a surge Pokemon (optionally filtered by extra NBT) three blocks in front of the bot and throws
 * the held ball at it. Resolves with the new credit record, or null if nothing was paid in time.
 */
async function throwAtSurgePokemon(s, name, id, { extraNbt = '', attempts = 5 } = {}) {
  const { bot } = s;
  const have = credits(name).length;
  const selector = `@e[type=cobblemon:pokemon,nbt={Pokemon:{PersistentData:{spookysurge:"${id}"${extraNbt}}}},limit=1,sort=nearest]`;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    cmd(`execute as ${name} at @s positioned ^ ^ ^3 run tp ${selector} ~ ~ ~`);
    await sleep(700);
    const target = s.nearby(20).find((e) => e.dist < 5);
    s.record('target', target || null);
    if (target) await bot.lookAt(bot.entity.position.offset(target.x - bot.entity.position.x, target.y - bot.entity.position.y + 0.4 - 1.62, target.z - bot.entity.position.z).offset(0, 1.62, 0), true);
    bot.setQuickBarSlot(0);
    await sleep(200);
    bot.activateItem();
    s.record('threw', { attempt });
    for (let i = 0; i < 24; i++) {
      await sleep(500);
      const now = credits(name);
      if (now.length > have) return now[now.length - 1];
    }
  }
  return null;
}

module.exports = { cmd, logText, logOffset, waitLog, surgeId, status, credits, throwAtSurgePokemon, sleep };
