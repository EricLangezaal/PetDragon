package com.ericdebouwer.petdragon.api;

import com.ericdebouwer.petdragon.DragonFactory;
import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.enderdragonNMS.PetEnderDragon;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Small API to properly interact with the PetDragon plugin. <br>
 * <b>Author</b> - 3ricL (Ericdebouwer).
 */
public class PetDragonAPI {

    private static final PetDragonAPI instance = new PetDragonAPI();

    private final DragonFactory factory;

    /**
     * Get an instance of this API. Make sure your plugin is a dependency of <b>PetDragon</b>,
     * so it will load after PetDragon has initialised.
     * @return an instance of this API.
     */
    public static  PetDragonAPI getInstance(){
        return instance;
    }

    private PetDragonAPI(){
        PetDragon plugin = JavaPlugin.getPlugin(PetDragon.class);
        factory = plugin.getFactory();
    }

    /**
     * Check if an entity is a PetDragon.
     * This does <b>NOT</b> guarantee it can be cast to anything other than EnderDragon!
     * @param entity the entity to check.
     */
    public boolean isPetDragon(@Nonnull Entity entity){
        return factory.isPetDragon(entity);
    }

    /**
     * Spawn a PetDragon for a certain player.
     * @param location the location to spawn this dragon, make sure <tt>location.getWorld()</tt> is <b>not</b> null.
     * @param owningPlayer the UUID of the player who will own this dragon.
     * @return an EnderDragon which corresponds to the PetDragon that just spawned.
     */
    public @Nonnull EnderDragon spawnDragon(@Nonnull Location location, @Nonnull UUID owningPlayer){
        return this.spawnDragon(location, owningPlayer, null);
    }

    /**
     * Spawn a PetDragon for a certain player with the supplied function run <b>before</b> the entity is added to the world.
     * @param location the location to spawn this dragon, make sure <tt>location.getWorld()</tt> is <b>not</b> null.
     * @param owningPlayer the UUID of the player who will own this dragon.
     * @param function the function to be run before the entity is spawned.
     * @return an EnderDragon which corresponds to the PetDragon that just spawned.
     */
    public @Nonnull EnderDragon spawnDragon(@Nonnull Location location, @Nonnull UUID owningPlayer, @Nullable Consumer<EnderDragon> function){
        Validate.notNull(owningPlayer, "Spawning PetDragons without an owner is no longer supported!");
        PetEnderDragon dragon = this.factory.create(location, owningPlayer);
        if (function != null) function.accept(dragon.getEntity());
        dragon.spawn();
        return dragon.getEntity();
    }

    /**
     * Gets the UUID of the player who owns this PetDragon. Can be null for dragons spawned in older versions of this plugin
     * @param dragon the EnderDragon to check, doesn't have to be a PetDragon.
     * @return the UUID of the player who owns this PetDragon if applicable.
     */
    public @Nullable UUID getOwningPlayer(@Nonnull EnderDragon dragon){
        return this.factory.getOwner(dragon);
    }

    @Deprecated
    /**
     *
     * Get all dragons that are currently <b>loaded</b> and owned by a specific player.
     * @param player the player to check for
     * @return a set of EnderDragons corresponding to all <b>loaded</b> PetDragons for this player.
     */
    public @Nonnull Set<EnderDragon> getDragons(@Nonnull OfflinePlayer player){
        Set<EnderDragon> result = new HashSet<>();
        for (World world: Bukkit.getWorlds()){
            for (EnderDragon dragon: world.getEntitiesByClass(EnderDragon.class)){
                if (!isPetDragon(dragon)) continue;
                if (!player.getUniqueId().equals(this.factory.getOwner(dragon))) continue;

                result.add(dragon);
            }
        }
        return result;
    }


}
