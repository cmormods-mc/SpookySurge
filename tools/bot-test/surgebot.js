// A mineflayer bot that records everything a player would see during a surge.
//
// Cobblemon adds recipe types a vanilla protocol parser does not know, so the recipes packet is
// treated as opaque bytes. That is the only tweak a vanilla-protocol client needs to join.
const mineflayer = require('mineflayer');
const fs = require('fs');

const SKIP_RECIPES = { '1.21': { play: { toClient: { types: { packet_declare_recipes: 'restBuffer' } } } } };

function createSurgeBot({ name, host = 'localhost', port = 25565, outFile, viewDistance }) {
  const started = Date.now();
  if (outFile) fs.writeFileSync(outFile, '');
  const events = [];
  const record = (kind, value) => {
    const row = { ms: Date.now() - started, kind, value };
    events.push(row);
    if (outFile) fs.appendFileSync(outFile, JSON.stringify(row) + '\n');
  };

  const bot = mineflayer.createBot({
    host, port, username: name, version: '1.21.1', auth: 'offline', customPackets: SKIP_RECIPES, hideErrors: true, ...(viewDistance ? { viewDistance } : {}),
  });

  const text = (m) => (m && m.toString ? m.toString() : String(m));
  bot.once('login', () => record('login', name));
  bot.once('spawn', () => record('spawn', bot.entity.position));
  bot.on('message', (m) => record('chat', text(m)));
  bot.on('actionBar', (m) => record('actionBar', text(m)));
  bot.on('title', (t) => record('title', text(t)));
  bot.on('bossBarCreated', (b) => record('bossCreated', barOf(b)));
  bot.on('bossBarUpdated', (b) => record('bossUpdated', barOf(b)));
  bot.on('bossBarDeleted', (b) => record('bossDeleted', barOf(b)));
  bot.on('kicked', (r) => record('kicked', typeof r === 'string' ? r : JSON.stringify(r)));
  let parseErrors = 0;
  bot.on('error', (e) => { parseErrors++; if (parseErrors <= 3) record('error', String(e).slice(0, 300)); });
  bot.on('end', (r) => record('end', r));

  function barOf(b) {
    return { title: text(b.title), progress: Number(b.health.toFixed(3)), color: b.color, dividers: b.dividers };
  }

  /** Plain text of a name or lore component as it arrives in an item's NBT components. */
  function textOf(n) {
    if (n == null) return '';
    if (typeof n === 'string') return n;
    if (Array.isArray(n)) return n.map(textOf).join('');
    if (n.type === 'string') return n.value;
    if (n.type === 'list') return textOf((n.value && n.value.value) || []);
    if (n.type === 'compound') return textOf(n.value);
    return textOf(n.text) + textOf(n.extra);
  }

  function inventory() {
    return bot.inventory.items().map((item) => {
      const c = item.components || [];
      const find = (type) => (c.find((x) => x.type === type) || {}).data;
      return {
        name: item.name,
        count: item.count,
        display: item.displayName,
        itemName: find('item_name') ? textOf(find('item_name')) : undefined,
        lore: find('lore') ? find('lore').map(textOf).join(' / ') : undefined,
        modelData: find('custom_model_data'),
      };
    });
  }

  /** Entities that vanilla knows nothing about (Cobblemon's) have a numeric type id above vanilla's range. */
  function nearby(limit = 60) {
    const me = bot.entity.position;
    return Object.values(bot.entities)
      .filter((e) => e !== bot.entity)
      .map((e) => ({ id: e.id, typeId: e.entityType !== undefined ? e.entityType : (e.entityTypeId !== undefined ? e.entityTypeId : null), name: e.name || null, kind: e.type, dist: Number(e.position.distanceTo(me).toFixed(1)), x: e.position.x, y: e.position.y, z: e.position.z }))
      .sort((a, b) => a.dist - b.dist)
      .slice(0, limit);
  }

  function snapshot() {
    return {
      bot: name,
      position: bot.entity ? bot.entity.position : null,
      inventory: inventory(),
      bossBars: bot.bossBars.map(barOf),
      nearby: nearby(),
      lastChat: events.filter((e) => e.kind === 'chat').slice(-8).map((e) => e.value),
      lastActionBar: (events.filter((e) => e.kind === 'actionBar').slice(-1)[0] || {}).value || '',
    };
  }

  return { bot, events, record, snapshot, inventory, nearby };
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

module.exports = { createSurgeBot, sleep };
