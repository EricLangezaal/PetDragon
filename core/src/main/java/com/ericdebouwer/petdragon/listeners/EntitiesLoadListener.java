package com.ericdebouwer.petdragon.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.ericdebouwer.petdragon.PetDragon;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class EntitiesLoadListener {

    public EntitiesLoadListener(PetDragon plugin) {
        try {
            Class.forName("com.destroystokyo.paper.event.entity.EntityAddToWorldEvent");
            plugin.getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler
                private void onEntitiesLoad(EntityAddToWorldEvent event) {
                    plugin.getFactory().handleOldDragon(event.getEntity());
                }
            }, plugin);
        } catch (ClassNotFoundException e1) {
            try {
                Class.forName("org.bukkit.event.world.EntitiesLoadEvent");
                plugin.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    private void onEntitiesLoad(EntitiesLoadEvent event) {
                        event.getEntities().forEach(ent -> plugin.getFactory().handleOldDragon(ent));
                    }
                }, plugin);
            } catch (ClassNotFoundException e2) {
                plugin.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    private void onChunkLoad(ChunkLoadEvent event) {
                        for (Entity ent: event.getChunk().getEntities()){
                            plugin.getFactory().handleOldDragon(ent);
                        }
                    }
                }, plugin);
            } finally {
                plugin.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    private void onChunkLoad(WorldLoadEvent event) {
                        for (Entity ent: event.getWorld().getEntitiesByClass(EnderDragon.class)){
                            plugin.getFactory().handleOldDragon(ent);
                        }
                    }
                }, plugin);
            }

        }
    }
}
