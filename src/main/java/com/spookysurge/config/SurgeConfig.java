package com.spookysurge.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything an operator can tune, read from {@code config/spookysurge/server.json}. Plain fields so
 * Gson can read and write it; {@link #validated()} repairs a bad value instead of refusing to start,
 * because a typo in a number must not take down a server on the night of the event.
 */
public final class SurgeConfig {
    public boolean enabled = true;

    /** The boss bar: "auto" (art for players whose client has the server's resource pack, plain text for everyone else), "art" or "plain". */
    public String bossBar = "auto";

    /** The event window, as UTC instants. Automatic surges only start inside it. */
    public String windowStart = "2026-10-31T04:00:00Z";
    public String windowEnd = "2026-11-03T05:00:00Z";

    /** Local clock times (HH:mm) in {@link #timezone} at which a surge starts each day of the window. */
    public String timezone = "America/New_York";
    public List<String> startTimes = new ArrayList<>(List.of("20:00"));

    public int countdownSeconds = 60;
    public int durationMinutes = 30;

    /** How often the spawner looks at each player, in ticks. {@link #boostedSpawnIntervalTicks} from tier 1. */
    public int spawnIntervalTicks = 100;
    public int boostedSpawnIntervalTicks = 60;
    public int perPlayerCap = 4;
    public int serverCap = 40;
    /** Spawns started per second across all players. Spreads the first wave over several seconds instead of one slow tick. */
    public int maxSpawnsPerSecond = 5;
    public int minSpawnDistance = 12;
    public int maxSpawnDistance = 32;
    public int lifetimeSeconds = 300;
    /** Wild level range for surge spawns. */
    public int levelMin = 10;
    public int levelMax = 35;

    /** The shared counter's target is {@code targetPerPlayer * online}, clamped to [targetMin, targetMax]. */
    public int targetPerPlayer = 60;
    public int targetMin = 100;
    public int targetMax = 1000;
    /** Fractions of the target at which tiers 1..4 unlock. Must be increasing and in (0, 1]. */
    public List<Double> tierFractions = new ArrayList<>(List.of(0.25, 0.5, 0.75, 1.0));

    /** Candy Corn per catch or defeat, by species rarity. */
    public int candyCommon = 1;
    public int candyUncommon = 3;
    public int candyRare = 6;
    /** From this tier on, catch rewards are multiplied by {@link #doubleCandyMultiplier}. */
    public int doubleCandyTier = 3;
    public int doubleCandyMultiplier = 2;
    /** Everyone with at least {@link #participationMin} rewarded actions gets this once, when the surge ends. */
    public int participationBonus = 5;
    public int participationMin = 3;
    /** Rewarded actions per player per surge; beyond it a catch still counts for the meter but pays nothing. */
    public int perPlayerRewardCap = 60;

    /** Shiny odds are multiplied by this for surge Pokemon from {@link #shinyTier}. */
    public double shinyMultiplier = 3.0;
    public int shinyTier = 2;
    /** The tier at which one rare Haunted Visitor appears for the table to race for. */
    public int visitorTier = 4;

    /** Off by default. When on, every Pokemon's experience gain is raised by this percent from this tier while a surge runs. */
    public boolean xpBonusEnabled = false;
    public int xpBonusPercent = 25;
    public int xpBonusTier = 2;

    public List<SpeciesEntry> species = defaultSpecies();

    public static final class SpeciesEntry {
        /** A Cobblemon species id such as {@code gastly}. */
        public String id;
        public int weight;
        /** One of {@code common}, {@code uncommon}, {@code rare}. */
        public String rarity;

        public SpeciesEntry() {}

        public SpeciesEntry(String id, int weight, String rarity) {
            this.id = id;
            this.weight = weight;
            this.rarity = rarity;
        }
    }

    public static List<SpeciesEntry> defaultSpecies() {
        List<SpeciesEntry> list = new ArrayList<>();
        for (String id : new String[] {"gastly", "shuppet", "phantump", "pumpkaboo", "litwick"}) {
            list.add(new SpeciesEntry(id, 10, "common"));
        }
        for (String id : new String[] {"haunter", "mimikyu", "sableye", "duskull", "misdreavus", "yamask"}) {
            list.add(new SpeciesEntry(id, 4, "uncommon"));
        }
        for (String id : new String[] {"gengar", "spiritomb", "dusclops", "banette", "decidueye", "dragapult"}) {
            list.add(new SpeciesEntry(id, 1, "rare"));
        }
        return list;
    }

    /** The species the Haunted Visitor is drawn from. */
    public List<String> visitorSpecies = new ArrayList<>(List.of("dragapult", "mimikyu", "spiritomb", "gengar"));

    /**
     * Returns a config that is safe to use: out-of-range numbers are clamped and malformed lists fall
     * back to their defaults. Each repair is appended to {@code problems} so the caller can log it.
     */
    public SurgeConfig validated(List<String> problems) {
        if (bossBar == null || !List.of("auto", "art", "plain").contains(bossBar.trim().toLowerCase())) {
            problems.add("bossBar must be auto, art or plain; reset to auto");
            bossBar = "auto";
        }
        countdownSeconds = clamp("countdownSeconds", countdownSeconds, 0, 600, problems);
        durationMinutes = clamp("durationMinutes", durationMinutes, 1, 240, problems);
        spawnIntervalTicks = clamp("spawnIntervalTicks", spawnIntervalTicks, 20, 1200, problems);
        boostedSpawnIntervalTicks = clamp("boostedSpawnIntervalTicks", boostedSpawnIntervalTicks, 20, spawnIntervalTicks, problems);
        perPlayerCap = clamp("perPlayerCap", perPlayerCap, 1, 20, problems);
        serverCap = clamp("serverCap", serverCap, 1, 200, problems);
        maxSpawnsPerSecond = clamp("maxSpawnsPerSecond", maxSpawnsPerSecond, 1, 100, problems);
        minSpawnDistance = clamp("minSpawnDistance", minSpawnDistance, 4, 64, problems);
        maxSpawnDistance = clamp("maxSpawnDistance", maxSpawnDistance, minSpawnDistance + 4, 96, problems);
        lifetimeSeconds = clamp("lifetimeSeconds", lifetimeSeconds, 30, 3600, problems);
        levelMin = clamp("levelMin", levelMin, 1, 100, problems);
        levelMax = clamp("levelMax", levelMax, levelMin, 100, problems);
        targetPerPlayer = clamp("targetPerPlayer", targetPerPlayer, 1, 10_000, problems);
        targetMin = clamp("targetMin", targetMin, 1, 100_000, problems);
        targetMax = clamp("targetMax", targetMax, targetMin, 1_000_000, problems);
        candyCommon = clamp("candyCommon", candyCommon, 0, 64, problems);
        candyUncommon = clamp("candyUncommon", candyUncommon, 0, 64, problems);
        candyRare = clamp("candyRare", candyRare, 0, 64, problems);
        doubleCandyMultiplier = clamp("doubleCandyMultiplier", doubleCandyMultiplier, 1, 10, problems);
        participationBonus = clamp("participationBonus", participationBonus, 0, 64, problems);
        participationMin = clamp("participationMin", participationMin, 0, 1000, problems);
        perPlayerRewardCap = clamp("perPlayerRewardCap", perPlayerRewardCap, 1, 10_000, problems);
        doubleCandyTier = clamp("doubleCandyTier", doubleCandyTier, 1, 4, problems);
        shinyTier = clamp("shinyTier", shinyTier, 1, 4, problems);
        visitorTier = clamp("visitorTier", visitorTier, 1, 4, problems);
        xpBonusPercent = clamp("xpBonusPercent", xpBonusPercent, 0, 500, problems);
        xpBonusTier = clamp("xpBonusTier", xpBonusTier, 1, 4, problems);
        if (shinyMultiplier < 1.0 || shinyMultiplier > 100.0 || Double.isNaN(shinyMultiplier)) {
            problems.add("shinyMultiplier " + shinyMultiplier + " reset to 3.0");
            shinyMultiplier = 3.0;
        }
        if (!validFractions(tierFractions)) {
            problems.add("tierFractions must be four increasing values in (0, 1]; reset to the defaults");
            tierFractions = new ArrayList<>(List.of(0.25, 0.5, 0.75, 1.0));
        }
        if (species == null || species.stream().noneMatch(SurgeConfig::usable)) {
            problems.add("species has no usable entry; reset to the defaults");
            species = defaultSpecies();
        } else {
            species = new ArrayList<>(species.stream().filter(SurgeConfig::usable).toList());
        }
        if (visitorSpecies == null || visitorSpecies.isEmpty()) {
            problems.add("visitorSpecies is empty; reset to the defaults");
            visitorSpecies = new ArrayList<>(List.of("dragapult", "mimikyu", "spiritomb", "gengar"));
        }
        if (startTimes == null || startTimes.isEmpty()) {
            problems.add("startTimes is empty; reset to 20:00");
            startTimes = new ArrayList<>(List.of("20:00"));
        }
        return this;
    }

    private static boolean usable(SpeciesEntry entry) {
        return entry != null && entry.id != null && !entry.id.isBlank() && entry.weight > 0;
    }

    static boolean validFractions(List<Double> fractions) {
        if (fractions == null || fractions.size() != 4) return false;
        double previous = 0;
        for (Double value : fractions) {
            if (value == null || value.isNaN() || value <= previous || value > 1.0) return false;
            previous = value;
        }
        return true;
    }

    private static int clamp(String name, int value, int min, int max, List<String> problems) {
        if (value >= min && value <= max) return value;
        int fixed = Math.max(min, Math.min(max, value));
        problems.add(name + " " + value + " clamped to " + fixed);
        return fixed;
    }
}
