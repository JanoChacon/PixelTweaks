package com.strangeone101.pixeltweaks.integration.ftbquests;

import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.TaskType;

public class TradeTask extends PokemonTask {

    public TradeTask(Quest q) {
        super(q);
    }

    @Override
    public TaskType getType() {
        return PokemonTaskTypes.TRADE_POKEMON;
    }

    public void tradePokemon(TeamData team, Pokemon pokemon) {
        if (!team.isCompleted(this) && (this.pokemonSpec.isEmpty() || this.cachedSpec.matches(pokemon))) {
            team.addProgress(this, 1L);
        }
    }
}
