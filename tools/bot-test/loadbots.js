// A child process for loadtest.js: joins `count` idle bots and stays until told to quit.
//   node loadbots.js <startIndex> <count>
const { createSurgeBot, sleep } = require('./surgebot');
const [start, count] = process.argv.slice(2).map(Number);
const bots = [];
(async () => {
  for (let i = 0; i < count; i++) {
    const name = `Load${String(start + i).padStart(3, '0')}`;
    const s = createSurgeBot({ name, viewDistance: 6 });
    s.bot.on('end', () => process.send && process.send({ ended: name }));
    bots.push(s);
    await sleep(1200);
  }
  process.send && process.send({ ready: bots.length });
})();
process.on('message', (m) => { if (m === 'quit') { bots.forEach((b) => { try { b.bot.quit(); } catch (e) {} }); setTimeout(() => process.exit(0), 800); } });
