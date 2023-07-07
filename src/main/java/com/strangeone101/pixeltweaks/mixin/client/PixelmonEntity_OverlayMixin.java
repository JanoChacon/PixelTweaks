package com.strangeone101.pixeltweaks.mixin.client;

import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonBase;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractHoldsItemsEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.strangeone101.pixeltweaks.client.overlay.PixelmonEntityLayerExtension;
import com.strangeone101.pixeltweaks.client.overlay.PokemonOverlay;
import net.minecraft.entity.EntityType;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@OnlyIn(Dist.CLIENT)
@Mixin(PixelmonEntity.class)
public class PixelmonEntity_OverlayMixin extends AbstractHoldsItemsEntity implements PixelmonEntityLayerExtension {

    @Unique
    @OnlyIn(Dist.CLIENT)
    private PokemonOverlay pixelTweaks$overlay;

    public PixelmonEntity_OverlayMixin(EntityType<? extends AbstractHoldsItemsEntity> type, World par1World) {
        super(type, par1World);
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public void setPTOverlay(PokemonOverlay overlay) {
        this.pixelTweaks$overlay = overlay;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public PokemonOverlay getPTOverlay() {
        return pixelTweaks$overlay;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setPokemon(Pokemon pokemon) {
        super.setPokemon(pokemon);

        this.pixelTweaks$set(pokemon);
    }

    @Unique
    private void pixelTweaks$set(Pokemon pokemon) {
        if (PokemonOverlay.SPECIES_EVENTS.containsKey(pokemon.getSpecies())) {
            for (PokemonSpecification spec : PokemonOverlay.SPECIES_EVENTS.get(pokemon.getSpecies()).keySet()) {
                if (spec.matches(pokemon)) {
                    this.setPTOverlay(PokemonOverlay.SPECIES_EVENTS.get(pokemon.getSpecies()).get(spec));
                    break;
                }
            }
        } else {
            for (PokemonSpecification spec : PokemonOverlay.NON_SPECIES_EVENT.keySet()) {
                if (spec.matches(pokemon)) {
                    this.setPTOverlay(PokemonOverlay.NON_SPECIES_EVENT.get(spec));
                    break;
                }
            }
        }
    }


    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);

        if (key.getId() == PokemonBase.SYNC_POKEMON_BASE.getParameterId()) {
            this.pixelTweaks$set(this.pokemon);
        }
    }
}
