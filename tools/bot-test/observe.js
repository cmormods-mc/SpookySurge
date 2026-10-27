// Join, wait, and dump what the bot saw. Usage: node observe.js <name> <seconds> <outPrefix>
const { createSurgeBot, sleep } = require('./surgebot');
const fs = require('fs');
(async () => {
  const [name = 'SurgeBotA', seconds = '30', prefix = 'observe'] = process.argv.slice(2);
  const s = createSurgeBot({ name, outFile: `${prefix}.events.jsonl` });
  await sleep(Number(seconds) * 1000);
  fs.writeFileSync(`${prefix}.snapshot.json`, JSON.stringify(s.snapshot(), null, 2));
  s.bot.quit();
  await sleep(500);
  process.exit(0);
})();
