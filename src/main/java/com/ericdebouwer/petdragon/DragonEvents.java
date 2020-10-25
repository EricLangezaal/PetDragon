package com.ericdebouwer.petdragon;

import java.util.Arrays;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;

public class DragonEvents implements Listener {
	
	PetDragon plugin;
	
	public DragonEvents(PetDragon plugin){
		this.plugin = plugin;
	}
	
	@EventHandler
	public void load(WorldLoadEvent e){
		plugin.getFactory().loadDragons(Arrays.asList(e.getWorld()));
	}
	
	@EventHandler
	public void onLoad(ChunkLoadEvent e){
		// alle draken resetten zodat ze over restart werken
		for (Entity ent: e.getChunk().getEntities()){
			if (ent instanceof EnderDragon && ent.getScoreboardTags().contains(PetEnderDragon.DRAGON_ID)){
				boolean issilent = ent.isSilent; 	/** grab isSilent before removing dragon */
				String cname = ent.getCustomeName; 	/** grab customName before removing dragon */
				plugin.getFactory().removeDragon((EnderDragon) ent);
				PetEnderDragon dragon = plugin.getFactory().copy((EnderDragon) ent);
				dragon.spawn();
				dragon.setSilent(issilent); 		/** setSilent on newly spawned dragon */
				dragon.setCustomName(cname); 		/** setCustomName on newly spawned dragon */
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void explode(EntityExplodeEvent e){
		if (!(e.getEntity() instanceof EnderDragon)) return;
		if (!e.getEntity().getScoreboardTags().contains(PetEnderDragon.DRAGON_ID)) return;
		if (plugin.getConfigManager().doGriefing) return;
		e.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void explode(EntityDamageByEntityEvent e){
		if (!(e.getDamager() instanceof EnderDragon)) return;
		if (!e.getDamager().getScoreboardTags().contains(PetEnderDragon.DRAGON_ID)) return;
		if (plugin.getConfigManager().damageEntities) return;
		e.setCancelled(true);
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
