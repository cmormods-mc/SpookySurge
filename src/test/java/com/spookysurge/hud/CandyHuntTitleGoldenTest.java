package com.spookysurge.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The Python twin (tools/bossbar-art/title.py) wrote these titles; the Java layout must produce the exact same strings. */
class CandyHuntTitleGoldenTest {
    @Test
    void javaAndPythonLayoutsAreIdentical() throws Exception {
        CandyHuntTitle t = new CandyHuntTitle(CandyHuntMetrics.load());
        JsonArray cases;
        try (var in = getClass().getClassLoader().getResourceAsStream("candyhunt_golden.json")) {
            cases = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        }
        assertFalse(cases.isEmpty());
        for (JsonElement e : cases) {
            JsonObject c = e.getAsJsonObject();
            String timer = c.get("timer").isJsonNull() ? null : c.get("timer").getAsString();
            assertEquals(c.get("title").getAsString(), t.title(c.get("name").getAsString(), c.get("tier").getAsInt(), timer),
                    "name '" + c.get("name").getAsString() + "' tier " + c.get("tier").getAsInt());
        }
    }
}
