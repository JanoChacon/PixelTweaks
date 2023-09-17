package com.strangeone101.pixeltweaks.integration.ftbquests.tasks;

import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.api.pokemon.requirement.impl.SpeciesRequirement;
import com.pixelmonmod.pixelmon.battles.controller.participants.RaidPixelmonParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.strangeone101.pixeltweaks.integration.ftbquests.PokemonTask;
import com.strangeone101.pixeltweaks.integration.ftbquests.PokemonTaskTypes;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class DefeatTask extends PokemonTask {

    public Tristate wild = Tristate.DEFAULT;
    public String usedPokemonSpec = "";
    public transient PokemonSpecification cachedUsedSpec;
    public boolean invertUsed;

    public DefeatTask(Quest q) {
        super(q);
    }

    @Override
    public TaskType getType() {
        return PokemonTaskTypes.DEFEAT_POKEMON;
    }

    @Override
    public void writeData(CompoundNBT nbt) {
        super.writeData(nbt);
        wild.write(nbt, "wild");
        nbt.putString("usedPokemonSpec", usedPokemonSpec);
        nbt.putBoolean("invertUsed", invertUsed);
    }

    @Override
    public void readData(CompoundNBT nbt) {
        super.readData(nbt);
        wild = Tristate.read(nbt, "wild");
        usedPokemonSpec = nbt.getString("usedPokemonSpec");
        cachedUsedSpec = PokemonSpecificationProxy.create(usedPokemonSpec);
        invertUsed = nbt.getBoolean("invertUsed");
    }

    @Override
    public void writeNetData(PacketBuffer buffer) {
        super.writeNetData(buffer);
        wild.write(buffer);
        buffer.writeString(usedPokemonSpec);
        buffer.writeBoolean(invertUsed);
    }

    @Override
    public void readNetData(PacketBuffer buffer) {
        super.readNetData(buffer);
        wild = Tristate.read(buffer);
        usedPokemonSpec = buffer.readString();
        cachedUsedSpec = PokemonSpecificationProxy.create(usedPokemonSpec);
        invertUsed = buffer.readBoolean();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Icon getAltIcon() {
        if (cachedUsedSpec.getValue(SpeciesRequirement.class).isPresent() && !this.usedPokemonSpec.isEmpty()) {
            return Icon.getIcon(cachedUsedSpec.create().getSprite());
        }

        return super.getAltIcon();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ITextComponent getAltTitle() {
        if (usedPokemonSpec.isEmpty()) return super.getAltTitle();

        StringTextComponent pokemonDefeat = new StringTextComponent("");
        if (count > 1) {
            pokemonDefeat.appendString(count + "x ");
        }
        pokemonDefeat.appendSibling(getPokemon());

        ITextComponent usedPokemon = getPokemon(cachedUsedSpec);

        return new TranslationTextComponent("ftbquests.task."
                + this.getType().id.getNamespace() + '.' + this.getType().id.getPath() + ".title.with",
                pokemonDefeat, usedPokemon);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void getConfig(ConfigGroup config) {
        super.getConfig(config);
        config.addTristate("wild", wild, v -> wild = v, Tristate.DEFAULT);
        config.addString("usedPokemonSpec", usedPokemonSpec, v -> usedPokemonSpec = v, "");
        config.addBool("invertUsed", invertUsed, v -> invertUsed = v, false);
    }

    public void defeatPokemon(TeamData team, PixelmonEntity pokemon, PixelmonEntity usedPokemon) {
        if (!team.isCompleted(this) && (this.cachedSpec == null || this.cachedSpec.matches(pokemon) != this.invert)
        && (wild == Tristate.DEFAULT || (pokemon.getPixelmonWrapper().getParticipant() instanceof WildPixelmonParticipant
                        || pokemon.getPixelmonWrapper().getParticipant() instanceof RaidPixelmonParticipant) == wild.get(true))
        && (this.usedPokemonSpec.isEmpty() || this.cachedUsedSpec.matches(usedPokemon) != this.invertUsed)) {
            team.addProgress(this, 1L);
        }
    }
}
