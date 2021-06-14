package com.ericdebouwer.petdragon;

import java.util.Arrays;

import com.ericdebouwer.petdragon.api.DragonSwoopEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.spigotmc.event.entity.EntityDismountEvent;

public class DragonEvents implements Listener {
	
	PetDragon plugin;
	
	public DragonEvents(PetDragon plugin){
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
			Entity vehicle = e.getPlayer().getVehicle();
			plugin.getFactory().handleDragonReset(vehicle);
		});
	}
	
	@EventHandler
	public void worldLoad(WorldLoadEvent e){
		for (Entity ent: e.getWorld().getEntitiesByClass(EnderDragon.class)){
			plugin.getFactory().handleDragonReset(ent);
		}
	}
	
	
	@EventHandler
	public void onLoad(ChunkLoadEvent e){
		// reset dragons so they will still work
		for (Entity ent: e.getChunk().getEntities()){
			plugin.getFactory().handleDragonReset(ent);
		}
	}

	/*
	@EventHandler(priority=EventPriority.HIGHEST)
	public void explode(EntityExplodeEvent e){
		if (!plugin.getFactory().isPetDragon(e.getEntity())) return;
		if (plugin.getConfigManager().doGriefing) return;
		e.setCancelled(true);
	}
	*/

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onSwoop(DragonSwoopEvent event){
		if (!(event.getTarget() instanceof Player)) return;
		Player target = (Player) event.getTarget();

		if (shouldCancelAttack(event.getEntity(), target)) event.setCancelled(true);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityDamage(EntityDamageByEntityEvent e){
		Entity damager = e.getDamager();
		if (damager instanceof AreaEffectCloud){
			AreaEffectCloud cloud = (AreaEffectCloud) damager;
			if (cloud.getSource() instanceof Entity) {
				damager = (Entity) cloud.getSource();
			}
		}

		if (!plugin.getFactory().isPetDragon(damager)) return;
		if (!plugin.getConfigManager().damageEntities) e.setCancelled(true);
		if (!(e.getEntity() instanceof Player)) return;
		
		Player player = (Player) e.getEntity();
		if (shouldCancelAttack((EnderDragon) damager, player)) e.setCancelled(true);
	}

	private boolean shouldCancelAttack(EnderDragon dragon, Player player){
		return player.getUniqueId().equals(plugin.getFactory().getOwner(dragon)) ||
				dragon.getPassengers().contains(player);
	}
	
	//stop kick for flying
	@EventHandler(priority=EventPriority.LOWEST)
	public void kick(PlayerKickEvent e){
		if (!e.getReason().toLowerCase().contains("flying")) return;
		if (e.getPlayer().getNoDamageTicks() > 10) e.setCancelled(true);
		if (plugin.getFactory().isPetDragon(e.getPlayer().getVehicle())) e.setCancelled(true);
	}
	
	@EventHandler
	public void dragonDismount(EntityDismountEvent e){
		if (!plugin.getFactory().isPetDragon(e.getDismounted())) return;
		plugin.getDragonRegistry().updateDragon((EnderDragon) e.getDismounted());
		if (!(e.getEntity() instanceof Player)) return;
		Player player = (Player) e.getEntity();
		//prevent fall damage
		player.setNoDamageTicks(150);
	}
	
	@EventHandler
	public void riderDamage(EntityDamageEvent e){
		if (!plugin.getFactory().isPetDragon(e.getEntity().getVehicle())) return;
		if (Arrays.asList(DamageCause.FLY_INTO_WALL, DamageCause.ENTITY_EXPLOSION, DamageCause.DRAGON_BREATH, DamageCause.FALL)
				.contains(e.getCause())) e.setCancelled(true);
	}

	@EventHandler
	public void onDeath(EntityDeathEvent event){
		if (!plugin.getFactory().isPetDragon(event.getEntity())) return;
		plugin.getDragonRegistry().setRemoved((EnderDragon) event.getEntity());
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
