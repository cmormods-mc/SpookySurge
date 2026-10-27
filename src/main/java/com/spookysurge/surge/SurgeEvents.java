package com.spookysurge.surge;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.spookysurge.SpookySurge;
import net.minecraft.server.level.ServerPlayer;

/** Cobblemon listeners. Each is wrapped, because an exception here would unwind into Cobblemon's own event dispatch. */
public final class SurgeEvents {
    private static boolean registered;

    private SurgeEvents() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CobblemonEvents.POKEMON_CAPTURED.subscribe(SurgeEvents::onCaptured);
        CobblemonEvents.BATTLE_VICTORY.subscribe(SurgeEvents::onVictory);
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(SurgeEvents::onExperience);
    }

    private static void onCaptured(PokemonCapturedEvent event) {
        try {
            SurgeManager.credit(event.getPlayer(), event.getPokemon());
        } catch (RuntimeException e) {
            SpookySurge.LOG.error("Spooky Surge: capture handling failed", e);
        }
    }

    private static void onExperience(ExperienceGainedEvent.Pre event) {
        try {
            event.setExperience(SurgeManager.adjustExperience(event.getExperience()));
        } catch (RuntimeException e) {
            SpookySurge.LOG.error("Spooky Surge: experience bonus failed; the gain is unchanged", e);
        }
    }

    private static void onVictory(BattleVictoryEvent event) {
        try {
            ServerPlayer winner = null;
            for (BattleActor actor : event.getWinners()) {
                if (actor instanceof PlayerBattleActor player && player.getEntity() != null) {
                    winner = player.getEntity();
                    break;
                }
            }
            if (winner == null) return;
            for (BattleActor loser : event.getLosers()) {
                if (loser instanceof PokemonBattleActor wild) {
                    Pokemon pokemon = wild.getPokemon().getOriginalPokemon();
                    SurgeManager.credit(winner, pokemon);
                }
            }
        } catch (RuntimeException e) {
            SpookySurge.LOG.error("Spooky Surge: battle-victory handling failed", e);
        }
    }
}
