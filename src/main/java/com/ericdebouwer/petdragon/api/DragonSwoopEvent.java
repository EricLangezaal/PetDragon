package com.ericdebouwer.petdragon.api;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;

/**
 * Fired when a PetDragon swoops a LivingEntity, so it launches it away. <br>
 * If `do-entity-interact` is disabled in PetDragon's configuration, the event will fire in a cancelled state.
 *
 * @author 3ricL (Ericdebouwer)
 */

public class DragonSwoopEvent extends EntityTargetLivingEntityEvent {


    private Vector velocity;

    public DragonSwoopEvent(EnderDragon dragon, LivingEntity target, Vector velocity){
        super(dragon, target, TargetReason.CUSTOM);
        this.velocity = velocity;
    }

    /**
     * Get the <b>additional</b> velocity this swoop will add to the target entity
     *
     * @return {@link org.bukkit.util.Vector} to denote additional velocity
     */
    public @Nonnull Vector getVelocity() {
        return velocity;
    }

    /**
     * Set the <b>additional</b> velocity this swoop will add to the target entity
     *
     * @param velocity {@link org.bukkit.util.Vector} for additional velocity
     */
    public void setVelocity(@Nonnull Vector velocity){
        this.velocity = velocity;
    }

    /**
     * Get the {@link org.bukkit.entity.EnderDragon} that is launching an entity
     *
     * @return the EnderDragon
     */
    @Override
    public @Nonnull EnderDragon getEntity(){
        return (EnderDragon) this.entity;
    }

}
