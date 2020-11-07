package com.ericdebouwer.petdragon;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;
import com.google.common.collect.ImmutableMap;

public class DragonFactory {
	
	PetDragon plugin;
	Class<?> dragonClass;
	private boolean correctVersion = true;
	public NamespacedKey ownerKey;

	public DragonFactory(PetDragon plugin){
		this.plugin = plugin;
		this.ownerKey = new NamespacedKey(plugin, PetEnderDragon.OWNER_ID);
		this.correctVersion = this.setUpDragonClass();
	}
	
	public boolean isCorrectVersion(){
		return correctVersion;
	}
	
	public PetEnderDragon create(Location loc, UUID owner){
		try {
			PetEnderDragon dragon = (PetEnderDragon) dragonClass.getConstructor(Location.class, PetDragon.class).newInstance(loc, plugin);
			if (owner != null){
				dragon.getEntity().getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
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
	
	public boolean tryRide(Player p, EnderDragon dragon){
		if (!isPetDragon(dragon)) return false;
		ItemStack handHeld = p.getInventory().getItemInMainHand();
		if ( !(handHeld == null || handHeld.getType().isAir())) return false;
		
		if (!p.hasPermission("petdragon.ride")) {
			plugin.getConfigManager().sendMessage(p, Message.NO_RIDE_PERMISSION, null);
			return true;
		}
		UUID owner = getOwner(dragon);
		if (!p.hasPermission("petdragon.bypass.owner") && owner != null && !p.getUniqueId().equals(owner)){
			plugin.getConfigManager().sendMessage(p, Message.NO_JOYRIDE, ImmutableMap.of("owner", Bukkit.getOfflinePlayer(owner).getName()));
			return true;
		}
		dragon.addPassenger(p);
		return true;
	}
	
	public PetEnderDragon copy(EnderDragon dragon){
		PetEnderDragon petDragon = this.create(dragon.getLocation(), null);
		petDragon.copyFrom(dragon);
		return petDragon;	
	}
	
	public Set<EnderDragon> getDragons(Player player){
		Set<EnderDragon> result = new HashSet<EnderDragon>();
		for (World world: Bukkit.getWorlds()){
			for (EnderDragon dragon: world.getEntitiesByClass(EnderDragon.class)){
				if (!isPetDragon(dragon)) continue;
				if (!player.getUniqueId().equals(getOwner(dragon))) continue;
				
				result.add(dragon);
			}
		}
		return result;
	}
	
	// all remove calls in one place for future
	public void removeDragon(EnderDragon dragon){
		dragon.remove();
	}
	
	public boolean setUpDragonClass(){
		String packageName = plugin.getServer().getClass().getPackage().getName();
    	String version = packageName.substring(packageName.lastIndexOf('.') + 1);

    	try {
        	final Class<?> clazz = Class.forName("com.ericdebouwer.enderdragonNMS.PetEnderDragon_" + version);
        	if (PetEnderDragon.class.isAssignableFrom(clazz)) { 
        		this.dragonClass = clazz;
        		return true;
        	}
    	} catch (final Exception e) {
        	return false;
   		}
    	return false;
	}
	
	public UUID getOwner(EnderDragon dragon){
		if (!dragon.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) return null;
		
		String uuidText = dragon.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
		if (uuidText.equals("")) return null;
		return UUID.fromString(uuidText);
	}
	
	
	
}
