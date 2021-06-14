package com.ericdebouwer.petdragon.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class DatabaseDragon {

    private UUID uuid;
    private UUID owner;

    private Vector coordinates;
    private String worldName;

    public DatabaseDragon(String uuid, String owner, String worldName, int x, int y, int z){
        this.uuid = UUID.fromString(uuid);
        this.owner = UUID.fromString(owner);
        this.worldName = worldName;
        this.coordinates = new Vector(x, y, z);
    }

    public @Nullable EnderDragon getDragon(){
        Entity dragon = Bukkit.getEntity(this.uuid);
        return dragon == null ? null : (EnderDragon) dragon;
    }

    private void purge(){
        EnderDragon dragon = getDragon();
        if (dragon != null){
            this.coordinates = dragon.getLocation().toVector();
            this.worldName = dragon.getWorld().getName();
        }
    }

    public @Nonnull Location getLocation(){
        this.purge();
        return new Location(Bukkit.getWorld(this.worldName), coordinates.getX(), coordinates.getY(), coordinates.getZ());
    }

    public @Nonnull String getWorldName(){
        this.purge();
        return this.worldName;
    }

    public @Nonnull UUID getUniqueId(){
        return this.uuid;
    }

    public @Nonnull UUID getOwner(){
        return this.owner;
    }


}
