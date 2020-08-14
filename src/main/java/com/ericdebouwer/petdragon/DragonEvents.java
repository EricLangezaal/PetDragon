package com.ericdebouwer.petdragon;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;

public class DragonEvents implements Listener {
	
	PetDragon plugin;
	
	public DragonEvents(PetDragon plugin){
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onLoad(ChunkLoadEvent e){
		// alle draken resetten zodat ze over restart werken
		for (Entity ent: e.getChunk().getEntities()){
			if (ent instanceof EnderDragon && ent.getScoreboardTags().contains(PetEnderDragon.DRAGON_ID)){
				ent.remove();
				PetEnderDragon dragon = plugin.createPetDragon(ent.getLocation());
				dragon.spawn();
			}
		}
	}
	
	
	@EventHandler
	public void interact(PlayerInteractEntityEvent e){
		if (e.getHand() != EquipmentSlot.HAND) return; //prevent double firing
		
		if (!plugin.getConfigManager().rightClickRide) return;
		
		ItemStack handHeld = e.getPlayer().getInventory().getItemInMainHand();
		if ( !(handHeld == null || handHeld.getType().isAir())) return;
		
		if (!(e.getRightClicked() instanceof EnderDragonPart)) return;
		
		EnderDragonPart part = (EnderDragonPart) e.getRightClicked();
		EnderDragon dragon = part.getParent();
		
		if (! dragon.getScoreboardTags().contains(PetEnderDragon.DRAGON_ID)) return;
		if (!e.getPlayer().hasPermission("petdragon.ride")) {
			plugin.getConfigManager().sendMessage(e.getPlayer(), Message.NO_RIDE_PERMISSION, null);
			return;
		}
		dragon.addPassenger(e.getPlayer());
		
			
		
	}

	
}
