package com.ericdebouwer.petdragon;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.ericdebouwer.petdragon.config.Message;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.ericdebouwer.petdragon.enderdragonNMS.PetEnderDragon;
import com.google.common.collect.ImmutableMap;

public class DragonFactory {
	
	PetDragon plugin;
	Class<?> dragonClass;
	private boolean correctVersion;
	public NamespacedKey ownerKey;

	public DragonFactory(PetDragon plugin){
		this.plugin = plugin;
		this.ownerKey = new NamespacedKey(plugin, PetEnderDragon.OWNER_ID);
		this.correctVersion = this.setUpDragonClass();
	}
	
	public boolean isCorrectVersion(){
		return correctVersion;
	}
	
	public boolean setUpDragonClass(){
		String packageName = plugin.getServer().getClass().getPackage().getName();
    	String version = packageName.substring(packageName.lastIndexOf('.') + 1);

    	try {
        	final Class<?> clazz = Class.forName("com.ericdebouwer.petdragon.enderdragonNMS.PetEnderDragon_" + version);
        	if (PetEnderDragon.class.isAssignableFrom(clazz)) { 
        		this.dragonClass = clazz;
        		return true;
        	}
    	} catch (final Exception e) {
        	return false;
   		}
    	return false;
	}

	public PetEnderDragon create(Location loc, UUID owner){
		return this.create(loc, owner, false);
	}
	
	private PetEnderDragon create(Location loc, UUID owner, boolean replace){
		try {
			PetEnderDragon dragon = (PetEnderDragon) dragonClass.getConstructor(Location.class, PetDragon.class).newInstance(loc, plugin);
			if (owner != null){
				dragon.getEntity().getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
			}
			if (!replace){
				plugin.getDragonRegistry().updateDragon(dragon.getEntity());
			}
			return dragon;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean isPetDragon(Entity ent){
		if (!(ent instanceof EnderDragon)) return false;
		return ent.getScoreboardTags().contains(PetEnderDragon.DRAGON_ID);
	}
	
	public boolean canDamage(HumanEntity player, PetEnderDragon dragon){
		UUID owner = this.getOwner(dragon.getEntity());
		if (owner == null) return true;
		if (owner.equals(player.getUniqueId())) return player.hasPermission("petdragon.hurt.self");
		return player.hasPermission("petdragon.hurt.others");
	}
	
	public boolean tryRide(HumanEntity p, EnderDragon dragon){
		if (!isPetDragon(dragon)) return false;
		plugin.getDragonRegistry().updateDragon(dragon);

		ItemStack handHeld = p.getInventory().getItemInMainHand();
		if ( !(handHeld == null || handHeld.getType().isAir())) return false;
		
		if (!p.hasPermission("petdragon.ride")) {
			plugin.getConfigManager().sendMessage(p, Message.NO_RIDE_PERMISSION, null);
			return true;
		}
		UUID owner = getOwner(dragon);
		if (!p.hasPermission("petdragon.bypass.owner") && owner != null && !p.getUniqueId().equals(owner)){
			String ownerName = Bukkit.getOfflinePlayer(owner).getName();
			plugin.getConfigManager().sendMessage(p, Message.NO_JOYRIDE, ImmutableMap.of("owner", ownerName == null ? "unknown" : ownerName));
			return true;
		}
		dragon.addPassenger(p);
		return true;
	}
	
	public void reloadDragons(){
		for (World w: Bukkit.getWorlds()){
			for (EnderDragon dragon: w.getEntitiesByClass(EnderDragon.class)){
				plugin.getFactory().handleDragonReset(dragon);
			}
		}
	}

	public void handleDragonReset(Entity ent){
		if (!isPetDragon(ent)) return;
		EnderDragon dragon = (EnderDragon) ent;

		dragon.remove();

		PetEnderDragon petDragon = this.create(dragon.getLocation(), null, true);
		petDragon.copyFrom(dragon);
		petDragon.spawn();
		for (Entity passenger: dragon.getPassengers()){
			petDragon.getEntity().addPassenger(passenger);
		}

		plugin.getDragonRegistry().handleDragonReset(dragon, () -> petDragon.getEntity().remove());
	}

	
	public UUID getOwner(EnderDragon dragon){
		if (!dragon.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) return null;
		
		String uuidText = dragon.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
		if (uuidText.equals("")) return null;
		return UUID.fromString(uuidText);
	}
	
	
	
}
