package com.ericdebouwer.petdragon.database;

import com.ericdebouwer.petdragon.PetDragon;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DragonRegistry {

    private PetDragon plugin;
    private Connection connection;

    public DragonRegistry(PetDragon plugin){
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::setupConnection);
    }

    public void setupConnection(){
        boolean mysql = plugin.getConfig().getBoolean("database.mysql.enabled");
        if (mysql){
            connection = new MySqlConnection(plugin, plugin.getConfig().getConfigurationSection("database.mysql"));
        }else {
            connection = new SQLiteConnection(plugin, plugin.getConfig().getConfigurationSection("database.sqlite"));
        }
        plugin.getLogger().log(Level.INFO, "now using " + (mysql ? "MySQL" : "SQLite") + " storage to keep track of the PetDragons!");
    }

    public void close(){
        connection.close();
    }

    public void handleDragonReset(EnderDragon dragon, Runnable onRemove){

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (connection.didJustRemove(dragon.getUniqueId(), true)){
                Bukkit.getScheduler().runTask(plugin, onRemove);
            }else {
                connection.addOrUpdate(dragon);
            }
        });
    }

    public void updateDragon(EnderDragon dragon){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> connection.addOrUpdate(dragon));
    }

    public void setRemoved(EnderDragon dragon){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> connection.didJustRemove(dragon.getUniqueId(), false));
    }

    public CompletableFuture<Boolean> remove(UUID dragonID){
        Entity ent = Bukkit.getEntity(dragonID);

        if (plugin.getFactory().isPetDragon(ent)){
            ent.remove();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> connection.didJustRemove(dragonID, false));
            return CompletableFuture.completedFuture(true);
        } else {
            return CompletableFuture.supplyAsync(() -> connection.markForRemoval(dragonID));
        }

    }

    public CompletableFuture<Set<DatabaseDragon>> fetchDragons(UUID owner){
        return CompletableFuture.supplyAsync(() -> connection.getDragons(owner));
    }

    public CompletableFuture<Integer> fetchDragonCount(UUID owner){
        return CompletableFuture.supplyAsync(() -> connection.getDragonCount(owner));
    }

    /*
    public void fetchDragons(UUID owner, Consumer<Set<DatabaseDragon>> action){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> action.accept(connection.getDragons(owner)));
        });
    }

    public void fetchDragonCount(UUID owner, Consumer<Integer> action){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> action.accept(connection.getDragonCount(owner)));
        });
    }*/

}
