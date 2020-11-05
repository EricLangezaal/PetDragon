package com.ericdebouwer.petdragon;


import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;

public class DragonEvents implements Listener {
	
	PetDragon plugin;
	
	public DragonEvents(PetDragon plugin){
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		new BukkitRunnable() {
			public void run() {
				Entity vehicle = e.getPlayer().getVehicle();
				PetEnderDragon newDragon = handleDragonReset(vehicle);
				if (newDragon != null)
					newDragon.getEntity().addPassenger(e.getPlayer());
				
			}
		}.runTask(plugin);
	}
	
	
	@EventHandler
	public void onLoad(ChunkLoadEvent e){
		// reset dragons so they will still work
		for (Entity ent: e.getChunk().getEntities()){
			handleDragonReset(ent);
		}
	}
	
	public PetEnderDragon handleDragonReset(Entity ent){
		if (!plugin.getFactory().isPetDragon(ent)) return null;
		plugin.getFactory().removeDragon((EnderDragon) ent);
		PetEnderDragon dragon = plugin.getFactory().copy((EnderDragon) ent);
		dragon.spawn();
		return dragon;
	
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void explode(EntityExplodeEvent e){
		if (!plugin.getFactory().isPetDragon(e.getEntity())) return;
		if (plugin.getConfigManager().doGriefing) return;
		e.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void explode(EntityDamageByEntityEvent e){
		if (!plugin.getFactory().isPetDragon(e.getEntity())) return;
		if (plugin.getConfigManager().damageEntities) return;
		e.setCancelled(true);
	}
	
	
	@EventHandler
	public void interact(PlayerInteractEntityEvent e){
		if (e.getHand() != EquipmentSlot.HAND) return; //prevent double firing
		
		if (!plugin.getConfigManager().rightClickRide) return;
		if (!(e.getRightClicked() instanceof EnderDragonPart)) return;
		
		EnderDragonPart part = (EnderDragonPart) e.getRightClicked();
		EnderDragon dragon = part.getParent();
		plugin.getFactory().tryRide(e.getPlayer(), dragon);
	}
	
}
