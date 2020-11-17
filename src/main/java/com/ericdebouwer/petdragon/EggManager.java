package com.ericdebouwer.petdragon;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;

public class EggManager implements Listener {
	
	private NamespacedKey key;
	private PetDragon plugin;

	public EggManager(PetDragon plugin){
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		key = new NamespacedKey(plugin, "egg");
	}
	
	public ItemStack getEgg(){
		ItemStack egg = new ItemStack(Material.DRAGON_EGG);
		ItemMeta meta = egg.getItemMeta();
		meta.getPersistentDataContainer().set(key, PersistentDataType.SHORT, (short) 1);
		meta.setDisplayName(plugin.getConfigManager().dragonEggName);
		egg.setItemMeta(meta);
		return egg;
	}
	
	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
	public void eggPlace(BlockPlaceEvent e){
		if (e.getItemInHand().getType() != Material.DRAGON_EGG) return;
		if (!e.getItemInHand().getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.SHORT)) return;
		if (!e.getPlayer().hasPermission("petdragon.spawnegg")) {
			plugin.getConfigManager().sendMessage(e.getPlayer(), Message.NO_EGG, null);
			e.setCancelled(true);
			return;
		}
		
		PetEnderDragon dragon = plugin.getFactory().create(e.getBlock().getLocation().add(0, 3, 0), e.getPlayer().getUniqueId());
		dragon.spawn();
		plugin.getConfigManager().sendMessage(e.getPlayer(), Message.EGG_HATCHED, null);
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> (e.getBlock().setType(Material.AIR)));
	}
}
