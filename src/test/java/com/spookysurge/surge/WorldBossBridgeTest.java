package com.spookysurge.surge;

import static org.junit.jupiter.api.Assertions.*;

import com.spookysurge.config.SurgeConfig;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class WorldBossBridgeTest {
    @Test
    void aBossThatStartedIsDone() {
        assertEquals(WorldBossBridge.Outcome.BOSS_STARTED, WorldBossBridge.decide(WorldBossBridge.Result.ok(), true));
    }

    @Test
    void aBossAlreadyRunningIsNotReplacedByAVisitor() {
        WorldBossBridge.Result r = WorldBossBridge.Result.no("a World Boss is already active");
        assertEquals(WorldBossBridge.Outcome.BOSS_ALREADY_RUNNING, WorldBossBridge.decide(r, true));
    }

    @Test
    void anyOtherRefusalFallsBackOrGivesNothing() {
        WorldBossBridge.Result r = WorldBossBridge.Result.no("the World Boss system is turned off");
        assertEquals(WorldBossBridge.Outcome.FALL_BACK_TO_VISITOR, WorldBossBridge.decide(r, true));
        assertEquals(WorldBossBridge.Outcome.NOTHING, WorldBossBridge.decide(r, false));
    }

    @Test
    void withoutCobbleRaidsOnTheClasspathTheAnswerIsNoNotACrash() {
        WorldBossBridge.Result r = WorldBossBridge.start(null, null);
        assertFalse(r.started());
        assertTrue(r.reason().contains("not installed"), r.reason());
    }

    @Test
    void tierPrizeIsValidated() {
        SurgeConfig c = new SurgeConfig();
        assertEquals("worldboss", c.tierPrize);
        assertTrue(c.prizeFallbackToVisitor);
        c.tierPrize = "dragon";
        ArrayList<String> problems = new ArrayList<>();
        c.validated(problems);
        assertEquals("worldboss", c.tierPrize);
        assertTrue(problems.stream().anyMatch(p -> p.contains("tierPrize")));
        c.tierPrize = " Visitor ";
        c.validated(new ArrayList<>());
        assertEquals("visitor", c.tierPrize);
    }
}
