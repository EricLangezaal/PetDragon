package com.ericdebouwer.petdragon;

import com.ericdebouwer.petdragon.config.Message;
import com.ericdebouwer.petdragon.enderdragonNMS.PetEnderDragon;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DragonFactory {
	
	PetDragon plugin;
	Class<?> dragonClass;
	@Getter
	private final boolean correctVersion;
	private final NamespacedKey ownerKey;
	private final NamespacedKey dragonIdKey;

	public DragonFactory(PetDragon plugin) {
		this.plugin = plugin;
		this.ownerKey = new NamespacedKey(plugin, PetEnderDragon.OWNER_ID);
		this.dragonIdKey = new NamespacedKey(plugin, PetEnderDragon.DRAGON_ID);
		this.correctVersion = this.setUpDragonClass();
	}

	public boolean setUpDragonClass() {
		String packageName = plugin.getServer().getClass().getPackage().getName();
		String nmsVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

		String mcVersion = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf('-'));
		switch (mcVersion){ // paper made this necessary...
			case "1.14": case "1.14.1": case "1.14.2": case "1.14.3": case "1.14.4": nmsVersion = "v1_14_R1"; break;
			case "1.15": case "1.15.1": case "1.15.2": nmsVersion = "v1_15_R1"; break;
			case "1.16.4": case "1.16.5": nmsVersion = "v1_16_R3"; break;
			case "1.17.1": nmsVersion = "v1_17_R1_2"; break;
			case "1.18.2": nmsVersion = "v1_18_R2"; break;
			case "1.19.4": nmsVersion = "v1_19_R3"; break;
			case "1.20": case "1.20.1": nmsVersion = "v1_20_R1"; break;
			case "1.20.2": case "1.20.3": nmsVersion = "v1_20_R2"; break;
			case "1.20.4": nmsVersion = "v1_20_R3"; break;
			case "1.20.5": case "1.20.6": nmsVersion = "v1_20_R4"; break;
			case "1.21": nmsVersion = "v1_21_R1"; break;
			case "1.21.4": nmsVersion = "v1_21_R3"; break;
			case "1.21.6": case "1.21.7": case "1.21.8": nmsVersion = "v1_21_R5"; break;
		}

    	try {
        	final Class<?> clazz = Class.forName("com.ericdebouwer.petdragon.enderdragonNMS.PetEnderDragon_" + nmsVersion);
        	if (PetEnderDragon.class.isAssignableFrom(clazz)) { 
        		this.dragonClass = clazz;
        		return true;
        	}
			return false;
    	} catch (final Exception e) {
        	return false;
   		}
	}

	public PetEnderDragon create(World world, UUID owner) {
		try {
			//PetEnderDragon dragon = (PetEnderDragon) dragonClass.getConstructor(Location.class, PetDragon.class).newInstance(loc, plugin);
			PetEnderDragon dragon = (PetEnderDragon) dragonClass.getConstructor(World.class).newInstance(world);

			if (!dragon.getEntity().getPersistentDataContainer().has(dragonIdKey, PersistentDataType.STRING)) {
				dragon.getEntity().getPersistentDataContainer().set(dragonIdKey, PersistentDataType.STRING, dragon.getEntity().getUniqueId().toString());
			}
			if (owner != null){
				dragon.getEntity().getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
			}
			return dragon;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean isPetDragon(Entity ent) {
		if (!(ent instanceof EnderDragon)) return false;
		return ent.getScoreboardTags().contains(PetEnderDragon.DRAGON_ID);
	}
	
	public boolean canDamage(HumanEntity player, PetEnderDragon dragon) {
		UUID owner = getOwner(dragon.getEntity());
		if (owner == null) return true;
		if (owner.equals(player.getUniqueId())) return player.hasPermission("petdragon.hurt.self");
		return player.hasPermission("petdragon.hurt.others");
	}
	
	public boolean tryRide(HumanEntity p, EnderDragon dragon) {
		if (!isPetDragon(dragon)) return false;

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

	/**
	 * Manually reset dragons spawned before 1.6 since their entity type is still wrong
	 * @param ent the dragon to check
	 */
	public void handleOldDragon(Entity ent) {
		if (!isPetDragon(ent)) return;
		EnderDragon dragon = (EnderDragon) ent;
		try {
			if (dragon.getClass().getDeclaredMethod("getHandle").invoke(dragon) instanceof PetEnderDragon) return;
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
		}
		resetDragon(dragon);
	}

	public void resetDragon(EnderDragon dragon) {
		if (!isPetDragon(dragon)) return;

		List<Entity> passengers = dragon.getPassengers();
		dragon.remove();

		PetEnderDragon petDragon = this.create(dragon.getWorld(), null);
		petDragon.copyFrom(dragon);
		petDragon.spawn(dragon.getLocation().toVector());

		passengers.forEach(p -> petDragon.getEntity().addPassenger(p));
	}

	public Set<EnderDragon> getDragons(OfflinePlayer player) {
		Set<EnderDragon> result = new HashSet<>();
		for (World world: Bukkit.getWorlds()){
			for (EnderDragon dragon: world.getEntitiesByClass(EnderDragon.class)){
				if (!isPetDragon(dragon)) continue;
				if (!player.getUniqueId().equals(getOwner(dragon))) continue;

				result.add(dragon);
			}
		}
		return result;
	}

	public UUID getKeyFromDragon(EnderDragon dragon, NamespacedKey key) {
		if (!dragon.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return null;
		String uuidText = dragon.getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if (uuidText == null || uuidText.equals("")) return null;
		return UUID.fromString(uuidText);
	}

	public @Nullable UUID getOwner(EnderDragon dragon){
		return getKeyFromDragon(dragon, ownerKey);
	}

}
