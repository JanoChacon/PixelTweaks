package com.strangeone101.pixeltweaks.pixelevents.condition;

import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.strangeone101.pixeltweaks.pixelevents.Condition;

public class PokemonCondition extends Condition<Pokemon> {
    public PokemonSpecification spec;
    public Boolean wild;

    @Override
    public boolean conditionMet(Pokemon pokemon) {
        if (wild != null && (pokemon.getOriginalTrainer() == null) != wild) return false;
        if (spec != null && !spec.matches(pokemon)) return false;

        return true;
    }

    @Override
    public Pokemon itemFromPixelmon(PixelmonEntity entity) {
        return entity.getPokemon();
    }

    @Override
    public String toString() {
        return "PokemonCondition{" +
                "spec='" + spec + '\'' +
                ", wild=" + wild +
                '}';
    }

    // Add getters and setters for the 'species' field
}