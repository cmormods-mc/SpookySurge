// One bot catches a surge Pokemon with a Master Ball and the script checks it was paid.
//
//   node catch.js <botName> <surgeId>
//
// Environment: CONSOLE = file the server reads console commands from (appended to),
//              LOG     = the server's log file (read to confirm the server side),
//              OUT     = prefix for the JSON files written (events, before/after snapshots).
// The server must be in offline mode with a surge already running. The bot is given operator
// rights and the ball from the console, and a surge Pokemon is teleported in front of it, because a
// vanilla-protocol bot cannot tell Cobblemon's entity types apart.
const fs = require('fs');
const { createSurgeBot, sleep } = require('./surgebot');

const [name = 'SurgeBotCatch', surgeId] = process.argv.slice(2);
const CONSOLE = process.env.CONSOLE, LOG = process.env.LOG, OUT = process.env.OUT || 'catch';
if (!CONSOLE || !LOG || !surgeId) { console.error('need CONSOLE, LOG and a surge id'); process.exit(2); }

const cmd = (c) => fs.appendFileSync(CONSOLE, c + '\n');
const logText = () => fs.readFileSync(LOG, 'utf8');
const creditsFor = (who) => (logText().match(new RegExp('Surge credit: ' + who + ' took', 'g')) || []).length;
const candy = (s) => s.inventory().filter((i) => i.name === 'sugar').reduce((n, i) => n + i.count, 0);

(async () => {
  const s = createSurgeBot({ name, outFile: `${OUT}.events.jsonl` });
  const { bot } = s;
  await new Promise((r) => bot.once('spawn', r));
  await sleep(3000);

  cmd(`op ${name}`);
  cmd(`give ${name} cobblemon:master_ball 8`);
  await sleep(2000);
  fs.writeFileSync(`${OUT}.before.json`, JSON.stringify(s.snapshot(), null, 2));
  const before = { candy: candy(s), credits: creditsFor(name) };
  s.record('baseline', before);

  const selector = `@e[type=cobblemon:pokemon,nbt={Pokemon:{PersistentData:{spookysurge:"${surgeId}"}}},limit=1,sort=nearest]`;
  let paid = false;
  for (let attempt = 1; attempt <= 5 && !paid; attempt++) {
    cmd(`execute as ${name} at @s positioned ^ ^ ^3 run tp ${selector} ~ ~ ~`);
    await sleep(700);
    const target = s.nearby(20).find((e) => e.dist < 5);
    s.record('target', target || null);
    if (target) await bot.lookAt(bot.entity.position.offset(target.x - bot.entity.position.x, target.y - bot.entity.position.y + 0.4 - 1.62, target.z - bot.entity.position.z).offset(0, 1.62, 0), true);
    bot.setQuickBarSlot(0);
    await sleep(200);
    bot.activateItem();
    s.record('threw', { attempt, held: bot.heldItem ? bot.heldItem.name : null });
    for (let i = 0; i < 24 && !paid; i++) { await sleep(500); paid = creditsFor(name) > before.credits; }
  }

  await sleep(1500);
  const after = s.snapshot();
  fs.writeFileSync(`${OUT}.after.json`, JSON.stringify(after, null, 2));
  const result = { paid, candyBefore: before.candy, candyAfter: candy(s), creditsBefore: before.credits, creditsAfter: creditsFor(name) };
  s.record('result', result);
  fs.writeFileSync(`${OUT}.result.json`, JSON.stringify(result, null, 2));
  bot.quit();
  await sleep(500);
  process.exit(paid ? 0 : 1);
})().catch((e) => { console.error(e); process.exit(3); });
