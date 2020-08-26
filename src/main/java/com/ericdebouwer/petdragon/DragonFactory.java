package com.ericdebouwer.petdragon;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;

public class DragonFactory {
	
	PetDragon plugin;
	Class<?> dragonClass;
	private boolean correctVersion = true;
	public NamespacedKey ownerKey;
	private Map<UUID, Set<UUID>> registry = new HashMap<UUID, Set<UUID>>();

	public DragonFactory(PetDragon plugin){
		this.plugin = plugin;
		this.ownerKey = new NamespacedKey(plugin, PetEnderDragon.OWNER_ID);
		this.correctVersion = this.setUpDragonClass();
		this.loadDragons(Bukkit.getWorlds());
	}

	public void loadDragons(List<World> worlds){
		for (World world: worlds){
			for (EnderDragon dragon: world.getEntitiesByClass(EnderDragon.class)){
				if (!dragon.getScoreboardTags().contains(PetEnderDragon.DRAGON_ID)) continue;
				this.addDragon(dragon.getUniqueId(), this.getOwner(dragon));
			}
		}
	}
	
	public boolean isCorrectVersion(){
		return correctVersion;
	}
	
	public PetEnderDragon create(Location loc, UUID owner){
		try {
			PetEnderDragon dragon = (PetEnderDragon) dragonClass.getConstructor(Location.class, PetDragon.class).newInstance(loc, plugin);
			String uuidText = (owner == null) ? "" : owner.toString();
			dragon.getEntity().getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, uuidText);
			
			this.addDragon(dragon.getEntity().getUniqueId(), owner);
			return dragon;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public void addDragon(UUID dragon, UUID owner){
		if (owner == null) return;
		if (!registry.containsKey(owner)) registry.put(owner, new HashSet<UUID>());
		Set<UUID> dragons = registry.get(owner);
		dragons.add(dragon);
		
	}
	
	public PetEnderDragon copy(EnderDragon dragon){
		UUID owner = this.getOwner(dragon);
		PetEnderDragon petDragon = this.create(dragon.getLocation(), owner);
		petDragon.copyFrom(dragon);
		return petDragon;	
	}
	
	public Set<UUID> getDragons(Player player){
		Set<UUID> dragons = registry.get(player.getUniqueId());
		if (dragons != null) return dragons;
		return Collections.emptySet();
	}
	
	public void removeDragon(EnderDragon dragon){
		dragon.remove();
		UUID owner = this.getOwner(dragon);
		Set<UUID> dragons = registry.get(owner);
		if (dragons != null)
			dragons.remove(dragon.getUniqueId());
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
        		e.printStackTrace();
   		}
    	return false;
	}
	
	public UUID getOwner(EnderDragon dragon){
		if (dragon.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)){
			String uuidText = dragon.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
			if (uuidText.equals("")) return null;
			return UUID.fromString(uuidText);
		}
		return null;
	}
	
	
	
}
